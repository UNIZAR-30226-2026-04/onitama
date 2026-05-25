package com.example.onitama.lib


/** * Requisitos actuales: mínimo 8 caracteres, al menos 1 letra y 1 número.
 * Esta función es una "Top-level function", accesible desde cualquier parte de tu app.
 */
fun validar(contrasena: String): Boolean{
    return contrasena.length >= 8 &&
            contrasena.any { it.isLetter() } &&
            contrasena.any { it.isDigit() }
}