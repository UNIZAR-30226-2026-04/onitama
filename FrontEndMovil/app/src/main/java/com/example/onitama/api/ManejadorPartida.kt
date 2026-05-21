package com.example.onitama.api

import com.example.onitama.api.Amigos.MensajeServidor
import com.example.onitama.api.Auth.MensajeCambiarContrasegna
import com.example.onitama.api.Auth.MensajeCliente
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import kotlin.apply


@Serializable
@SerialName("INVITACION_PARTIDA")
data class MensajeInvitacionPartida(
    val remitente: String,
    val destinatario: String
): MensajeCliente()


@Serializable
@SerialName("CANCELAR_NOTIFICACION")
data class MensajeCancelarNotificacion(
    val idNotificacion: Int
): MensajeCliente()

@Serializable
@SerialName("ACEPTAR_INVITACION")
data class MensajeAceptarInvitacion(
    val idNotificacion: Int,
    val nombre: String
): MensajeCliente()


@Serializable
@SerialName("RECHAZAR_INVITACION")
data class MensajeRechazarInvitacion(
    val idNotificacion: Int,
    val nombre: String
): MensajeCliente()


@Serializable
@SerialName("SOLICITAR_PARTIDAS_PUB")
data class MensajeSolicitarPartidasRecientes(
    val usuario: String,
): MensajeCliente()

@Serializable
data class PartidaReciente(
    val partida_id: Int,
    val oponente: String,
    val estado: String,
    val tiempo: Int,
    val ganador: String
)

@Serializable
data class RespuestaPartidasPublicas(
    val tipo: String,
    val partidas: List<PartidaReciente>
)

@Serializable
@SerialName("SOLICITAR_PARTIDAS_PRIV")
data class MensajeSolicitarPartidasPausadas(
    val usuario: String,
    val amigo: String
): MensajeCliente()

private val jsonSerializer = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "tipo"
}
class ManejadorPartidaAPI {
    fun enviarInvitacion(
        remitente: String,
        destinatario: String
    ) {

        val mensaje = MensajeInvitacionPartida(remitente, destinatario)
        val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)

        ManejadorGlobal.enviarMensaje(jsonMsg)
    }

    fun cancelarNotificacionEnviada(
        idNotificacion: Int
    ) {
        if (idNotificacion == -1) {
            return
        }
        val mensaje = MensajeCancelarNotificacion(idNotificacion)
        val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
        ManejadorGlobal.enviarMensaje(jsonMsg)
    }

    fun responderInvitacion(
        idNotificacion: Int,
        destinatario: String,
        aceptada: Boolean
    ) {

        if (aceptada) {
            val mensaje = MensajeAceptarInvitacion(idNotificacion, destinatario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)
        }
        else {
            val mensaje = MensajeRechazarInvitacion(idNotificacion, destinatario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)
        }
    }

    fun obtenerPartidasPausadas(
        nombreUsuario: String,
        nombreAmigo: String
    ) {
        val mensaje = MensajeSolicitarPartidasPausadas(nombreUsuario, nombreAmigo)
        val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
        ManejadorGlobal.enviarMensaje(jsonMsg)
    }

    @Serializable
    @SerialName("SOLICITAR_REANUDAR")
    data class MensajeSolicitarReanudar(
        val remitente: String,
        val destinatario: String,
        val idPartida: Int
    ): MensajeCliente()

    fun solicitarReanudar(
        remitente: String,
        destinatario: String,
        idPartida: Int
    ) {
        val mensaje = MensajeSolicitarReanudar(remitente, destinatario, idPartida)
        val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
        ManejadorGlobal.enviarMensaje(jsonMsg)
    }


    suspend fun solicitarRecientes(usuario: String): List<PartidaReciente> {
        val mensaje = MensajeSolicitarPartidasRecientes(usuario)
        val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)

        ManejadorGlobal.enviarMensaje(jsonMsg)

        val respuestaStr: String = withTimeoutOrNull(5000L) {
            ManejadorGlobal.mensajesEntrantes
                .filter { json ->
                    json.optString("tipo") == "PARTIDAS_PUBLICAS"
                }
                .first()
                .toString()
        } ?: throw Exception("Tiempo de espera agotado al recibir partidas")

        val respuesta = jsonSerializer.decodeFromString<RespuestaPartidasPublicas>(respuestaStr)

        return respuesta.partidas.take(3)
    }

    @Serializable
    @SerialName("ACEPTAR_REANUDAR")
    data class MensajeAceptarReanudar(
        val idNotificacion: Int,
        val nombre: String
    ): MensajeCliente()

    @Serializable
    @SerialName("RECHAZAR_REANUDAR")
    data class MensajeRechazarReanudar(
        val idNotificacion: Int,
        val nombre: String
    ): MensajeCliente()

    fun responderReanudacion(
        idNotificacion: Int,
        nombreUsuario: String,
        aceptada: Boolean
    ){

        if (aceptada) {
           val mensaje = MensajeAceptarReanudar(idNotificacion, nombreUsuario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)
        }
        else {
            val mensaje = MensajeRechazarReanudar(idNotificacion, nombreUsuario)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)
        }
    }
}