package com.example.onitama

import android.content.Context
import android.content.SharedPreferences


object autoLogin {

    private const val NOMBREINICIO = "Onitama"
    private const val HAINICIADO = "yaHaIniciado"
    private const val NOMBRE = "nombre"
    private const val KATANAS = "katanas"
    private const val CORES = "cores"

    private fun obtenerPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(NOMBREINICIO, Context.MODE_PRIVATE)
    }

    // Funciónn que guarda los datos tras hacer el login
    fun inicioSesion(context: Context, nombre: String, katanas: Int, cores: Int){
        val pref = obtenerPreferences(context).edit()
        pref.putBoolean(HAINICIADO, true)
        pref.putString(NOMBRE, nombre)
        pref.putInt(KATANAS, katanas)
        pref.putInt(CORES, cores)
        pref.apply()
    }

    // Función que verifica si el usuario ya ha iniciado sesión
    fun yaHaIniciadoSesion(context: Context): Boolean {
        val haIniciado = obtenerPreferences(context).getBoolean(HAINICIADO, false)
        return haIniciado
    }

    // Funciones para obtener los datos del usuario
    // Usuario ya registrado
    fun obtenerNombre(context: Context): String {
        val nombre = obtenerPreferences(context).getString(NOMBRE, "Jugador") ?: "Jugador"
        return nombre
    }

    fun obtenerKatanas(context: Context): Int {
        val katanas = obtenerPreferences(context).getInt(KATANAS, 0)
        return katanas
    }

    fun obtenerCores(context: Context): Int {
        val cores = obtenerPreferences(context).getInt(CORES, 0)
        return cores
    }

    // Función para cerrar la sesión
    fun cerrarSesion(context: Context){
        obtenerPreferences(context).edit().clear().apply()
    }
}