package com.example.onitama.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.api.Auth
import com.example.onitama.autoLogin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona la lógica de inicio de sesión.
 */
class ViewModelInicioSesion() : ViewModel() {

    private val authClient: Auth = Auth()
    private val _estadoUI = MutableStateFlow(EstadoInicioSesion())
    val uiState: StateFlow<EstadoInicioSesion> = _estadoUI.asStateFlow()

    fun onNombreChange(nombre: String) {
        _estadoUI.value = _estadoUI.value.copy(nombre = nombre)
    }

    fun onContrasenyaChange(contrasenya: String) {
        _estadoUI.value = _estadoUI.value.copy(contrasenya = contrasenya)
    }

    /**
     * Función que se ejecuta al hacer clic en el botón 'Iniciar Sesión'.
     * Comprueba que los datos se han rellenado correctamente y envía
     * la solicitud al servidor.
     */
    fun onEntrarClick(context: Context) {
        val estadoActual = _estadoUI.value
        if (estadoActual.nombre.isEmpty() || estadoActual.contrasenya.isEmpty()) {
            _estadoUI.value = estadoActual.copy(error = "Completa todos los campos")
            return
        }

        viewModelScope.launch {
            _estadoUI.value = estadoActual.copy(isLoading = true, error = null)
            try {

                val datos = authClient.iniciarSesion(
                    estadoActual.nombre, estadoActual.contrasenya
                )
                
                // Guardamos la sesión en el Singleton 'autoLogin'
                autoLogin.inicioSesion(
                    context,
                    datos.nombre,
                    datos.puntos,
                    datos.cores
                )
                
                _estadoUI.value = _estadoUI.value.copy(isLoading = false, iniciada = true)
            } catch (e: Exception) {
                _estadoUI.value = _estadoUI.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al iniciar sesión"
                )
            }
        }
    }
}
