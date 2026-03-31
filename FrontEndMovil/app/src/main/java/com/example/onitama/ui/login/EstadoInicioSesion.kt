package com.example.onitama.ui.login

/**
 * Estado que representa el estado de la UI de inicio de sesión.
 */
data class EstadoInicioSesion(
    val nombre: String = "",
    val contrasenya: String = "",
    val error: String? = null,
    val isLoading: Boolean = false,
    val iniciada: Boolean = false
)
