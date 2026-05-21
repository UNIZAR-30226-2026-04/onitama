package com.example.onitama.ui.perfil

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.AutoLogin
import com.example.onitama.DatosPerfil
import com.example.onitama.api.Auth
import com.example.onitama.api.Auth.MensajeCliente
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.api.ManejadorPartidaAPI
import com.example.onitama.api.PartidaReciente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.example.onitama.lib.validar
import com.example.onitama.ui.registro.EstadoRegistro
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable






/**
 * ViewModel que gestiona la lógica de registro.
 */
class ViewModelEditar() : ViewModel() {

    private val authClient: Auth = Auth()
    private val avatarCurrent = MutableStateFlow(AutoLogin.sesion.value?.avatar_id)
    val avatarState: StateFlow<String?> = avatarCurrent.asStateFlow()
    private val newPass1 = MutableStateFlow("")
    private val newPass2 = MutableStateFlow("")
    private val oldPass = MutableStateFlow("")
    val newPass1State: StateFlow<String> = newPass1.asStateFlow()
    val newPass2State: StateFlow<String> = newPass2.asStateFlow()
    val oldPassState: StateFlow<String> = oldPass.asStateFlow()

    private val _partidasRecientes = MutableStateFlow<List<PartidaReciente>>(emptyList())
    val partidasRecientes: StateFlow<List<PartidaReciente>> = _partidasRecientes.asStateFlow()
    fun onPass1Change(contrasenya: String) {
        newPass1.value =  contrasenya
    }

    fun onPass2Change(contrasenya: String) {
        newPass2.value = contrasenya
    }

    fun onOldPassChange(contrasenya: String) {
        oldPass.value = contrasenya
    }


    fun onAvatarChange(avatar: String?){
        avatarCurrent.value = avatar
    }

    /**
     * Función que se ejecuta al hacer clic en el botón 'cambiar avatar'
     */
    fun cambiarPerfil(context: Context, nombre: String, avatar: String?){
        viewModelScope.launch {
            try{
                authClient.cambiarAvatar(nombre, avatar)
                val datos = authClient.obtenerPerfil(nombre)
                AutoLogin.actualizar(context, datos)
            }
            catch (e: Exception){
                Log.e("ERROR", "Error al cambiar el avatar")
            }
        }
    }

    fun cambiarPass(context: Context, nombre: String): Boolean{
        val contrasenya = newPass1.value
        val contrasenyavieja = oldPass.value
        var error = false
        viewModelScope.launch {
            try{
                authClient.cambiarContrasegna(nombre, contrasenyavieja, contrasenya)
                val datos = authClient.obtenerPerfil(nombre)
                AutoLogin.actualizar(context, datos)
                if(AutoLogin.haySesionActiva(context)){
                    //Si tiene la sesión guardada la contraseña guardada debe cambiarse también
                    AutoLogin.mantenerSesion(context, nombre, contrasenya)
                }
            }
            catch (e: Exception){
                error = true
                Log.e("ERROR", "Error al cambiar la contraseña")
            }
        }
        return !error
    }

    fun getPartidas(nombre: String){
        viewModelScope.launch {
            try {
                val misPartidas = ManejadorPartidaAPI().solicitarRecientes(nombre)
                _partidasRecientes.value = misPartidas
                // Aquí ya puedes actualizar tu estado de Compose o tu lista
                Log.d("API", "He recibido ${misPartidas.size} partidas")
            } catch (e: Exception) {
                Log.e("API", "Error: ${e.message}")
            }
        }
    }

}
