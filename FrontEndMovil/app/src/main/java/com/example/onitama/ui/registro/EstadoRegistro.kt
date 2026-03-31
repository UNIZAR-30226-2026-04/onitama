package com.example.onitama.ui.registro

/**
 * Estado que representa el estado de la UI de registro.
 */
data class EstadoRegistro(
    val nombre: String = "",
    val correo: String = "",
    val contrasenya: String = "",
    val contrasenyaR: String = "",
    val creada: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
