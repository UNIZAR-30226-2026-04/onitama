package com.example.onitama.ui.amigos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.AutoLogin
import com.example.onitama.api.Amigos
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona la lógica de 'amigos'.
 */
class ViewModelAmigos : ViewModel() {

    private val api = Amigos()

    private val _raizBuscada = MutableStateFlow("")
    val raizBuscada: StateFlow<String> = _raizBuscada.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _listaAmigos = MutableStateFlow<List<Amigos.Info>>(emptyList())
    val listaAmigos: StateFlow<List<Amigos.Info>> = _listaAmigos.asStateFlow()

    init {
        // Carga inicial de amigos del usuario actual
        cargarAmigos()
    }

    fun busqueda(raiz: String) {
        _raizBuscada.value = raiz
        if (raiz.isEmpty()) {
            cargarAmigos()
        } else {
            // La búsqueda se dispara desde el Flow de abajo o manualmente
            buscar(raiz)
        }
    }

    private fun cargarAmigos() {
        viewModelScope.launch {
            _cargando.value = true
            val user = AutoLogin.sesion.value?.nombre ?: "Jugador"
            val amigos = api.obtenerAmigos(user)
            _listaAmigos.value = amigos
            _cargando.value = false
        }
    }

    private fun buscar(raiz: String) {
        viewModelScope.launch {
            _cargando.value = true
            val resultados = api.buscarJugadores(raiz)
            _listaAmigos.value = resultados
            _cargando.value = false
        }
    }

    fun seguir(destinatario: String) {
        viewModelScope.launch {
            val remitente = AutoLogin.sesion.value?.nombre ?: return@launch
            val exito = api.enviarSolicitudAmistad(remitente, destinatario)
            if (exito) {
                // Podríamos mostrar un mensaje o simplemente recargar
                // En este caso, si estamos buscando, seguimos mostrando la búsqueda
                // pero si estamos en la lista de amigos, recargamos
                if (_raizBuscada.value.isEmpty()) {
                    cargarAmigos()
                } else {
                    // Si queremos que se actualice el estado de "amigo" en la búsqueda,
                    // deberíamos volver a cargar pero sin que se note mucho o simplemente
                    // confiar en que la siguiente carga traerá los datos actualizados.
                    // Por ahora, recargamos misAmigos para que la UI sepa que ya es amigo.
                    actualizarMisAmigos()
                }
            }
        }
    }

    fun dejarDeSeguir(amigoNombre: String) {
        viewModelScope.launch {
            val usuario = AutoLogin.sesion.value?.nombre ?: return@launch
            val exito = api.borrarAmigo(usuario, amigoNombre)
            if (exito) {
                if (_raizBuscada.value.isEmpty()) {
                    cargarAmigos()
                } else {
                    actualizarMisAmigos()
                }
            }
        }
    }

    private suspend fun actualizarMisAmigos() {
        val user = AutoLogin.sesion.value?.nombre ?: return
        val amigos = api.obtenerAmigos(user)
        _listaAmigos.value = amigos
    }
}
