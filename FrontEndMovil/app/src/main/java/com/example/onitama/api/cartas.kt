package com.example.onitama.api

import android.util.Log
import com.example.onitama.Config
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CartasAPI(
    private val wsUrl: String = Config.WS_URL
) {
    val usarServidor: Boolean get() = !(wsUrl.isEmpty())

    @Serializable
    data class CartaYPuntos(
        val nombre: String,
        val puntos_necesarios: Int,
        val descripcion: String = ""
    )

    @Serializable
    sealed class MensajeCliente

    @Serializable
    @SerialName("OBTENER_CARTAS")
    object MensajeObtenerCartas : MensajeCliente()

    @Serializable
    @SerialName("OBTENER_CARTAS_ACCION")
    object MensajeObtenerCartasAccion : MensajeCliente()

    @Serializable
    sealed class MensajeServidor

    @Serializable
    @SerialName("LISTA_CARTAS")
    data class MensajeListaCartas(
        val cartas: List<CartaYPuntos>
    ): MensajeServidor()

    @Serializable
    @SerialName("LISTA_CARTAS_ACCION")
    data class MensajeListaCartasAccion(
        val cartas: List<CartaYPuntos>
    ): MensajeServidor()

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "tipo"
    }

    /**
     * Esta función se encarga de enviar y recibir los mensajes
     * correspondientes al servidor para obtener la lista de cartas de movimiento.
     *
     * @return Lista de cartas disponibles con sus puntos necesarios.
     */
    suspend fun obtenerCartas(): List<CartaYPuntos> {
        if (!usarServidor) {
            // Datos mock en caso de no usar servidor
            return listOf(
                CartaYPuntos(nombre = "Tigre", puntos_necesarios = 0),
                CartaYPuntos(nombre = "Cangrejo", puntos_necesarios = 0),
                CartaYPuntos(nombre = "Rana", puntos_necesarios = 0),
                CartaYPuntos(nombre = "Ganso", puntos_necesarios = 0),
                CartaYPuntos(nombre = "Conejo", puntos_necesarios = 0)
            )
        }

        return try {
            val mensaje = MensajeObtenerCartas
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)

            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        json.optString("tipo") == "LISTA_CARTAS"
                    }
                    .first()
                    .toString()
            } ?: return emptyList()

            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeListaCartas) {
                respuesta.cartas
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Cartas_API", "Error al obtener cartas de movimiento", e)
            emptyList()
        }
    }

    /**
     * Esta función se encarga de enviar y recibir los mensajes
     * correspondientes al servidor para obtener la lista de cartas de accion.
     *
     * @return Lista de cartas disponibles con sus puntos necesarios.
     */
    suspend fun obtenerCartasAccion(): List<CartaYPuntos> {
        if (!usarServidor) {
        }

        return try {
            val mensaje = MensajeObtenerCartasAccion
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)

            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        json.optString("tipo") == "LISTA_CARTAS_ACCION"
                    }
                    .first()
                    .toString()
            } ?: return emptyList()

            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeListaCartasAccion) {
                respuesta.cartas
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Cartas_API", "Error al obtener cartas de accion", e)
            emptyList()
        }
    }
}