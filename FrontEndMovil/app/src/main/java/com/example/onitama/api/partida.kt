package com.example.onitama.api

import android.util.Log
import com.example.onitama.Config
import com.example.onitama.PartidaActiva
import com.example.onitama.lib.EquipoID
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.Objects

/**
 * Cliente WebSocket para la partida en curso.
 *
 * Mensajes cliente → servidor:
 *   ESTOY_LISTO  – la pantalla de partida está cargada y lista
 *   MOVER        – el jugador mueve una ficha
 *   ABANDONAR    – el jugador abandona voluntariamente la partida
 *
 * Mensajes servidor → cliente:
 *   TU_TURNO         – el servidor autoriza al cliente a mover (primer turno)
 *   MOVER            – el oponente ha movido; se retransmite al otro cliente
 *   TERMINAR_PARTIDA – la partida ha terminado (tiempo, victoria, abandono)
 *
 * NOTA sobre nomenclatura: el diagrama de secuencia del backend usa
 * QUIERO_JUGAR / EMPIEZA_PARTIDA, pero el servidor (Servidor.java) implementa
 * BUSCAR_PARTIDA / PARTIDA_ENCONTRADA. Se usan estos últimos para coherencia
 * con el código del servidor existente.
 */

val jsonPartida = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "tipo"
}


fun invertirCoordenadasString(input: String?, end: Int = 6): String? {
    if (input == null) return null
    val regex = Regex("(\\d+),(\\d+)")
    return regex.replace(input) { matchResult ->
        val x = matchResult.groupValues[1].toInt()
        val y = matchResult.groupValues[2].toInt()
        "${end - x},${end - y}"
    }
}

class Partida(
    private val wsUrl: String = Config.WS_URL
){

    val usarServidor: Boolean get() = !(wsUrl.isEmpty())
    // ----------------------------------------------------
    // MENSAJES QUE ENVIAMOS AL SERVIDOR (Cliente -> Servidor)
    // ----------------------------------------------------
    @Serializable
    sealed class MensajeCliente

    @Serializable
    @SerialName("ESTOY_LISTO")
    object MensajeEstoyListo : MensajeCliente() // Usamos object porque no tiene variables

    @Serializable
    @SerialName("MOVER")
    data class MensajeMover(
        // Borramos el 'val tipo' manual. @SerialName("MOVER") lo añadirá automáticamente.
        val equipo: Int,
        val col_origen: Int,
        val fila_origen: Int,
        val col_destino: Int,
        val fila_destino: Int,
        val carta: String
    ): MensajeCliente()

    @Serializable
    @SerialName("ABANDONAR")
    data class MensajeAbandonar(
        val equipo: Int
    ): MensajeCliente()

    @Serializable
    @SerialName("SOLICITAR_PAUSA")
    data class MensajeSolicitarPausa(
        val remitente: String,
        val destinatario: String, 
        val idPartida: Int
    ) : MensajeCliente()

    @Serializable
    @SerialName("ACEPTAR_PAUSA")
    data class MensajeAceptarPausa(
        val idNotificacion: Int,
        val nombre: String
    ) : MensajeCliente()

    @Serializable
    @SerialName("RECHAZAR_PAUSA")
    data class MensajeRechazarPausa(
        val idNotificacion: Int,
        val nombre: String
    ) : MensajeCliente()

    @Serializable
    @SerialName("PONER_TRAMPA")
    data class MensajePonerTrampa(
        val equipo: Int,
        val fila: Int,
        val columna: Int
    ) : MensajeCliente()

    @Serializable
    @SerialName("CARTA_ACCION")
    data class MensajeSeleccionarCartaAccion(
        val carta: String,
        val equipo: Int
    ) : MensajeCliente()

    @Serializable
    @SerialName("JUGAR_CARTA_ACCION")
    data class MensajeJugarCartaAccion(
        val cartaAccion: String,
        val equipo: Int,
        val x: Int,
        val y: Int,
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault val x_op: Int = -1,
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault val y_op: Int = -1,
        @OptIn(ExperimentalSerializationApi::class)
        @EncodeDefault val cartaRobar: String = "ninguna"
    ) : MensajeCliente()

    // ----------------------------------------------------
    // MENSAJES QUE RECIBIMOS DEL SERVIDOR (Servidor -> Cliente)
    // ----------------------------------------------------
    @Serializable
    sealed class MensajeServidor

    @Serializable
    data class CartaJson(
        val nombre: String
    )

    @Serializable
    data class CartaAccionJson(
        val nombre: String,
        val accion: String,
        val estado: String? = null
    )

    @Serializable
    @SerialName("PARTIDA_ENCONTRADA")
    data class RespuestaPartidaEncontrada(
        val partida_id: Int,
        val equipo: Int,
        val oponente: String,
        val oponentePt: Int,
        val cartas_jugador: List<CartaJson>,
        val cartas_oponente: List<CartaJson>,
        val carta_siguiente: List<CartaJson>,
        val oponente_avatar_id: String? = null,
        val oponente_skin_id: String? = null,
        val tablero_eq1: String? = null,
        val tablero_eq2: String? = null,
        val turno: Int? = null,
        val cartas_accion_jugador: List<CartaAccionJson>? = emptyList(),
        val cartas_accion_oponente: List<CartaAccionJson>? = emptyList(),
        val trampa_j1_pos: String? = null,
        val trampa_j2_pos: String? = null
    ): MensajeServidor() {
        fun obtenerEquipoID(): EquipoID {
            return if (equipo == 1) EquipoID.AZUL else EquipoID.ROJO
        }
    }

    @Serializable
    @SerialName("PARTIDA_PRIVADA_ENCONTRADA")
    data class RespuestaPartidaPrivadaEncontrada(
        val partida_id: Int,
        val equipo: Int,
        val oponente: String,
        val oponentePt: Int,
        val cartas_jugador: List<CartaJson>,
        val cartas_oponente: List<CartaJson>,
        val carta_siguiente: List<CartaJson>,
        val oponente_avatar_id: String? = null,
        val oponente_skin_id: String? = null,
        val tablero_eq1: String? = null,
        val tablero_eq2: String? = null,
        val turno: Int? = null,
        val cartas_accion_jugador: List<CartaAccionJson>? = emptyList(),
        val cartas_accion_oponente: List<CartaAccionJson>? = emptyList(),
        val trampa_j1_pos: String? = null,
        val trampa_j2_pos: String? = null
    ): MensajeServidor() {
        fun toPartidaEncontrada() = RespuestaPartidaEncontrada(
            partida_id = this.partida_id,
            equipo = this.equipo,
            oponente = this.oponente,
            oponentePt = this.oponentePt,
            cartas_jugador = this.cartas_jugador,
            cartas_oponente = this.cartas_oponente,
            carta_siguiente = this.carta_siguiente,
            oponente_avatar_id = this.oponente_avatar_id,
            oponente_skin_id = this.oponente_skin_id,
            tablero_eq1 = this.tablero_eq1,
            tablero_eq2 = this.tablero_eq2,
            turno = this.turno,
            cartas_accion_jugador = this.cartas_accion_jugador,
            cartas_accion_oponente = this.cartas_accion_oponente,
            trampa_j1_pos = this.trampa_j1_pos,
            trampa_j2_pos = this.trampa_j2_pos
        )
    }

    @Serializable
    @SerialName("TU_TURNO")
    object RespuestaTuTurno : MensajeServidor() // Convertido a object

    @Serializable
    @SerialName("MOVER")
    data class RespuestaMover(
        val col_origen: Int,
        val fila_origen: Int,
        val col_destino: Int,
        val fila_destino: Int,
        val carta: String,
        val trampa_activada: Boolean? = false
    ): MensajeServidor()

    @Serializable
    @SerialName("MOVIMIENTO_INVALIDO")
    data class RespuestaMovimientoInvalido(
        val motivo: String,
        val codigo: Int
    ): MensajeServidor()

    @Serializable
    @SerialName("TERMINAR_PARTIDA")
    data class RespuestaTerminarPartida(
        val ganador: String,
        val razon: String
    ): MensajeServidor()

    @Serializable
    @SerialName("VICTORIA")
    data class RespuestaVictoria(
        val motivo: String,
        val equipo_responsable: Int
    ) : MensajeServidor() // Convertido a object

    @Serializable
    @SerialName("DERROTA")
    data class RespuestaDerrota(
        val motivo: String,
        val equipo_responsable: Int
    ) : MensajeServidor() // Convertido a object

    @Serializable
    @SerialName("SOLICITUD_PAUSA")
    data class RespuestaSolicitudPausa(
        val remitente: String,
        val idNotificacion: Int
    ) : MensajeServidor()

    @Serializable
    @SerialName("PARTIDA_PAUSADA")
    object RespuestaPartidaPausada : MensajeServidor()

    @Serializable
    @SerialName("PAUSA_RECHAZADA")
    object RespuestaPausaRechazada : MensajeServidor()

    @Serializable
    @SerialName("SELECCIONE_CARTA_ACCION")
    data class RespuestaSeleccioneCartaAccion(
        val cartas_accion: List<CartaAccionJson>
    ) : MensajeServidor()

    @Serializable
    @SerialName("PARTIDA_LISTA")
    data class RespuestaPartidaLista(
        val cartas_accion: List<CartaAccionJson>
    ) : MensajeServidor()

    @Serializable
    @SerialName("TRAMPA_ACTIVADA")
    data class RespuestaTrampaActivada(
        val columna: Int,
        val fila: Int
    ) : MensajeServidor()

    @Serializable
    @SerialName("CARTA_ACCION_JUGADA")
    data class RespuestaCartaAccionJugada(
        @SerialName("carta_accion")
        val cartaAccion: String,
        val accion: String?,
        val x: Int,
        val y: Int,
        val x_op: Int,
        val y_op: Int,
        @SerialName("carta_robar")
        val cartaRobar: String
    ) : MensajeServidor()

    @Serializable
    @SerialName("TRAMPA_INVALIDA")
    object RespuestaTrampaInvalida : MensajeServidor()

    @Serializable
    @SerialName("CARTA_ACCION_INVALIDA")
    object RespuestaCartaAccionInvalida : MensajeServidor()

    @Serializable
    @SerialName("PEON_MUERTO")
    data class RespuestaPeonMuerto(
        val pos_x: Int,
        val pos_y: Int
    ) : MensajeServidor()

    @Serializable
    @SerialName("PARTIDA_CANCELADA")
    data class RespuestaPartidaCancelada(
        val motivo: String
    ) : MensajeServidor()


    fun desconectarPartida() {
            PartidaActiva.wsEstoyListoEnviado = false
            PartidaActiva.datosPartida = null
    }

    fun enviar(msg: Partida.MensajeCliente): Boolean {

        return try {
            val jsonTexto = jsonPartida.encodeToString(msg)
            ManejadorGlobal.enviarMensaje(jsonTexto)
            true

        } catch (e: Exception) {
            false
        }
    }

    fun enviarEstoyListo(): Boolean{
        if(PartidaActiva.wsEstoyListoEnviado){
            return true
        }
        val msg = MensajeEstoyListo
        val res = enviar(msg)
        if (res) {PartidaActiva.wsEstoyListoEnviado = true}
        return res
    }

    fun enviarMovimiento(datos: MensajeMover): Boolean{
        return enviar(datos)
    }

    fun enviarAbandono(quien: Int):Boolean{
        val msg = MensajeAbandonar(equipo = quien)
        return enviar(msg)
    }

    fun enviarPonerTrampa(
        equipo: Int,
        fila: Int,
        columna: Int
    ) : Boolean {
        return enviar(MensajePonerTrampa(equipo, fila, columna))
    }

    fun enviarSeleccionAccion(
        nombreCartaAccion: String,
        equipo: Int
    ) : Boolean {
        return enviar(MensajeSeleccionarCartaAccion(nombreCartaAccion, equipo))
    }

    fun enviarJugarCartaAccion(
        datos: MensajeJugarCartaAccion
    ) : Boolean {
        return enviar(datos)
    }

    fun enviarSolicitudPausa(
        remitente: String,
        destinatario: String,
        idPartida: Int
    ) : Boolean {
        return enviar(MensajeSolicitarPausa(remitente, destinatario, idPartida))
    }

    fun enviarAceptarPausa(
        idNotificacion: Int,
        miNombre: String
    ) : Boolean {
        return enviar(MensajeAceptarPausa(idNotificacion, miNombre))
    }

    fun enviarRechazarPausa(
        idNotificacion: Int,
        miNombre: String
    ) : Boolean {
        return enviar(MensajeRechazarPausa(idNotificacion, miNombre))
    }

}