package com.example.onitama.api

import android.util.Log
import com.example.onitama.Config
import com.example.onitama.DatosPerfil
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Auth(
    private val wsUrl: String = Config.WS_URL
) {

    /** true cuando hay URL de servidor configurada */
    val usarServidor: Boolean get() = !(wsUrl.isEmpty())

    @Serializable
    sealed class MensajeCliente

    @Serializable
    @SerialName("REGISTRARSE")
    data class MensajeRegistrarse(
        val password: String,
        val nombre: String,
        val correo: String,
        val avatar_id: String?
    ): MensajeCliente()

    @Serializable
    @SerialName("INICIAR_SESION")
    data class MensajeIniciarSesion(
        val password: String,
        val nombre: String
    ): MensajeCliente()

    @Serializable
    @SerialName("OBTENER_PERFIL")
    data class MensajeObtenerPerfil(
        val nombre: String
    ): MensajeCliente()


    @Serializable
    @SerialName("CAMBIAR_AVATAR")
    data class MensajeCambiarAvatar(
        val usuario: String,
        val avatar_id: String?,
    ): MensajeCliente()

    @Serializable
    @SerialName("CAMBIAR_CONTRASENA")
    data class MensajeCambiarContrasegna(
        val usuario: String,
        val contrasena_actual: String,
        val contrasena_nueva: String,
    ): MensajeCliente()

    @Serializable
    sealed class MensajeServidor

    @Serializable
    @SerialName("INICIO_SESION_EXITOSO")
    data class MensajeInicioSesionExitoso(
        val nombre: String,
        val puntos: Int,
        val correo: String,
        val partidas_ganadas: Int,
        val partidas_jugadas: Int,
        val cores: Int,
        val skin_activa: String,
        val avatar_id: String
    ) : MensajeServidor()

    @Serializable
    @SerialName("ERROR_SESION_PSSWD")
    object MensajeErrorSesionPsswd : MensajeServidor()

    @Serializable
    @SerialName("ERROR_SESION_USS")
    object MensajeErrorSesionUss : MensajeServidor()

    @Serializable
    @SerialName("REGISTRO_EXITOSO")
    object MensajeRegistroExitoso : MensajeServidor()

    @Serializable
    @SerialName("REGISTRO_ERRONEO")
    object MensajeRegistroErroneo : MensajeServidor()

    @Serializable
    @SerialName("CAMBIO_AVATAR_ERROR")
    object MensajeCambioAvatarError : MensajeServidor()

    @Serializable
    @SerialName("CONTRASENA_CAMBIADA")
    object MensajeCambioContrasena : MensajeServidor()

    @Serializable
    @SerialName("CAMBIO_CONTRASENA_ERROR")
    object MensajeCambioContrasenaError : MensajeServidor()

    @Serializable
    @SerialName("AVATAR_CAMBIADO")
    object MensajeCambioAvatarExitoso : MensajeServidor()

    @Serializable
    @SerialName("PERFIL_ACTUALIZADO")
    data class MensajePerfilActualizado(
        val nombre: String,
        val puntos: Int,
        val correo: String,
        val partidas_ganadas: Int,
        val partidas_jugadas: Int,
        val cores: Int,
        val skin_activa: String,
        val avatar_id: String
    ) : MensajeServidor()

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "tipo"
    }

    // ─── Datos mock para desarrollo sin servidor ──────────────────────────────────
    private val MOCK_USUARIOS = mapOf(
        "IronMaster" to DatosPerfil(
            nombre = "IronMaster",
            correo = "jugador@onitama.com",
            puntos = 1372,
            partidas_ganadas = 5,
            partidas_jugadas = 10,
            cores = 430,
            skin_activa = "Skin0",
            avatar_id = "avatar_01"
        )
    )

    // ─── Inicio de sesión ─────────────────────────────────────────────────────────
    suspend fun iniciarSesion(nombre: String, password: String): DatosPerfil {
        if (!usarServidor) {
            return MOCK_USUARIOS[nombre] ?: throw Exception("Usuario no encontrado (Mock)")
        }

        return try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeIniciarSesion(password, nombre)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)

            // Espera 5 segundos para recibir la respuesta del servidor.
            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("INICIO_SESION_EXITOSO", "ERROR_SESION_PSSWD", "ERROR_SESION_USS")
                    }
                    .first()
                    .toString()
            } ?: throw Exception("Tiempo de espera agotado")

            // Si recibe respuesta la decodifica y obtiene los datos del perfil.
            // Si recibe un mensaje de error lanza una excepción.
            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            when (respuesta) {
                is MensajeInicioSesionExitoso -> {
                    DatosPerfil(
                        nombre = respuesta.nombre,
                        correo = respuesta.correo,
                        puntos = respuesta.puntos,
                        partidas_ganadas = respuesta.partidas_ganadas,
                        partidas_jugadas = respuesta.partidas_jugadas,
                        cores = respuesta.cores,
                        skin_activa = respuesta.skin_activa,
                        avatar_id = respuesta.avatar_id
                    )
                }
                is MensajeErrorSesionPsswd -> throw Exception("Contraseña incorrecta")
                is MensajeErrorSesionUss -> throw Exception("Usuario no encontrado")
                else -> throw Exception("Error desconocido en inicio de sesión")
            }
        } catch (e: Exception) {
            Log.e("Auth_API", "Error al iniciar sesión", e)
            throw e
        }
    }

    // ─── Registro ─────────────────────────────────────────────────────────────────
    suspend fun registrarUsuario(correo: String, nombre: String, password: String, avatar: String?) {
        if (!usarServidor) return

        try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeRegistrarse(password, nombre, correo, avatar)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)

            // Espera 5 segundos para recibir la respuesta del servidor.
            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("REGISTRO_EXITOSO", "REGISTRO_ERRONEO")
                    }
                    .first()
                    .toString()
            } ?: throw Exception("Tiempo de espera agotado")

            // Si recibe respuesta la decodifica, si ha habido algún problema
            // con el registro lanza una excepción.
            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeRegistroErroneo) {
                throw Exception("Error al registrar, ya existe alguien con ese nombre")
            }
        } catch (e: Exception) {
            Log.e("Auth_API", "Error al registrar usuario", e)
            throw e
        }
    }

    // ─── Obtención de perfil en caso de actualización ──────────────────────────────
    suspend fun obtenerPerfil(nombre: String): DatosPerfil? {
        if (!usarServidor) {
            return MOCK_USUARIOS[nombre]
        }

        return try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeObtenerPerfil(nombre)
            // Lo envía a través del websocket.
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            ManejadorGlobal.enviarMensaje(jsonMsg)

            // Espera 5 segundos para recibir la respuesta del servidor.
            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("INICIO_SESION_EXITOSO", "PERFIL_ACTUALIZADO")
                    }
                    .first()
                    .toString()
            } ?: return null

            // Si recibe respuesta la decodifica, y devuelve los datos obtenidos.
            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            when (respuesta) {
                is MensajePerfilActualizado -> {
                    DatosPerfil(
                        nombre = respuesta.nombre,
                        correo = respuesta.correo,
                        puntos = respuesta.puntos,
                        partidas_ganadas = respuesta.partidas_ganadas,
                        partidas_jugadas = respuesta.partidas_jugadas,
                        cores = respuesta.cores,
                        skin_activa = respuesta.skin_activa,
                        avatar_id = respuesta.avatar_id
                    )
                }
                else -> null
            }
        } catch (e: Exception) {
            Log.e("Auth_API", "Error al obtener perfil", e)
            null
        }
    }


    suspend fun cambiarAvatar(nombre: String, avatar: String?) {
        if (!usarServidor) return

        try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeCambiarAvatar(nombre, avatar)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)

            // Espera 5 segundos para recibir la respuesta del servidor.
            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("AVATAR_CAMBIADO", "CAMBIO_AVATAR_ERROR")
                    }
                    .first()
                    .toString()
            } ?: throw Exception("Tiempo de espera agotado")

            // Si recibe respuesta la decodifica, si ha habido algún problema
            // con el registro lanza una excepción.
            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeCambioAvatarError) {
                throw Exception("Error al cambiar el avatar")
            }
        } catch (e: Exception) {
            Log.e("Auth_API", "Error al cambiar el avatar", e)
            throw e
        }
    }

    suspend fun cambiarContrasegna(nombre: String, oldPass: String, newPass: String) {
        if (!usarServidor) return

        try {
            // Convierte el mensaje a enviar al servidor en JSON.
            val mensaje = MensajeCambiarContrasegna(nombre, oldPass, newPass)
            val jsonMsg = jsonSerializer.encodeToString<MensajeCliente>(mensaje)
            // Lo envía a través del websocket.
            ManejadorGlobal.enviarMensaje(jsonMsg)

            val respuestaStr = withTimeoutOrNull(5000L) {
                ManejadorGlobal.mensajesEntrantes
                    .filter { json ->
                        val tipo = json.optString("tipo")
                        tipo in listOf("CONTRASENA_CAMBIADA", "CAMBIO_CONTRASENA_ERROR")
                    }
                    .first()
                    .toString()
            } ?: throw Exception("Tiempo de espera agotado")

            val respuesta = jsonSerializer.decodeFromString<MensajeServidor>(respuestaStr)
            if (respuesta is MensajeCambioContrasenaError) {
                throw Exception("Error al cambiar la contrasegna")
            }
        } catch (e: Exception) {
            Log.e("Auth_API", "Error al cambiar la contrasegna", e)
            throw e
        }
    }
}