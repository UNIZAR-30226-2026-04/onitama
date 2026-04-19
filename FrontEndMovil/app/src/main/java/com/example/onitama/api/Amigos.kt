package com.example.onitama.api

import android.util.Log
import com.example.onitama.Config
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class Amigos (
    private val wsUrl: String = Config.WS_URL
){

    val usarServidor: Boolean get() = !(wsUrl.isEmpty())

    @Serializable
    sealed class MensajeCliente

    @Serializable
    @SerialName("OBTENER_AMIGOS")
    data class MensajeObtenerAmigos(
        val usuario: String
    ): MensajeCliente()

    @Serializable
    @SerialName("BUSCAR_JUGADORES")
    data class MensajeBuscarJugadores(
        val raiz: String
    ): MensajeCliente()

    @Serializable
    @SerialName("SOLICITUD_AMISTAD")
    data class MensajeSolicitudAmistad(
        val remitente: String,
        val destinatario: String
    ): MensajeCliente()

    @Serializable
    @SerialName("ACEPTAR_AMISTAD")
    data class MensajeAceptarAmistad(
        val remitente: String,
        val destinatario: String
    ): MensajeCliente()

    @Serializable
    @SerialName("RECHAZAR_AMISTAD")
    data class MensajeRechazarAmistad(
        val idNotificacion: Int
    ): MensajeCliente()

    @Serializable
    @SerialName("BORRAR_AMIGO")
    data class MensajeBorrarAmigo(
        val usuario: String,
        val amigo: String
    ): MensajeCliente()

    @Serializable
    sealed class MensajeServidor

    @Serializable
    @SerialName("ERROR_AL_BORRAR_AMIGO")
    object MensajeErrorBorrarAmigo : MensajeServidor()

    @Serializable
    @SerialName("AMIGO_BORRADO")
    object MensajeAmigoBorrado : MensajeServidor()

    @Serializable
    @SerialName("ERROR_AMIGOS")
    object MensajeErrorAmigos : MensajeServidor()

    @Serializable
    @SerialName("NO_AMIGOS")
    object MensajeNoAmigos : MensajeServidor()

    @Serializable
    data class Info(
        val nombre: String,
        val puntos: Int
    )

    @Serializable
    @SerialName("INFORMACION_AMIGOS")
    data class MensajeInformacionAmigos(
        val info: List<Info>
    ): MensajeServidor()

    @Serializable
    @SerialName("INFORMACION_JUGADORES")
    data class MensajeInformacionJugadores(
        val info: List<Info>
    ): MensajeServidor()

    @Serializable
    @SerialName("NO_ENCONTRADOS")
    object MensajeNoEncontrados : MensajeServidor()

    @Serializable
    @SerialName("AMISTAD_ACEPTADA")
    data class MensajeAmistadAceptada(
        val amigo: String
    ): MensajeServidor()

    @Serializable
    @SerialName("SOLICITUD_AMISTAD")
    data class MensajeSolicitudAmistadS(
    val remitente: String,
    val fecha_ini: String,
    val fecha_fin: String,
    val idNotificacion: Int
    ): MensajeServidor()

    @Serializable
    @SerialName("ERROR_SOLICITUD_AMISTAD")
    data class MensajeErrorSolicitudAmistad(
        val destinatario: String
    ): MensajeServidor()

    private val client = OkHttpClient()
    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "tipo"
    }

    private suspend fun enviarYEsperarRespuesta(mensajeJson: String): String {
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(wsUrl).build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("Amigos_WS", "Conectado. Enviando: $mensajeJson")
                    webSocket.send(mensajeJson)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("Amigos_WS", "Mensaje recibido del servidor: $text")
                    webSocket.close(1000, "Cierre normal")
                    if (continuation.isActive) {
                        continuation.resume(text)
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("Amigos_WS", "Fallo en el socket", t)
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("Error de conexión con el servidor."))
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("El servidor cerró la conexión sin responder."))
                    }
                }
            }

            val ws = client.newWebSocket(request, listener)

            continuation.invokeOnCancellation {
                ws.cancel()
            }
        }
    }

    suspend fun obtenerAmigos(usuario: String): List<Info> {
        if (!usarServidor) {
            val amigo1 = Info(nombre = "granluchador", puntos = 100)
            val amigo2 = Info(nombre = "margaret", puntos = 200)
            val amigos = listOf(amigo1, amigo2)
            return amigos
        }

        return try {
            val mensaje = MensajeObtenerAmigos(usuario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            
            val respuestaStr = withTimeoutOrNull(5000L) {
                enviarYEsperarRespuesta(jsonMsg)
            } ?: return emptyList()

            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeInformacionAmigos) {
                respuesta.info
            } else if (respuesta is MensajeNoAmigos) {
                emptyList()
            } else {
                Log.e("Amigos_API", "Error al obtener amigos")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al obtener amigos", e)
            emptyList()
        }
    }

    suspend fun buscarJugadores(raiz: String): List<Info> {
        if (!usarServidor) return emptyList()

        return try {
            val mensaje = MensajeBuscarJugadores(raiz)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)

            val respuestaStr = withTimeoutOrNull(5000L) {
                enviarYEsperarRespuesta(jsonMsg)
            } ?: return emptyList()

            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeInformacionJugadores) {
                respuesta.info
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al buscar jugadores", e)
            emptyList()
        }
    }

    suspend fun enviarSolicitudAmistad(remitente: String, destinatario: String): Boolean {
        if (!usarServidor) {
            Log.d("Amigos_API", "Mock: Enviando solicitud de $remitente a $destinatario")
            return true
        }

        return try {
            val mensaje = MensajeSolicitudAmistad(remitente, destinatario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)

            val respuestaStr = withTimeoutOrNull(5000L) {
                enviarYEsperarRespuesta(jsonMsg)
            } ?: return true

            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeErrorSolicitudAmistad) {
                false
            } else {
                false
            }

        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al enviar solicitud", e)
            false
        }
    }

    suspend fun borrarAmigo(usuario: String, amigo: String): Boolean {
        if (!usarServidor) {
            Log.d("Amigos_API", "Mock: Borrando amigo $amigo de $usuario")
            return true
        }

        return try {
            val mensaje = MensajeBorrarAmigo(usuario, amigo)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)

            val respuestaStr = withTimeoutOrNull(5000L) {
                enviarYEsperarRespuesta(jsonMsg)
            } ?: return false

            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeAmigoBorrado) {
                Log.e("Amigos_API", "Amigo borrado con éxito")
                true
            } else {
                Log.e("Amigos_API", "Error al borrar amigo")
                false
            }
        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al borrar amigo", e)
            false
        }
    }
}