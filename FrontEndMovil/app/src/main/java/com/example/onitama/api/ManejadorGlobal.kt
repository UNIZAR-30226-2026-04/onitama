package com.example.onitama.api

import android.util.Log
import com.example.onitama.Config
import com.example.onitama.Config.WS_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import kotlin.coroutines.resume

object ManejadorGlobal {
    val wsUrl = Config.WS_URL
    private val client = OkHttpClient()
    // Guardamos la conexión activa aquí
    private var webSocketActivo: WebSocket? = null

    private val _mensajesEntrantes = MutableSharedFlow<JSONObject>(extraBufferCapacity = 10)
    val mensajesEntrantes = _mensajesEntrantes.asSharedFlow()

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "tipo"
    }

    private val _notificaciones = MutableStateFlow<List<Amigos.MensajeSolicitudAmistadS>>(emptyList())
    val notificaciones = _notificaciones.asStateFlow()

    private val _notificacionesPartida = MutableStateFlow<List<Amigos.MensajeServidor>>(emptyList())
    val notificacionesPartida = _notificacionesPartida.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            mensajesEntrantes.collect { json ->
                val tipo = json.optString("tipo")
                try {
                    when(tipo) {
                        "SOLICITUD_AMISTAD" -> {
                            val solicitud =
                                jsonSerializer.decodeFromString<Amigos.MensajeSolicitudAmistadS>(
                                    json.toString()
                                )
                            if (_notificaciones.value.none { it.idNotificacion == solicitud.idNotificacion }) {
                                _notificaciones.value = _notificaciones.value + solicitud
                            }
                        }

                        "INVITACION_PARTIDA", "SOLICITUD_REANUDAR" -> {
                            val solicitud =
                                jsonSerializer.decodeFromString<Amigos.MensajeServidor>(
                                    json.toString()
                                )
                            _notificacionesPartida.value = _notificacionesPartida.value + solicitud
                        }

                        "NOTIFICACION_CANCELADA", "PAUSA_RECHAZADA" -> {
                            val id = json.optInt("idNotificacion")
                            eliminarNotificacion(id)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GESTOR_WS", "Error al decodificar solicitud de amistad", e)
                }
            }
        }
    }

    fun eliminarNotificacion(idNotificacion: Int) {
        _notificaciones.value = _notificaciones.value.filter { it.idNotificacion != idNotificacion }
        _notificacionesPartida.value = _notificacionesPartida.value.filter {
            when (it) {
                is Amigos.MensajeInvitacionPartida -> it.idNotificacion != idNotificacion
                is Amigos.MensajeSolicitudReanudar -> it.idNotificacion != idNotificacion
                else -> true
            }
        }
    }

    // 1. Nueva función para conectar y mantener vivo el socket
    suspend fun conectarYMantener(): Boolean {
        if (webSocketActivo != null) return true
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(wsUrl).build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocketActivo = webSocket
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    webSocketActivo = null
                    if (continuation.isActive) continuation.resume(false)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("GESTOR_WS", "Llegó un mensaje: $text")
                    try {
                        val json = JSONObject(text)

                        // En lugar de procesarlo todo aquí y que este archivo tenga 1000 líneas,
                        // simplemente tiramos el mensaje a la cinta transportadora:
                        CoroutineScope(Dispatchers.IO).launch {
                            _mensajesEntrantes.emit(json)
                        }
                    } catch (e: Exception) {
                        Log.e("GESTOR_WS", "Error leyendo JSON", e)
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    webSocketActivo = null // El servidor o nosotros hemos cerrado la conexión
                }
            }
            client.newWebSocket(request, listener)
        }
    }

    fun enviarMensaje(jsonString: String) {
        webSocketActivo?.send(jsonString) ?: Log.e("GESTOR_WS", "Intentaste enviar pero el tubo está desconectado")
    }

    fun estaConectado(): Boolean {
        return webSocketActivo != null
    }

    fun desconectar() {
        // Al cerrar este "cable", el servidor web se dará cuenta al instante,
        // igual que cuando cierras la pestaña en Chrome, y liberará la cuenta.
        webSocketActivo?.close(1000, "Cierre de sesión voluntario")
        webSocketActivo = null
    }
}