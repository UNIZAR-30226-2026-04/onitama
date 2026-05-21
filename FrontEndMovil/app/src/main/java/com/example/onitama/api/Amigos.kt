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

    @Serializable
    @SerialName("INVITACION_PARTIDA")
    data class MensajeInvitacionPartida(
        val remitente: String,
        val idNotificacion: Int
    ): MensajeServidor()

    @Serializable
    @SerialName("SOLICITUD_REANUDAR")
    data class MensajeSolicitudReanudar(
        val remitente: String,
        val idNotificacion: Int,
        val idPartida: Int
    ): MensajeServidor()

    @Serializable
    @SerialName("NOTIFICACION_CANCELADA")
    data class MensajeNotificacionCancelada(
        val idNotificacion: Int
    ): MensajeServidor()

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "tipo"
    }

    /**
     * Esta función se encarga de enviar y recibir los mensajes
     * correspondientes al servidor para obtener los amigos del
     * usuario 'usuario'.
     *
     * @param usuario Nombre del usuario cuyos amigos se van a obtener.
     * @return Lista con los amigos del usuario 'usuario'.
     */
    suspend fun obtenerAmigos(usuario: String): List<Info> {
        if (!usarServidor) {
            val amigo1 = Info(nombre = "granluchador", puntos = 100)
            val amigo2 = Info(nombre = "margaret", puntos = 200)
            val amigos = listOf(amigo1, amigo2)
            return amigos
        }

        return try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeObtenerAmigos(usuario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)

            // Espera 5 segundos para recibir la respuesta del servidor.
            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("INFORMACION_AMIGOS", "NO_AMIGOS", "ERROR_AMIGOS")
                    }
                    .first()
                    .toString()
            } ?: return emptyList()

            // Si recibe respuesta la decodifica y según el mensaje recibido
            // devuelve una lista con los amigos o una lista vacía.
            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeInformacionAmigos) {
                respuesta.info
            } else if (respuesta is MensajeNoAmigos) {
                emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al obtener amigos", e)
            emptyList()
        }
    }

    /**
     * Esta función se encarga de enviar y recibir los mensajes
     * correspondientes al servidor para obtener los jugadores cuyo
     * nombre de usuario comienza por 'raiz'.
     *
     * @param raiz La raíz por la que comienzan los nombres de los
     * jugadores que se buscan.
     * @return Lista con los jugadores que tienen la raíz 'raiz'.
     */
    suspend fun buscarJugadores(raiz: String): List<Info> {
        if (!usarServidor) return emptyList()

        return try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeBuscarJugadores(raiz)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)

            // Espera 5 segundos para recibir la respuesta del servidor.
            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("INFORMACION_JUGADORES", "NO_ENCONTRADOS")
                    }
                    .first()
                    .toString()
            } ?: return emptyList()

            // Si recibe respuesta la decodifica y según el mensaje recibido
            // devuelve una lista con los jugadores que comparten la raíz o una lista vacía.
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

    /**
     * Esta función se encarga de enviar y recibir los mensajes
     * correspondientes al servidor para enviar una solicitud
     * de amistad al usuario 'destinatario'.
     *
     * @param remitente Nombre del usuario que envía la solicitud.
     * @param destinatario Nombre del usuario que recibe la solicitud.
     * @return Devuelve 'true' si la solicitud ha sido enviada con
     * éxito y 'false' si ha fallado.
     */
    suspend fun enviarSolicitudAmistad(remitente: String, destinatario: String): Boolean {
        if (!usarServidor) {
            Log.d("Amigos_API", "Mock: Enviando solicitud")
            return true
        }

        return try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeSolicitudAmistad(remitente, destinatario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)

            // Espera 5 segundos para recibir la respuesta del servidor.
            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("AMISTAD_ACEPTADA", "ERROR_SOLICITUD_AMISTAD", "SOLICITUD_ENVIADA")
                    }
                    .first()
                    .toString()
            }

            if (respuestaStr == null) return true

            // Si recibe respuesta la decodifica y según el mensaje recibido
            // devuelve una lista con los jugadores que comparten la raíz o una lista vacía.
            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeErrorSolicitudAmistad) {
                false
            } else {
                // Si recibe cualquier otra cosa asumimos éxito.
                true 
            }

        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al enviar solicitud", e)
            false
        }
    }

    /**
     * Esta función se encarga de enviar y recibir los mensajes
     * correspondientes al servidor para borrar el amigo 'amigo',
     * es decir, que deje de ser amigo de 'usuario'.
     *
     * @param usuario Nombre del usuario que borra al amigo.
     * @param amigo Nombre del amigo a borrar.
     * @return Devuelve 'true' si el amigo ha sido borrado con
     * éxito y 'false' si no se ha podido borrar.
     */
    suspend fun borrarAmigo(usuario: String, amigo: String): Boolean {
        if (!usarServidor) {
            Log.d("Amigos_API", "Mock: Borrando amigo $amigo de $usuario")
            return true
        }

        return try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeBorrarAmigo(usuario, amigo)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)

            // Espera 5 segundos para recibir la respuesta del servidor.
            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("AMIGO_BORRADO", "ERROR_AL_BORRAR_AMIGO")
                    }
                    .first()
                    .toString()
            } ?: return false

            // Si recibe respuesta la decodifica y según el mensaje recibido
            // devuelve 'true' si se ha borrado o 'false' si no se ha borrado.
            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeAmigoBorrado) {
                Log.d("Amigos_API", "Amigo borrado con éxito")
                true
            } else {
                Log.d("Amigos_API", "Error al borrar amigo")
                false
            }
        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al borrar amigo", e)
            false
        }
    }

    /**
     * Esta función se encarga de enviar los mensajes correspondeintes
     * al servidor para aceptar una solicitud de amistad.
     *
     * @param remitente Nombre del usuario que envió la solicitud.
     * @param destinatario Nombre del usuario que acepta la solicitud.
     * @return Devuelve 'true' si la amistad ha sido aceptada con éxito.
     */
    suspend fun aceptarAmistad(remitente: String, destinatario: String): Boolean {
        if (!usarServidor) return true

        return try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeAceptarAmistad(remitente, destinatario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)
            true

        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al aceptar amistad", e)
            false
        }
    }

    /**
     * Esta función se encarga de enviar y recibir los mensajes correspondientes
     * al servidor para rechazar una solicitud de amistad.
     *
     * @param idNotificacion ID de la notificación a rechazar.
     * @return Devuelve 'true' si la solicitud ha sido rechazada con éxito.
     */
    suspend fun rechazarAmistad(idNotificacion: Int): Boolean {
        if (!usarServidor) return true

        return try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeRechazarAmistad(idNotificacion)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)
            true

        } catch (e: Exception) {
            Log.e("Amigos_API", "Error al rechazar amistad", e)
            false
        }
    }
}