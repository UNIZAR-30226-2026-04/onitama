package com.example.onitama.api

import android.util.Log
import com.example.onitama.Config
import com.example.onitama.PartidaActiva
import com.example.onitama.lib.EquipoID
import kotlinx.coroutines.suspendCancellableCoroutine
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

class Partida(
    private val wsUrl: String = Config.WS_URL
){

    val usarServidor: Boolean get() = (wsUrl != "ws://10.0.2.2:8080")
    @Serializable
    sealed class MensajeServidor

    sealed class MensajeCliente

    data class MensajeEstoyListo(
        val tipo: String = "ESTOY_LISTO",
    ): MensajeCliente()


    data class MensajeMover(
        val tipo: String = "MOVER",
        val equipo: EquipoID,
        val col_origen: Int,
        val fila_origen: Int,
        val col_destino: Int,
        val fila_destino: Int,
        val carta: String

    ): MensajeCliente()

    data class MensajeAbandonar(
        val tipo: String = "ABANDONAR",
        val equipo: EquipoID
    ): MensajeCliente()

    @Serializable
    @SerialName("PARTIDA_ENCONTRADA")
    data class RespuestaPartidaEncontrada(
        val partida_id: String,
        val equipo: EquipoID,
        val oponente: String,
        val oponentePt: Int,
        val cartas_jugador: List<String>,
        val cartas_oponente: List<String>,
        val cartas_siguiente: List<String>

    ): MensajeServidor()

    @Serializable
    @SerialName("TU_TURNO")
    class RespuestaTuTurno : MensajeServidor()

    @Serializable
    @SerialName("MOVER")
    data class RespuestaMover(
        val col_origen: Int,
        val fila_origen: Int,
        val col_destino: Int,
        val fila_destino: Int,
        val carta: String
    ): MensajeServidor()

    @Serializable
    @SerialName("TERMINAR_PARTIDA")
    data class RespuestaTerminarPartida(
        val ganador: String,
        /** Razón: TIEMPO_AGOTADO | REY_CAPTURADO | TRONO | ABANDONO */
        val razon: String
    ): MensajeServidor()

    @Serializable
    @SerialName("VICTORIA")
    class RespuestaVictoria: MensajeServidor()

    @Serializable
    @SerialName("DERROTA")
    class RespuestaDerrota: MensajeServidor()

    data class MensajeDesconocido(
        val tipo: String,
        val other: Map<String, Any?>
    ): MensajeServidor()

    /**
     * Devuelve una función lambda () -> Unit que sirve para desconectar (limpiar).
     */
    fun conectarPartida(onMensaje: (MensajeServidor) -> Unit): () -> Unit {
        if(!usarServidor){
            return {}
        }
        val receptor: (String) -> Unit = { textoJson ->
            try {
                val mensaje = jsonPartida.decodeFromString<MensajeServidor>(textoJson)
                onMensaje(mensaje)
            } catch (e: Exception) {
                Log.e("Partida", "Mensaje no válido o tipo desconocido: $textoJson", e)
            }
        }

        // Si la partida es nueva(será siempre el caso en las públicas): Reusar el WebSocket existente (inyectado desde buscarpartida)
        if (PartidaActiva.wsActivo != null) {
            PartidaActiva.onMensajeJuegoRecibido = receptor

            // Retornamos la función de limpieza (cleanup)
            return { PartidaActiva.onMensajeJuegoRecibido = null }
        }
        //Fallback (abrir conexión nueva si por alguna razón no existe)
        try {
            val client = OkHttpClient()
            val solicitud = Request.Builder().url(wsUrl).build()

            val wsFallback = client.newWebSocket(solicitud, object : WebSocketListener() {
                override fun onMessage(webSocket: WebSocket, text: String) {
                    receptor(text)
                }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    Log.e("Partida", "Error en el WebSocket (Fallback)", t)
                }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    PartidaActiva.wsActivo = null
                }
            })

            PartidaActiva.wsActivo = wsFallback

            //devuelve una función lambda () -> Unit que sirve para desconectar (limpiar)
            return {
                wsFallback.cancel()
                PartidaActiva.wsActivo = null
            }
        } catch (e: Exception) {
            Log.e("Partida", "No se pudo abrir WebSocket.", e)
            return {}
        }

    }

    fun desconectarPartida() {
        if(PartidaActiva.wsActivo != null) {
            PartidaActiva.wsActivo?.close(1000, "Cerrando conexión")
            PartidaActiva.wsActivo = null
            PartidaActiva.wsEstoyListoEnviado = false
        }
    }

    fun enviar(msg: Partida.MensajeCliente): Boolean {
        //si el websocket no está activo no se puede enviar nada
        val ws = PartidaActiva.wsActivo ?: return false

        return try {
            val jsonTexto = jsonPartida.encodeToString(msg)
            ws.send(jsonTexto)

        } catch (e: Exception) {
            false
        }
    }

    fun enviarEstoyListo(): Boolean{
        if(PartidaActiva.wsEstoyListoEnviado){
            return true
        }
        val msg = MensajeEstoyListo()
        val res = enviar(msg)
        if (res) {PartidaActiva.wsEstoyListoEnviado = true}
        return res
    }

    fun enviarMovimiento(datos: MensajeMover): Boolean{
        return enviar(datos)
    }

    fun enviarAbandono(quien: EquipoID):Boolean{
        val msg = MensajeAbandonar(equipo = quien)
        return enviar(msg)
    }

}