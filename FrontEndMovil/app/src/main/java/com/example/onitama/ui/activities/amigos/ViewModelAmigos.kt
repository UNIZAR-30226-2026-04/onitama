package com.example.onitama.ui.activities.amigos

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
 * ViewModel que gestiona la lógica de negocio de la pantalla de 'amigos' y la búsqueda de jugadores.
 * Se encarga de coordinar las peticiones HTTP a la API y reaccionar a eventos en tiempo real
 * a través de flujos de datos asíncronos ([StateFlow]).
 */
class ViewModelAmigos : ViewModel() {

    // ─── Proveedores de Datos y Conectores de red ────────────────────────────
    private val api = Amigos()                             // Servicio encargado de la gestión de relaciones sociales
    private val manejadorPartidaAPI = ManejadorPartidaAPI() // Servicio encargado de la orquestación de partidas/invitaciones

    // ─── Estados Mutables (Privados) y de Solo Lectura (Públicos) ─────────────

    /** Texto que el usuario introduce activamente en la barra de búsqueda */
    private val _raizBuscada = MutableStateFlow("")
    val raizBuscada: StateFlow<String> = _raizBuscada.asStateFlow()

    /** Bandera de control de la UI para renderizar un indicador de carga (ProgressBar) */
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    /** Listado de perfiles globales que coinciden con el patrón de búsqueda actual */
    private val _listaJugadores = MutableStateFlow<List<Amigos.Info>>(emptyList())
    val listaJugadores: StateFlow<List<Amigos.Info>> = _listaJugadores.asStateFlow()

    /** Agenda actual de amigos validados del usuario en sesión */
    private val _listaAmigos = MutableStateFlow<List<Amigos.Info>>(emptyList())
    val listaAmigos: StateFlow<List<Amigos.Info>> = _listaAmigos.asStateFlow()

    // ─── Inicialización ───────────────────────────────────────────────────────
    init {
        obtenerAmigos()     // Carga inicial del listado de amigos al instanciar el componente
        escucharMensajes()  // Registra el receptor de eventos WebSocket/Push en segundo plano
    }

    /**
     * Se subscribe al canal de mensajería entrante global en tiempo real.
     * Si detecta actualizaciones en la base de datos de relaciones, sincroniza la UI local automáticamente.
     */
    private fun escucharMensajes() {
        viewModelScope.launch {
            // collectLatest cancela el procesamiento previo si entra un paquete nuevo de manera inmediata
            ManejadorGlobal.mensajesEntrantes.collectLatest { json ->
                val tipo = json.optString("tipo")
                // Si otro usuario nos acepta o nos borra, forzamos un refresco de la lista
                if (tipo == "AMISTAD_ACEPTADA" || tipo == "AMIGO_BORRADO") {
                    obtenerAmigos()
                }
            }
        }
    }

    /**
     * Despacha una solicitud para invitar a un amigo seleccionado a una partida privada.
     */
    fun enviarPartidaPrivada(
        nombreAmigo: String
    ) {
        val miNombre = AutoLogin.sesion.value?.nombre ?: return
        manejadorPartidaAPI.enviarInvitacion(miNombre, nombreAmigo)
    }

    /**
     * Consulta con el backend si existe alguna sesión de juego guardada o suspendida entre
     * ambos jugadores para ofrecer la opción de retomar el duelo.
     */
    fun solicitarReanudacion(
        nombreAmigo: String
    ) {
        val miNombre = AutoLogin.sesion.value?.nombre ?: return
        manejadorPartidaAPI.obtenerPartidasPausadas(miNombre, nombreAmigo)
    }

    /**
     * Lanza una corrutina asíncrona para consultar el listado actualizado de amigos en el servidor.
     */
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

    /**
     * Ejecuta una consulta de filtrado de jugadores en el servidor basada en un fragmento de texto.
     * @param query Patrón o nombre parcial del jugador a buscar.
     */
    fun busqueda(query: String) {
        _raizBuscada.value = query
        if (query.isNotEmpty()) {
            viewModelScope.launch {
                _cargando.value = true
                _listaJugadores.value = api.buscarJugadores(query)
                _cargando.value = false
            }
        } else {
            // Si la consulta está vacía, limpia el flujo de resultados inmediatamente sin golpear el servidor
            _listaJugadores.value = emptyList()
        }
    }

    /**
     * Envía una solicitud de amistad a otro jugador.
     */
    fun seguir(nombre: String) {
        viewModelScope.launch {
            val remitente = AutoLogin.sesion.value?.nombre ?: ""
            if (remitente.isNotEmpty() && api.enviarSolicitudAmistad(remitente, nombre)) {
                obtenerAmigos()
            }
        }
    }

    /**
     * Borra al jugador del listado de contactos del usuario y borra al usuario del listado de contactos del jugador.
     */
    fun dejarDeSeguir(nombre: String) {
        viewModelScope.launch {
            val usuario = AutoLogin.sesion.value?.nombre ?: ""
            if (usuario.isNotEmpty() && api.borrarAmigo(usuario, nombre)) {
                obtenerAmigos()
            }
        }
    }
}