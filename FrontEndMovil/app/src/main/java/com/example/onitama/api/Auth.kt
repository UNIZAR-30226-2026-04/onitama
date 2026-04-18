package com.example.onitama.api

import android.util.Log
import com.example.onitama.Config
import com.example.onitama.DatosPerfil
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.*
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


data class DatosSesion(
    val nombre: String,
    val correo: String,
    val puntos: Int,
    val partidas_ganadas: Int,
    val partidas_jugadas: Int,
    val cores: Int,
    val password: String = ""
)

class Auth(
    // Esta variable se leerá de build.config (cuando la fijemos)
    // No he implementado esto aún, pero he hecho una manera sencilla con un archivo config
    // Para poder ir comprobando cosas con servidro
    private val wsUrl: String = Config.WS_URL
) {

    /** true cuando hay URL de servidor configurada */
    val usarServidor: Boolean get() = !(wsUrl.isEmpty())

    //private val gson = Gson()

    private val client = OkHttpClient()

    // ─── Datos mock para desarrollo sin servidor ──────────────────────────────────
    private val MOCK_USUARIOS = mapOf(
        "IronMaster" to DatosSesion(
            password = "password123",
            nombre = "IronMaster",
            correo = "jugador@onitama.com",
            puntos = 1372,
            partidas_ganadas = 5,
            partidas_jugadas = 10,
            cores = 430
        )
    )

    // ─── Helper: abrir WebSocket y esperar un solo mensaje ────────────────────────
    /**
     * Abre un WebSocket, envía el [mensajeJson], espera una respuesta,
     * devuelve el JSONObject y cierra la conexión.
     */
    suspend fun enviarYEsperarRespuesta(mensajeJson: String): JSONObject {
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(wsUrl).build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("WS_CLIENT", "Conectado. Enviando: $mensajeJson")
                    webSocket.send(mensajeJson)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("WS_CLIENT", "Mensaje recibido del servidor: $text")
                    if (continuation.isActive) {
                        try {
                            val json = JSONObject(text)
                            continuation.resume(json)
                        } catch (e: Exception) {
                            continuation.resumeWithException(Exception("Respuesta inválida del servidor."))
                        }finally {
                            // ¡CLAVE! Cerramos la conexión inmediatamente después de recibir el dato.
                            // 1000 es el código estándar para un cierre normal y exitoso.
                            webSocket.close(1000, "Transacción completada")
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("WS_CLIENT", "Fallo en el socket", t)
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("Error de conexión con el servidor."))
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w("WS_CLIENT", "El servidor cerró la conexión. Razón: $reason")
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("El servidor cerró la conexión sin responder."))
                    }
                }
            }

            val ws = client.newWebSocket(request, listener)

            // Si se cancela la corrutina (ej. por timeout), cerramos el socket
            continuation.invokeOnCancellation {
                Log.w("WS_CLIENT", "Corrutina cancelada. Cerrando conexión.")
                ws.cancel()
            }
        }
    }

    // ─── Inicio de sesión ─────────────────────────────────────────────────────────
    suspend fun iniciarSesion(nombre: String, password: String): DatosSesion {
        // ── Mock ──
        if (!usarServidor) {
            val u = MOCK_USUARIOS[nombre] ?: throw Exception("Usuario no encontrado.")
            if (u.password != password) throw Exception("Contraseña incorrecta.")
            return u
        }

        val requestJson = JSONObject().apply {
            put("tipo", "INICIAR_SESION")
            put("nombre", nombre)
            put("password", password)
        }
        val mensajeIni = requestJson.toString()
        ManejadorGlobal.enviarMensaje(mensajeIni)
        val respuesta = withTimeoutOrNull(10_000L) {
            ManejadorGlobal.mensajesEntrantes
                .filter { json ->
                    val tipo = json.optString("tipo")
                    // Solo dejamos pasar si es ENCONTRADA o ERROR
                    tipo == "INICIO_SESION_EXITOSO" || tipo == "ERROR_SESION_USS" || tipo == "ERROR_SESION_PSSWD"
                }
                .first()
        }?: throw Exception("El servidor no respondió a tiempo.")// Nos quedamos con el PRIMERO que cumpla la condición y dejamos de mirar
        val tipo = respuesta.optString("tipo")
        return when (tipo) {
            "INICIO_SESION_EXITOSO" -> DatosSesion(
                nombre = respuesta.optString("nombre"),
                correo = respuesta.optString("correo"),
                puntos = respuesta.optInt("puntos"),
                partidas_ganadas = respuesta.optInt("partidas_ganadas"),
                partidas_jugadas = respuesta.optInt("partidas_jugadas"),
                cores = respuesta.optInt("cores")
            )

            "ERROR_SESION_USS" -> throw Exception("Usuario no encontrado.")
            "ERROR_SESION_PSSWD" -> throw Exception("Contraseña incorrecta.")
            else -> throw Exception("Respuesta inesperada del servidor.")
        }
    }

    // ─── Registro ─────────────────────────────────────────────────────────────────
    suspend fun registrarUsuario(correo: String, nombre: String, password: String, avatar: String) {
        // ── Mock ──
        if (!usarServidor) {
            return // Simulamos éxito en modo desarrollo
        }

        val requestJson = JSONObject().apply {
            put("tipo", "REGISTRARSE")
            put("correo", correo)
            put("nombre", nombre)
            put("password", password)
            put("avatar_id", avatar)
        }
        ManejadorGlobal.enviarMensaje(requestJson.toString())
        // ── Servidor ──
        val respuesta = withTimeoutOrNull(10_000L) {
            ManejadorGlobal.mensajesEntrantes
                .filter { json ->
                    val tipo = json.optString("tipo")
                    // Solo dejamos pasar si es ENCONTRADA o ERROR
                    tipo == "REGISTRO_EXITOSO" || tipo == "REGISTRO_ERRONEO"
                }
                .first()
        } ?: throw Exception("El servidor no respondió a tiempo.")

        when (respuesta.optString("tipo")) {
            "REGISTRO_EXITOSO" -> return
            "REGISTRO_ERRONEO" -> throw Exception("No se pudo registrar. El usuario o correo ya podría existir.")
            else -> throw Exception("Respuesta inesperada del servidor.")
        }
    }

    // ─── Obtención de perfil en caso de actualización ─────────────────────────────────────────────────────────────────
    suspend fun obtenerPerfil(nombre: String): DatosPerfil? {
        if (!usarServidor) {
            return null
        }


        val requestJson = JSONObject().apply {
            put("tipo", "OBTENER_PERFIL")
            put("nombre", nombre)
        }
        ManejadorGlobal.enviarMensaje(requestJson.toString())

        val respuesta = withTimeoutOrNull(10_000L) {
            ManejadorGlobal.mensajesEntrantes
                .filter { json ->
                    val tipo = json.optString("tipo")
                    // Solo dejamos pasar si es ENCONTRADA o ERROR
                    tipo == "PERFIL_ACTUALIZADO" || tipo == "ERROR"
                }
                .first()
        } ?: throw Exception("El servidor no respondió a tiempo.")
        val tipo = respuesta.optString("tipo")
        when(tipo){
            "PERFIL_ACTUALIZADO" ->{
                val resultado = DatosPerfil(
                    nombre = respuesta.optString("nombre"),
                    correo = respuesta.optString("correo"),
                    puntos = respuesta.optInt("puntos"),
                    partidas_ganadas = respuesta.optInt("partidas_ganadas"),
                    partidas_jugadas = respuesta.optInt("partidas_jugadas"),
                    cores = respuesta.optInt("cores"),
                    avatar_id= respuesta.optString("avatar_id"))
                return resultado

            }
            else -> Log.e("ERROR", "Respuesta inesperada del servidor.")
        }
        return null
    }




}