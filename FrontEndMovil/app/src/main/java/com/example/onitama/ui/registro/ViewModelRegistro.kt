package com.example.onitama.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.api.Auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.onitama.lib.validar

/**
 * ViewModel que gestiona la lógica de registro.
 */
class ViewModelRegistro() : ViewModel() {

    private val authClient: Auth = Auth()
    private val _estadoUI = MutableStateFlow(EstadoRegistro())
    val uiState: StateFlow<EstadoRegistro> = _estadoUI.asStateFlow()

    fun onNombreChange(nombre: String) {
        _estadoUI.value = _estadoUI.value.copy(nombre = nombre)
    }

    fun onCorreoChange(correo: String) {
        _estadoUI.value = _estadoUI.value.copy(correo = correo)
    }

    fun onContrasenyaChange(contrasenya: String) {
        _estadoUI.value = _estadoUI.value.copy(contrasenya = contrasenya)
    }

    fun onContrasenyaRChange(contrasenyaR: String) {
        _estadoUI.value = _estadoUI.value.copy(contrasenyaR = contrasenyaR)
    }

    /**
     * Función que se ejecuta al hacer clic en el botón 'Registrarse'.
     * Comprueba que los datos se han rellenado correctamente y envía
     * la solicitud al servidor.
     */
    fun onCrearClick() {

        val estadoActual = _estadoUI.value
        if (estadoActual.nombre.isEmpty() || estadoActual.correo.isEmpty()
            || estadoActual.contrasenya.isEmpty()) {
            _estadoUI.value = estadoActual.copy(
                error = "Completa todos los campos"
            )
            return
        }
        if (estadoActual.contrasenya != estadoActual.contrasenyaR) {
            _estadoUI.value = estadoActual.copy(
                error = "Las contraseñas no coinciden"
            )
            return
        }
        if (!validar(estadoActual.contrasenya)) {
            _estadoUI.value = estadoActual.copy(
                error = "La contraseña debe tener al menos 8" +
                        "caracteres, incluyendo letras y números"
            )
            return
        }

        viewModelScope.launch {
            _estadoUI.value = estadoActual.copy(isLoading = true, error = null)
            try {
                authClient.registrarUsuario(
                    estadoActual.correo,
                    estadoActual.nombre,
                    estadoActual.contrasenya
                )
                
                _estadoUI.value = _estadoUI.value.copy(isLoading = false, creada = true)
            } catch (e: Exception) {
                _estadoUI.value = _estadoUI.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al registrar"
                )
            }
        }
    }
}
