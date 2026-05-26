package com.example.onitama.ui.activities.tienda

import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.AutoLogin
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.api.Skin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * ViewModel que gestiona la lógica de negocio de la tienda de skins (aspectos cosméticos).
 * Coordina las peticiones de compra y activación hacia la API, y reacciona de manera asíncrona
 * a las respuestas del servidor para actualizar el inventario y el saldo del jugador en tiempo real.
 */
class ViewModelTienda : ViewModel() {

    // ─── Estados Mutables (Privados) y de Solo Lectura (Públicos) ─────────────

    /** Catálogo completo de skins disponibles con su estado actual (comprada, precio, etc.) */
    private val _skins = MutableStateFlow<List<Skin.Skin>>(emptyList())
    val skins = _skins.asStateFlow()

    /** Numero de cores actual del usuario */
    private val _cores = MutableStateFlow(0)
    val cores = _cores.asStateFlow()

    /** ID de la skin que el usuario tiene equipada en este momento */
    private val _skinActivaId = MutableStateFlow("")
    val skinActivaId = _skinActivaId.asStateFlow()

    // ─── Proveedor de Datos y Serializador ────────────────────────────────────
    private val skinApi = Skin()

    /** Configuración del parseador JSON para la deserialización segura de paquetes de red */
    private val jsonSerializer = Json {
        ignoreUnknownKeys = true        // Ignora campos nuevos o desconocidos en el JSON para evitar excepciones
        classDiscriminator = "tipo"     // Define el campo clave utilizado para identificar polimorfismo (si aplica)
    }

    // ─── Inicialización ───────────────────────────────────────────────────────
    init {
        observarMensajes() // Inicia la escucha activa del canal de eventos en segundo plano
    }

    /**
     * Se subscribe al flujo global de mensajes entrantes (WebSockets).
     * Analiza el campo "tipo" de cada JSON para actualizar el estado del inventario de forma reactiva.
     */
    private fun observarMensajes() {
        viewModelScope.launch {
            ManejadorGlobal.mensajesEntrantes.collect { json ->
                val tipo = json.optString("tipo")
                Log.d("ViewModelTienda", "Mensaje recibido: $tipo")

                when (tipo) {
                    // Evento 1: Carga o refresco inicial de la tienda
                    "TIENDA_SKINS" -> {
                        try {
                            val msg = jsonSerializer.decodeFromString<Skin.MensajeTiendaSkins>(json.toString())
                            _skins.value = msg.skins
                            _cores.value = msg.cores
                            _skinActivaId.value = msg.skin_activa
                        } catch (e: Exception) {
                            Log.e("ViewModelTienda", "Error al decodificar TIENDA_SKINS", e)
                        }
                    }

                    // Evento 2: Confirmación de compra exitosa de un aspecto
                    "COMPRA_SKIN_OK" -> {
                        try {
                            val msg = jsonSerializer.decodeFromString<Skin.MensajeCompraSkinOk>(json.toString())

                            // 1. Actualiza el saldo en la UI local de la tienda
                            _cores.value = msg.cores
                            // 2. Sincroniza el nuevo saldo en el gestor de sesión global persistente
                            AutoLogin.actualizarCores(msg.cores)

                            // 3. Modifica la lista en memoria marcando como obtenida ('owned = true') la skin comprada
                            _skins.value = _skins.value.map {
                                if (it.skin_id == msg.skin_id) it.copy(owned = true) else it
                            }
                        } catch (e: Exception) {
                            Log.e("ViewModelTienda", "Error al decodificar COMPRA_SKIN_OK", e)
                        }
                    }

                    // Evento 3: Confirmación de equipamiento/activación de skin
                    "SKIN_ACTIVADA" -> {
                        try {
                            val msg = jsonSerializer.decodeFromString<Skin.MensajeSkinActivada>(json.toString())

                            // 1. Actualiza el ID del aspecto activo
                            _skinActivaId.value = msg.skin_activa
                            // 2. Sincroniza el cambio estético en la sesión global
                            AutoLogin.actualizarSkin(msg.skin_activa)

                            // 3. Recorre el catálogo y conmuta la bandera 'es_activa' evaluando si coincide con el nuevo ID
                            _skins.value = _skins.value.map {
                                it.copy(es_activa = it.skin_id == msg.skin_activa)
                            }
                        } catch (e: Exception) {
                            Log.e("ViewModelTienda", "Error al decodificar SKIN_ACTIVADA", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Solicita al servidor el catálogo completo de skins adaptado al contexto del usuario.
     * El servidor responderá de forma asíncrona disparando el evento "TIENDA_SKINS".
     */
    fun obtenerTiendaSkins(usuario: String) {
        viewModelScope.launch {
            skinApi.obtenerTiendaSkins(usuario)
        }
    }

    /**
     * Envía una petición de compra para una skin específica consumiendo "cores" del usuario.
     * El servidor responderá de forma asíncrona disparando el evento "COMPRA_SKIN_OK".
     */
    fun comprarSkin(usuario: String, skinId: String) {
        viewModelScope.launch {
            skinApi.comprarSkin(usuario, skinId)
        }
    }

    /**
     * Solicita equipar/activar una skin que el usuario ya posee en su inventario.
     * El servidor responderá de forma asíncrona disparando el evento "SKIN_ACTIVADA".
     */
    fun activarSkin(usuario: String, skinId: String) {
        viewModelScope.launch {
            skinApi.activarSkin(usuario, skinId)
        }
    }
}