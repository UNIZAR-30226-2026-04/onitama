package com.example.onitama.api

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
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.json.JSONObject

/**
 * Cliente API – Buscar Partida Pública.
 * Envía BUSCAR_PARTIDA al servidor vía WebSocket y espera PARTIDA_ENCONTRADA.
 *
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

    /**
     * Función que se usa en caso de que no haya servidor (se usaba en las primeras pruebas)
     **/

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

    /**
     * Esta clase contiene
     * una promesa que es más o menos un puntero que por el momento está vacío, pero que nos avisará cuando deje de estarlo, que será cuando encontremos una partida
     * y una función para cancelar la búsqueda
     **/
    data class ResultadoBusqueda(
        val promise: Deferred<RespuestaBuscarPartida>,
        val cancel: () -> Unit
    )

    /**
     * La función que se usa para enviarle el mensaje de búsqueda al servidor, devuelve un dato del tipo definido anteriormente y
     * se queda esperando de forma concurrente en la corutina que se pasa en el valor scope durante timeout milisegundo
     * Cuando la promesa se complete, sabremos si se ha encontrado una partida o no
     **/
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

                            tipo == "PARTIDA_ENCONTRADA" || tipo == "PARTIDA_PRIVADA_ENCONTRADA"
                        }
                        .first()

                    val jsonTolerante = Json {
                        ignoreUnknownKeys = true
                        classDiscriminator =
                            "tipo" // El nombre del campo que dice si es PARTIDA_ENCONTRADA u otro
                    } //en caso de que el mensaje sea correcto se inicia la partida
                    val mensajeEntrante =
                        jsonTolerante.decodeFromString<Partida.MensajeServidor>(respuesta.toString())
                    when (mensajeEntrante) {
                        is Partida.RespuestaPartidaEncontrada -> {
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

                        is Partida.RespuestaPartidaPrivadaEncontrada -> {
                            PartidaActiva.datosPartida = mensajeEntrante.toPartidaEncontrada()
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

                        else -> {}
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