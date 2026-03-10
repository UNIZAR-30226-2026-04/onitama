package com.example.onitama.lib


/** * Requisitos actuales: mínimo 8 caracteres, al menos 1 letra y 1 número.
 * Esta función es una "Top-level function", accesible desde cualquier parte de tu app.
 */
fun validar(contrasena: String): Boolean{
    return contrasena.length >= 8 &&
            contrasena.any { it.isLetter() } &&
            contrasena.any { it.isDigit() }
}

/**
 * Mensaje de ayuda para los requisitos de contraseña.
 * En Kotlin, las constantes de nivel superior son visibles en todo el módulo.
 */
const val HINT_CONTRASENA = "Usa al menos 8 caracteres con letras y números";