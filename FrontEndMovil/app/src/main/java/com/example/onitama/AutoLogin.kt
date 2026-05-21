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
    val skin_activa: String,
    val avatar_id: String?
)

data class LoginTool(
    val nombre: String,
    val contrasenya: String
)

object AutoLogin {

    private const val NOMBREINICIO = "Onitama"
    private const val HAINICIADO = "yaHaIniciado"
    private const val NOMBRE = "nombre"
    private const val CORREO = "correo"

    private const val PWD = "password"
    private const val JUGADAS = "jugadas"
    private const val GANADAS = "ganadas"
    private const val KATANAS = "katanas"
    private const val AVATAR = "Avatar_id"
    private const val SKIN = "Skin_id"
    private const val CORES = "cores"

    // El estado interno (privado)
    private val _sesion = MutableStateFlow<DatosPerfil?>(null)

    //Versión de solo lectura del estado
    val sesion: StateFlow<DatosPerfil?> = _sesion.asStateFlow()





    private fun obtenerPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(NOMBREINICIO, Context.MODE_PRIVATE)
    }



    fun inicioSesion(context: Context, nombre: String, katanas: Int, cores: Int, avatar: String?, skin: String){
        val pref = obtenerPreferences(context).edit()
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
                skin_activa = skin,
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
        pref.putString(SKIN, datos.skin_activa)
        pref.apply()

        _sesion.value = datos
    }

    fun haySesionActiva(context: Context): Boolean {
        return obtenerPreferences(context).getBoolean(HAINICIADO, false)
    }

    fun obtenerNombre(context: Context): String = obtenerPreferences(context).getString(NOMBRE, "Jugador") ?: "Jugador"
    fun obtenerKatanas(context: Context): Int = obtenerPreferences(context).getInt(KATANAS, 0)
    fun obtenerCores(context: Context): Int = obtenerPreferences(context).getInt(CORES, 0)

    fun actualizarCores( coresNuevo: Int){
        _sesion.value = _sesion.value?.copy(cores = coresNuevo)
    }


    fun actualizarSkin( newSkin: String){
        _sesion.value = _sesion.value?.copy(skin_activa = newSkin)
    }

    fun cerrarSesion(context: Context){
        obtenerPreferences(context).edit().putBoolean(HAINICIADO, false).apply()
        obtenerPreferences(context).edit().clear().apply()
        _sesion.value = null
    }

    fun mantenerSesion(context: Context, nombre: String, contrasenya: String){
        obtenerPreferences(context).edit().putBoolean(HAINICIADO, true).apply()
        obtenerPreferences(context).edit().putString(NOMBRE, nombre).apply()
        obtenerPreferences(context).edit().putString(PWD, contrasenya).apply()
    }

    fun datosIni(context: Context): LoginTool? {
        val pref = obtenerPreferences(context)
        val nombre = pref.getString(NOMBRE, null)
        val pwd = pref.getString(PWD, null)
        if (nombre != null && pwd != null) {
            return LoginTool(nombre, pwd)
        }
        return null
    }
}