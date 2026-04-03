package com.example.onitama.api


import android.util.Log
import com.example.onitama.Config
import com.example.onitama.PartidaActiva
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.Response

/**
 * Cliente API – Buscar Partida Pública.
 * Envía BUSCAR_PARTIDA al servidor vía WebSocket y espera PARTIDA_ENCONTRADA.
 *
 * Cambios respecto a la versión inicial:
 *  - Ahora envía `nombre` y `puntos` para el sistema de matchmaking por puntuación.
 *  - NO cierra el WebSocket al recibir PARTIDA_ENCONTRADA: lo traspasa a
 *    api/partida.ts (setWsActivo) para que la pantalla de partida lo reutilice.
 *  - Guarda los datos completos de la partida en sessionStorage para que
 *    /partida/page.tsx los lea al inicializarse.
 *
 * Si el servidor no está disponible, el fallback mock sigue funcionando igual.
 */


class BuscarPartida(
    private val wsUrl: String = Config.WS_URL
){
    val usarServidor: Boolean get() = !(wsUrl.isEmpty())
    @Serializable
    data class MensajeBuscarPartida(
        val tipo: String,
        val nombre: String,
        val puntos: Int
    )

    enum class EstadoPartida{
        BUSCANDO, ENCONTRADA, ERROR, TIMEOUT, CANCELADO
    }
    @Serializable
    data class RespuestaBuscarPartida(
        val estado: EstadoPartida,
        val mensaje: String,
        val partida_id: Int?,
        val oponente: String?,
        val oponentePt: Int?
    )


    suspend fun mockBuscarPartida(): RespuestaBuscarPartida {
        delay(1500)

        return RespuestaBuscarPartida(
            estado = EstadoPartida.ENCONTRADA,
            mensaje = "¡Partida encontrada! (modo local sin servidor)",
            partida_id = 6969, // o partida_id dependiendo de cómo lo dejaras
            oponente = "granluchador",
            oponentePt = 1200
        )
    }

    data class ResultadoBusqueda(
        val promise: Deferred<RespuestaBuscarPartida>,
        val cancel: () -> Unit
    )

    fun buscarPartida(scope: CoroutineScope, nombre: String = "Jugador", puntos: Int = 0, timeout: Long = 30000): ResultadoBusqueda {
        if (!usarServidor) {
            return ResultadoBusqueda(
                promise = scope.async { mockBuscarPartida() },
                cancel = {}
            )
        }
        val promise = CompletableDeferred<RespuestaBuscarPartida>()

        val client = OkHttpClient()
        val request = Request.Builder().url(wsUrl).build()
        var ws: WebSocket? = null

        val timerJob = scope.launch { //temporizador, si tarda más de 30 seg en responder salta error
            delay(timeout)
            if (!promise.isCompleted) {
                ws?.close(1000, "Timeout")
                promise.complete(
                    RespuestaBuscarPartida(EstadoPartida.TIMEOUT, "Sin respuesta del servidor. Inténtalo de nuevo.", null, null, null)
                )
            }
        }

        val cancel = {
            if (!promise.isCompleted) { //se le da al user la opción de cancelar la búsqueda
                timerJob.cancel()
                val mensaje = MensajeBuscarPartida("CANCELAR", nombre, puntos)
                ws?.send(Json.encodeToString(mensaje))
                ws?.close(1000, "Búsqueda cancelada por el usuario")

                promise.complete(
                    RespuestaBuscarPartida(EstadoPartida.CANCELADO, "Búsqueda cancelada", null, null, null)
                )
            }
        }

        ws = client.newWebSocket(request, object : okhttp3.WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { //si el websocket está abierto le envía el mensaje de buscar partida
                val mensaje = MensajeBuscarPartida("BUSCAR_PARTIDA", nombre, puntos)
                webSocket.send(Json.encodeToString(mensaje))
            }
            override fun onMessage(webSocket: WebSocket, event: String) {
                Log.w("CHIVATO_WS", "El móvil (Capa de Red) acaba de recibir: $event")//al recibir un mensaje del websocket
                if (promise.isCompleted) {
                    val receptor = PartidaActiva.onMensajeJuegoRecibido
                    if(receptor == null){
                        Log.e("CHIVATO_WS", "¡PELIGRO! Llegó un mensaje pero la PartidaActivity no está escuchando.")
                    }else{
                        receptor.invoke(event)
                        return
                    } //si la partida ya ha empezado se le pasa el mensaje a la pantalla de juego usando el objeto PartidaActiva
                    return
                }
                try {
                    val jsonTolerante = Json {
                        ignoreUnknownKeys = true
                        classDiscriminator = "tipo" // El nombre del campo que dice si es PARTIDA_ENCONTRADA u otro
                    } //en caso de que el mensaje sea correcto se inicia la partida
                    val mensajeEntrante = jsonTolerante.decodeFromString<Partida.MensajeServidor>(event)
                    when (mensajeEntrante) {
                        is Partida.RespuestaPartidaEncontrada -> {
                            timerJob.cancel()

                            // 1. Guardamos los datos de la partida en el Singleton
                            PartidaActiva.datosPartida = mensajeEntrante
                            PartidaActiva.wsActivo = webSocket

                            // 2. Resolvemos la promesa para que la UI sepa que hemos terminado
                            promise.complete(
                                RespuestaBuscarPartida(
                                    estado = EstadoPartida.ENCONTRADA,
                                    mensaje = "¡Partida encontrada! Te enfrentarás a ${mensajeEntrante.oponente}",
                                    // Estos campos ya no hacen falta aquí porque están en PartidaActiva,
                                    // pero los mantenemos si tu UI los lee:
                                    partida_id = mensajeEntrante.partida_id,
                                    oponente = mensajeEntrante.oponente,
                                    oponentePt = mensajeEntrante.oponentePt
                                )
                            )
                        }

                        else -> {
                            // Es otro tipo de mensaje (ej: "BUSCANDO..."), lo ignoramos
                            Log.d("BuscarPartida", "Mensaje de estado recibido: $event")
                        }
                    }
                } catch( x: Exception){ //si no lo es se usa el mock
                    Log.w("BuscarPartida","Error al decodificar el mensaje: $event")
                    if (!promise.isCompleted) {
                        timerJob.cancel()
                        scope.launch { promise.complete(mockBuscarPartida()) }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { //en caso de fallo se usa el mock
                timerJob.cancel()
                if (!promise.isCompleted) {
                    Log.w("Matchmaking", "Conexión cerrada inesperadamente. Usando mock. Error: ${t.message}")
                    timerJob.cancel()
                    scope.launch {
                        // Ejecutamos el mock y esperamos su resultado (.await())
                        val resultadoMock = mockBuscarPartida()
                        // Equivalente a: mockBuscarPartida().then(resolvePromise)
                        promise.complete(resultadoMock)
                    }
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { //en caso de que la conexión esté cerrada (por timeout o por cancelar) da error
                Log.e("CHIVATO_WS", "¡EL SOCKET SE HA CERRADO! Razón: $reason")
                if (!promise.isCompleted ) {
                    timerJob.cancel()
                    promise.complete(
                        RespuestaBuscarPartida(
                            EstadoPartida.ERROR,
                            "Conexión cerrada",
                            null,
                            null,
                            null
                        )
                    )
                }
            }
        })

        return ResultadoBusqueda(promise, cancel)
    }
}