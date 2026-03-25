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
        val partida_id: String?,
        val oponente: String?,
        val oponentePt: Int?
    )


    suspend fun mockBuscarPartida(): RespuestaBuscarPartida {
        delay(1500)

        return RespuestaBuscarPartida(
            estado = EstadoPartida.ENCONTRADA,
            mensaje = "¡Partida encontrada! (modo local sin servidor)",
            partida_id = "mock-partida-001", // o partida_id dependiendo de cómo lo dejaras
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
            override fun onMessage(webSocket: WebSocket, event: String) { //al recibir un mensaje del websocket
                if (promise.isCompleted) {
                    PartidaActiva.onMensajeJuegoRecibido?.invoke(event) //si la partida ya ha empezado se le pasa el mensaje a la pantalla de juego usando el objeto gamesession
                    return
                }
                try {
                    val jsonTolerante = Json { ignoreUnknownKeys = true } //en caso de que el mensaje sea correcto se inicia la partida
                    val datos = jsonTolerante.decodeFromString<RespuestaBuscarPartida>(event)
                    if (datos.estado == EstadoPartida.ENCONTRADA) {
                        timerJob.cancel()
                        val resultadoFinal = datos.copy(
                            mensaje = "Partida encontrada, te enfrentarás a ${datos.oponente}"
                        )
                        PartidaActiva.datosPartida = resultadoFinal
                        PartidaActiva.wsActivo = webSocket
                        promise.complete(resultadoFinal)

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