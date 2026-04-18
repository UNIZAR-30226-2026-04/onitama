package com.example.onitama.api


import android.util.Log
import com.example.onitama.Config
import com.example.onitama.PartidaActiva
import com.example.onitama.api.ManejadorGlobal.mensajesEntrantes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import okhttp3.Response
import org.json.JSONObject

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
        val cancel = {
            if (!promise.isCompleted) { //se le da al user la opción de cancelar la búsqueda
                val mensaje = MensajeBuscarPartida("CANCELAR", nombre, puntos)
                ManejadorGlobal.enviarMensaje(Json.encodeToString(mensaje))

                promise.complete(
                    RespuestaBuscarPartida(EstadoPartida.CANCELADO, "Búsqueda cancelada", null, null, null)
                )
            }
        }
        val requestJson = JSONObject().apply {
            put("tipo", "BUSCAR_PARTIDA")
            put("nombre", nombre)
            put("puntos", puntos)
        }
        ManejadorGlobal.enviarMensaje(requestJson.toString())
        scope.launch {
            try {
                // 2. Nos quedamos mirando la cinta transportadora CON UN TIMEOUT de 30 seg
                withTimeout(timeout) {

                    val respuesta = mensajesEntrantes
                        .filter { json ->
                            val tipo = json.optString("tipo")

                            tipo == "PARTIDA_ENCONTRADA"
                        }
                        .first()
                    // Aquí puedes usar tu PartidaActiva.datosPartida = ...
                    // Igual que hacías antes.
                    val jsonTolerante = Json {
                        ignoreUnknownKeys = true
                        classDiscriminator =
                            "tipo" // El nombre del campo que dice si es PARTIDA_ENCONTRADA u otro
                    } //en caso de que el mensaje sea correcto se inicia la partida
                    val mensajeEntrante =
                        jsonTolerante.decodeFromString<Partida.MensajeServidor>(respuesta.toString())
                    if (mensajeEntrante is Partida.RespuestaPartidaEncontrada) {
                        PartidaActiva.datosPartida = mensajeEntrante
                        promise.complete(
                            RespuestaBuscarPartida(
                                estado = EstadoPartida.ENCONTRADA,
                                mensaje = "¡Partida encontrada! Te enfrentarás a ${mensajeEntrante.oponente}",
                                partida_id = mensajeEntrante.partida_id,
                                oponente = mensajeEntrante.oponente,
                                oponentePt = mensajeEntrante.oponentePt
                            )
                        )
                    }
                }

            } catch (e: TimeoutCancellationException) {
                // Si pasaron 30 segundos y la función .first() no encontró nada
                val mensajeCancelar = MensajeBuscarPartida("CANCELAR", nombre, puntos)
                ManejadorGlobal.enviarMensaje(Json.encodeToString(mensajeCancelar))
                promise.complete(
                    RespuestaBuscarPartida(
                        EstadoPartida.TIMEOUT,
                        "Tiempo agotado",
                        null,
                        null,
                        null
                    )
                )
            }
        }

        return ResultadoBusqueda(promise, cancel)
    }
}