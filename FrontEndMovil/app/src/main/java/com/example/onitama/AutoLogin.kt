package com.example.onitama

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Representa la información detallada del perfil de un usuario.
 */
data class DatosPerfil(
    val nombre: String,
    val correo: String,
    val puntos: Int,            // Representados internamente en el almacenamiento como "katanas"
    val partidas_ganadas: Int,
    val partidas_jugadas: Int,
    val cores: Int,             // Divisa o créditos especiales dentro del juego
    val skin_activa: String,    // ID o nombre cosmético del tablero/fichas activo
    val avatar_id: String?      // ID del icono o foto de perfil del usuario
)

/**
 * Estructura de datos simple para almacenar las credenciales básicas
 * necesarias durante el proceso de login automatizado.
 */
data class LoginTool(
    val nombre: String,
    val contrasenya: String
)

/**
 * Gestor de sesión centralizado (Singleton).
 * Se encarga del inicio, mantenimiento, actualización y cierre de sesión del usuario,
 * combinando persistencia local en disco ([SharedPreferences]) con un estado reactivo en memoria ([StateFlow]).
 */
object AutoLogin {

    // ─── Claves de almacenamiento para SharedPreferences ──────────────────────
    private const val NOMBREINICIO = "Onitama"     // Nombre del archivo XML de preferencias
    private const val HAINICIADO = "yaHaIniciado"   // Flag booleano para recordar si hay sesión activa
    private const val NOMBRE = "nombre"
    private const val CORREO = "correo"
    private const val PWD = "password"             // Contraseña encriptada o plana para el login automático
    private const val JUGADAS = "jugadas"
    private const val GANADAS = "ganadas"
    private const val KATANAS = "katanas"          // Equivalente a los puntos en DatosPerfil
    private const val AVATAR = "Avatar_id"
    private const val SKIN = "Skin_id"
    private const val CORES = "cores"

    // ─── Flujos de Estado Reactivos (Coroutines Architecture) ─────────────────

    /** Estado interno mutable de la sesión actual; null indica que no se ha iniciado sesión. */
    private val _sesion = MutableStateFlow<DatosPerfil?>(null)

    /** Versión pública de solo lectura expuesta a la interfaz de usuario (UI) para reaccionar a cambios. */
    val sesion: StateFlow<DatosPerfil?> = _sesion.asStateFlow()

    /**
     * Obtiene el acceso al archivo de preferencias compartidas en modo privado.
     */
    private fun obtenerPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(NOMBREINICIO, Context.MODE_PRIVATE)
    }

    /**
     * Registra el inicio de sesión inicial guardando datos clave en local
     * y actualizando/creando el estado reactivo en memoria.
     */
    fun inicioSesion(context: Context, nombre: String, katanas: Int, cores: Int, avatar: String?, skin: String){
        // Guardado persistente en disco
        val pref = obtenerPreferences(context).edit()
        pref.putString(NOMBRE, nombre)
        pref.putInt(KATANAS, katanas)
        pref.putInt(CORES, cores)
        pref.putString(AVATAR, avatar)
        pref.apply() // Ejecución asíncrona en segundo plano

        val estadoActual = _sesion.value

        // Actualización del estado reactivo en memoria
        if (estadoActual != null) {
            // Si ya existía una estructura de perfil previa, modifica solo los campos nuevos
            _sesion.value = estadoActual.copy(
                nombre = nombre,
                puntos = katanas,
                cores = cores,
                avatar_id = avatar
            )
        } else {
            // Si la sesión arranca limpia, instancia un nuevo objeto DatosPerfil con valores por defecto
            _sesion.value = DatosPerfil(
                nombre = nombre,
                correo = "",
                puntos = katanas,
                partidas_ganadas = 0,
                partidas_jugadas = 0,
                cores = cores,
                skin_activa = skin,
                avatar_id = avatar
            )
        }
    }

    /**
     * Sobrescribe de forma masiva tanto el archivo local en disco como el flujo reactivo
     * con una nueva estructura completa de [DatosPerfil].
     */
    fun actualizar(context: Context, datos: DatosPerfil?){
        if (datos == null) return

        // Actualización masiva de las preferencias locales
        val pref = obtenerPreferences(context).edit()
        pref.putString(NOMBRE, datos.nombre)
        pref.putString(CORREO, datos.correo)
        pref.putInt(KATANAS, datos.puntos)
        pref.putInt(CORES, datos.cores)
        pref.putInt(JUGADAS, datos.partidas_jugadas)
        pref.putInt(GANADAS, datos.partidas_ganadas)
        pref.putString(AVATAR, datos.avatar_id)
        pref.putString(SKIN, datos.skin_activa)
        pref.apply()

        // Sincronización del flujo en memoria
        _sesion.value = datos
    }

    /**
     * Comprueba si existe una marca que valide que el usuario no cerró explícitamente su última sesión.
     * @return true si se puede omitir la pantalla de login tradicional.
     */
    fun haySesionActiva(context: Context): Boolean {
        return obtenerPreferences(context).getBoolean(HAINICIADO, false)
    }

    // ─── Getters directos desde almacenamiento local ──────────────────────────
    fun obtenerNombre(context: Context): String = obtenerPreferences(context).getString(NOMBRE, "Jugador") ?: "Jugador"
    fun obtenerKatanas(context: Context): Int = obtenerPreferences(context).getInt(KATANAS, 0)
    fun obtenerCores(context: Context): Int = obtenerPreferences(context).getInt(CORES, 0)

    /**
     * Modifica únicamente los Cores (créditos) en el estado en memoria.
     * *Nota:* Este cambio no se persiste automáticamente en SharedPreferences hasta que se llame a [actualizar].
     */
    fun actualizarCores(coresNuevo: Int){
        _sesion.value = _sesion.value?.copy(cores = coresNuevo)
    }

    /**
     * Modifica únicamente la Skin estética en el estado en memoria.
     * *Nota:* Este cambio no se persiste automáticamente en SharedPreferences hasta que se llame a [actualizar].
     */
    fun actualizarSkin(newSkin: String){
        _sesion.value = _sesion.value?.copy(skin_activa = newSkin)
    }

    /**
     * Borra todo rastro de datos de usuario guardados en la aplicación
     * y desconecta los observadores de la interfaz limpiando el flujo en memoria.
     */
    fun cerrarSesion(context: Context){
        obtenerPreferences(context).edit().putBoolean(HAINICIADO, false).apply()
        obtenerPreferences(context).edit().clear().apply() // Limpia por completo todas las claves del XML
        _sesion.value = null // Notifica a la UI que la sesión es nula
    }

    /**
     * Almacena de forma persistente las credenciales del usuario y activa la bandera
     * para que la app recuerde la sesión en los próximos arranques de la aplicación.
     */
    fun mantenerSesion(context: Context, nombre: String, contrasenya: String){
        obtenerPreferences(context).edit().putBoolean(HAINICIADO, true).apply()
        obtenerPreferences(context).edit().putString(NOMBRE, nombre).apply()
        obtenerPreferences(context).edit().putString(PWD, contrasenya).apply()
    }

    /**
     * Recupera las credenciales locales de la última sesión guardada.
     * Utilizado habitualmente por los servicios o repositorios en el Splash Screen para re-autenticar contra la API.
     * @return Objeto [LoginTool] si existen credenciales guardadas, o null en caso contrario.
     */
    fun datosIni(context: Context): LoginTool? {
        val pref = obtenerPreferences(context)
        val nombre = pref.getString(NOMBRE, null)
        val pwd = pref.getString(PWD, null)

        // Solo devuelve la estructura si ambas partes de la credencial están presentes
        if (nombre != null && pwd != null) {
            return LoginTool(nombre, pwd)
        }
        return null
    }
}