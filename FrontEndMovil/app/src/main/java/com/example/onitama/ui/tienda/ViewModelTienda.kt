package com.example.onitama.ui.tienda

import android.content.Context
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.api.Skin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * ViewModel que gestiona la lógica de la tienda de skins.
 */
class ViewModelTienda : ViewModel() {

    private val _skins = MutableStateFlow<List<Skin.Skin>>(emptyList())
    val skins = _skins.asStateFlow()

    private val _cores = MutableStateFlow(0)
    val cores = _cores.asStateFlow()

    private val _skinActivaId = MutableStateFlow("")
    val skinActivaId = _skinActivaId.asStateFlow()

    private val skinApi = Skin()

    private val jsonSerializer = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "tipo"
    }

    init {
        observarMensajes()
    }

    private fun observarMensajes() {
        viewModelScope.launch {
            ManejadorGlobal.mensajesEntrantes.collect { json ->
                val tipo = json.optString("tipo")
                Log.d("ViewModelTienda", "Mensaje recibido: $tipo")
                when (tipo) {
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
                    "COMPRA_SKIN_OK" -> {
                        try {
                            val msg = jsonSerializer.decodeFromString<Skin.MensajeCompraSkinOk>(json.toString())
                            _cores.value = msg.cores
                            com.example.onitama.AutoLogin.actualizarCores(msg.cores)
                            _skins.value = _skins.value.map {
                                if (it.skin_id == msg.skin_id) it.copy(owned = true) else it
                            }
                        } catch (e: Exception) {
                            Log.e("ViewModelTienda", "Error al decodificar COMPRA_SKIN_OK", e)
                        }
                    }
                    "SKIN_ACTIVADA" -> {
                        try {
                            val msg = jsonSerializer.decodeFromString<Skin.MensajeSkinActivada>(json.toString())
                            _skinActivaId.value = msg.skin_activa
                            com.example.onitama.AutoLogin.actualizarSkin(msg.skin_activa)
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

    fun obtenerTiendaSkins(usuario: String) {
        viewModelScope.launch {
            skinApi.obtenerTiendaSkins(usuario)
        }
    }

    fun comprarSkin(usuario: String, skinId: String) {
        viewModelScope.launch {
            skinApi.comprarSkin(usuario, skinId)
        }
    }

    fun activarSkin(usuario: String, skinId: String) {
        viewModelScope.launch {
            skinApi.activarSkin(usuario, skinId)
        }
    }
}