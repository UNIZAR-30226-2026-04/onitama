package com.example.onitama.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.api.Auth
import com.example.onitama.AutoLogin
import com.example.onitama.DatosPerfil
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

                //con esto se inicia sesión
                authClient.iniciarSesion(
                    estadoActual.nombre, estadoActual.contrasenya
                )
                //con esto otro se actualiza el perfil (iniciarsesión no tiene partidas ganadas o jugadas)
                val datos = authClient.obtenerPerfil(estadoActual.nombre)
                
                // Guardamos la sesión en el Singleton 'AutoLogin'
                AutoLogin.inicioSesion(
                    context,
                    datos!!.nombre,
                    datos.puntos,
                    datos.cores
                )
                AutoLogin.actualizar(context, datos as DatosPerfil?)
                
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
