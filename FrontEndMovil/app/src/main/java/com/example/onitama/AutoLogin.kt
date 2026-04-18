package com.example.onitama

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DatosPerfil(
    val nombre: String,
    val correo: String,
    val puntos: Int,
    val partidas_ganadas: Int,
    val partidas_jugadas: Int,
    val cores: Int,
    val avatar_id: String
)

object AutoLogin {

    private const val NOMBREINICIO = "Onitama"
    private const val HAINICIADO = "yaHaIniciado"
    private const val NOMBRE = "nombre"
    private const val CORREO = "correo"
    private const val JUGADAS = "jugadas"
    private const val GANADAS = "ganadas"
    private const val KATANAS = "katanas"

    private const val AVATAR = "Avatra_id"
    private const val CORES = "cores"

    // El estado interno (privado)
    private val _sesion = MutableStateFlow<DatosPerfil?>(null)

    //Versión de solo lectura del estado
    val sesion: StateFlow<DatosPerfil?> = _sesion.asStateFlow()


    private fun obtenerPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(NOMBREINICIO, Context.MODE_PRIVATE)
    }

    fun cargarDatosAlIniciarApp(context: Context) {
        if (yaHaIniciadoSesion(context)) {
            val pref = obtenerPreferences(context)
            _sesion.value = DatosPerfil(
                nombre = pref.getString(NOMBRE, "Jugador") ?: "Jugador",
                correo = pref.getString(CORREO, "") ?: "",
                puntos = pref.getInt(KATANAS, 0),
                partidas_ganadas = pref.getInt(GANADAS, 0),
                partidas_jugadas = pref.getInt(JUGADAS, 0),
                cores = pref.getInt(CORES, 0),
                avatar_id = pref.getString(AVATAR, "")?: ""
            )
        }
    }

    fun inicioSesion(context: Context, nombre: String, katanas: Int, cores: Int, avatar: String){
        val pref = obtenerPreferences(context).edit()
        pref.putBoolean(HAINICIADO, true)
        pref.putString(NOMBRE, nombre)
        pref.putInt(KATANAS, katanas)
        pref.putInt(CORES, cores)
        pref.putString(AVATAR, avatar)
        pref.apply()

        val estadoActual = _sesion.value

        if (estadoActual != null) {
            _sesion.value = estadoActual.copy(
                nombre = nombre,
                puntos = katanas,
                cores = cores,
                avatar_id = avatar
            )
        } else {
            _sesion.value = DatosPerfil(
                nombre = nombre,
                correo = "",
                puntos = katanas,
                partidas_ganadas = 0,
                partidas_jugadas = 0,
                cores = cores,
                avatar_id = avatar
            )
        }
    }

    fun actualizar(context: Context, datos: DatosPerfil?){
        if (datos == null) return

        val pref = obtenerPreferences(context).edit()
        pref.putString(NOMBRE, datos.nombre)
        pref.putString(CORREO, datos.correo)
        pref.putInt(KATANAS, datos.puntos)
        pref.putInt(CORES, datos.cores)
        pref.putInt(JUGADAS, datos.partidas_jugadas)
        pref.putInt(GANADAS, datos.partidas_ganadas)
        pref.putString(AVATAR, datos.avatar_id)
        pref.apply()

        _sesion.value = datos
    }

    fun yaHaIniciadoSesion(context: Context): Boolean {
        return obtenerPreferences(context).getBoolean(HAINICIADO, false)
    }

    fun obtenerNombre(context: Context): String = obtenerPreferences(context).getString(NOMBRE, "Jugador") ?: "Jugador"
    fun obtenerKatanas(context: Context): Int = obtenerPreferences(context).getInt(KATANAS, 0)
    fun obtenerCores(context: Context): Int = obtenerPreferences(context).getInt(CORES, 0)

    fun cerrarSesion(context: Context){
        obtenerPreferences(context).edit().clear().apply()
        _sesion.value = null
        //ws?.close(1000, "Sesión cerrada por el usuario")
    }
}