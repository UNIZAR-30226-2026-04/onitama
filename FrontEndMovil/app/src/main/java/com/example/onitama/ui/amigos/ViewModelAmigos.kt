package com.example.onitama.ui.amigos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.AutoLogin
import com.example.onitama.api.Amigos
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.api.ManejadorPartidaAPI
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel que gestiona la lógica de 'amigos'.
 */
class ViewModelAmigos : ViewModel() {

    private val api = Amigos()
    private val manejadorPartidaAPI = ManejadorPartidaAPI()

    private val _raizBuscada = MutableStateFlow("")
    val raizBuscada: StateFlow<String> = _raizBuscada.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _listaJugadores = MutableStateFlow<List<Amigos.Info>>(emptyList())
    val listaJugadores: StateFlow<List<Amigos.Info>> = _listaJugadores.asStateFlow()

    private val _listaAmigos = MutableStateFlow<List<Amigos.Info>>(emptyList())
    val listaAmigos: StateFlow<List<Amigos.Info>> = _listaAmigos.asStateFlow()

    init {
        obtenerAmigos()
        escucharMensajes()
    }

    private fun escucharMensajes() {
        viewModelScope.launch {
            ManejadorGlobal.mensajesEntrantes.collectLatest { json ->
                val tipo = json.optString("tipo")
                if (tipo == "AMISTAD_ACEPTADA" || tipo == "AMIGO_BORRADO") {
                    obtenerAmigos()
                }
            }
        }
    }

    fun enviarPartidaPrivada(
        nombreAmigo: String
    ) {
        val miNombre = AutoLogin.sesion.value?.nombre ?: return
        manejadorPartidaAPI.enviarInvitacion(miNombre, nombreAmigo)
    }

    fun solicitarReanudacion(
        nombreAmigo: String
    ) {
        val miNombre = AutoLogin.sesion.value?.nombre ?: return
        manejadorPartidaAPI.obtenerPartidasPausadas(miNombre, nombreAmigo)
    }

    fun obtenerAmigos() {
        viewModelScope.launch {
            _cargando.value = true
            val usuario = AutoLogin.sesion.value?.nombre ?: ""
            if (usuario.isNotEmpty()) {
                _listaAmigos.value = api.obtenerAmigos(usuario)
            }
            _cargando.value = false
        }
    }

    fun busqueda(query: String) {
        _raizBuscada.value = query
        if (query.isNotEmpty()) {
            viewModelScope.launch {
                _cargando.value = true
                _listaJugadores.value = api.buscarJugadores(query)
                _cargando.value = false
            }
        } else {
            _listaJugadores.value = emptyList()
        }
    }

    fun seguir(nombre: String) {
        viewModelScope.launch {
            val remitente = AutoLogin.sesion.value?.nombre ?: ""
            if (remitente.isNotEmpty() && api.enviarSolicitudAmistad(remitente, nombre)) {
                obtenerAmigos()
            }
        }
    }

    fun dejarDeSeguir(nombre: String) {
        viewModelScope.launch {
            val usuario = AutoLogin.sesion.value?.nombre ?: ""
            if (usuario.isNotEmpty() && api.borrarAmigo(usuario, nombre)) {
                obtenerAmigos()
            }
        }
    }
}
