package com.example.onitama.ui.login

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.api.Auth
import com.example.onitama.AutoLogin
import com.example.onitama.DatosPerfil
import com.example.onitama.api.ManejadorGlobal
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
    fun onEntrarClick(context: Context, keepLogged: Boolean) {
        val estadoActual = _estadoUI.value
        if (estadoActual.nombre.isEmpty() || estadoActual.contrasenya.isEmpty()) {
            _estadoUI.value = estadoActual.copy(error = "Completa todos los campos")
            return
        }

        viewModelScope.launch {
            _estadoUI.value = estadoActual.copy(isLoading = true, error = null)
            try {
                //con esto se inicia sesión
                val conectado = ManejadorGlobal.conectarYMantener()
                if(conectado){
                    authClient.iniciarSesion(
                        estadoActual.nombre, estadoActual.contrasenya
                    )
                    //con esto otro se actualiza el perfil (iniciarsesión no tiene partidas ganadas o jugadas)
                    val datos = authClient.obtenerPerfil(estadoActual.nombre)

                    if(keepLogged && datos != null){
                        AutoLogin.mantenerSesion(context, estadoActual.nombre, estadoActual.contrasenya)
                    }

                    // Guardamos la sesión en el Singleton 'AutoLogin'
                    AutoLogin.inicioSesion(
                        context,
                        datos!!.nombre,
                        datos.puntos,
                        datos.cores,
                        datos.avatar_id,
                        datos.skin_activa,
                    )
                    AutoLogin.actualizar(context, datos as DatosPerfil?)



                    _estadoUI.value = _estadoUI.value.copy(isLoading = false, iniciada = true)
                }
                else{
                    _estadoUI.value = _estadoUI.value.copy(
                        isLoading = false,
                        error =  "Error al conectar al servidor"
                    )
                    Log.e("ERROR", "No se pudo conectar al servidor")
                    ManejadorGlobal.desconectar()
                }
            } catch (e: Exception) {
                if(e.message == "Contraseña incorrecta" )
                _estadoUI.value = _estadoUI.value.copy(
                    isLoading = false,
                    error = e.message ?: "La contraseña introducida no es correcta"
                )
                else if(e.message == "Usuario no encontrado"){
                    _estadoUI.value = _estadoUI.value.copy(
                        isLoading = false,
                        error = e.message ?: "El usuario introducido no existe"
                    )
                }
                else{
                    _estadoUI.value = _estadoUI.value.copy(
                        isLoading = false,
                        error = e.message ?: "Algo ha ido mal, inténtalo de nuevo"
                    )
                    Log.e("ERROR", e.message ?: "Error desconocido")
                }
                ManejadorGlobal.desconectar()
            }
        }
    }
}
