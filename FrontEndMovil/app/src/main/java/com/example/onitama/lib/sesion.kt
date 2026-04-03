package com.example.onitama.lib

data class DatosSesion(
    val nombre: String,
    val correo: String,
    val puntos: Int,
    val partidas_ganadas: Int,
    val partidas_jugadas: Int,
    val cores: Int,
    val skin_activa: String?,
    val avatar_id: String?
)

fun guardarSesion(datos: DatosSesion): Unit {

}