package com.example.onitama.api

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
    private val wsUrl: String = ""
) {

    /** true cuando hay URL de servidor configurada */
    val usarServidor: Boolean get() = wsUrl.isNotEmpty()

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
    private suspend fun enviarYEsperarRespuesta(mensajeJson: String): JSONObject {
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(wsUrl).build()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(mensajeJson)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    webSocket.close(1000, "Cierre normal") // Cierra temporalmente como en la web
                    if (continuation.isActive) {
                        try {
                            val json = JSONObject(text)
                            continuation.resume(json)
                        } catch (e: Exception) {
                            continuation.resumeWithException(Exception("Respuesta inválida del servidor."))
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(Exception("Error de conexión con el servidor."))
                    }
                }
            }

            val ws = client.newWebSocket(request, listener)

            // Si se cancela la corrutina (ej. por timeout), cerramos el socket
            continuation.invokeOnCancellation {
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

        // ── Servidor ──
        // Envolvemos en un timeout de 10 segundos
        val respuesta = withTimeoutOrNull(10_000L) {
            val requestJson = JSONObject().apply {
                put("tipo", "INICIAR_SESION")
                put("nombre", nombre)
                put("password", password)
            }

            enviarYEsperarRespuesta(requestJson.toString())
        } ?: throw Exception("El servidor no respondió a tiempo.")

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
    suspend fun registrarUsuario(correo: String, nombre: String, password: String) {
        // ── Mock ──
        if (!usarServidor) {
            return // Simulamos éxito en modo desarrollo
        }

        // ── Servidor ──
        val respuesta = withTimeoutOrNull(10_000L) {
            val requestJson = JSONObject().apply {
                put("tipo", "REGISTRARSE")
                put("correo", correo)
                put("nombre", nombre)
                put("password", password)
            }

            enviarYEsperarRespuesta(requestJson.toString())
        } ?: throw Exception("El servidor no respondió a tiempo.")

        when (respuesta.optString("tipo")) {
            "REGISTRO_EXITOSO" -> return
            "REGISTRO_ERRONEO" -> throw Exception("No se pudo registrar. El usuario o correo ya podría existir.")
            else -> throw Exception("Respuesta inesperada del servidor.")
        }
    }
}