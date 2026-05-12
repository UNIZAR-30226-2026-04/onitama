package com.example.onitama.ui.tableros

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.api.Skin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * ViewModel que gestiona la lógica de 'tableros' propios.
 */
class ViewModelTableros : ViewModel() {

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
                Log.d("ViewModelTableros", "Mensaje recibido: $tipo")
                when (tipo) {
                    "TIENDA_SKINS" -> {
                        try {
                            val msg = jsonSerializer.decodeFromString<Skin.MensajeTiendaSkins>(json.toString())
                            // Filtrar solo las que son propiedad del usuario
                            _skins.value = msg.skins.filter { it.owned }
                            _cores.value = msg.cores
                            _skinActivaId.value = msg.skin_activa
                        } catch (e: Exception) {
                            Log.e("ViewModelTableros", "Error al decodificar TIENDA_SKINS", e)
                        }
                    }
                    "SKIN_ACTIVADA" -> {
                        try {
                            val msg = jsonSerializer.decodeFromString<Skin.MensajeSkinActivada>(json.toString())
                            _skinActivaId.value = msg.skin_activa
                            com.example.onitama.AutoLogin.actualizarSkin(msg.skin_activa)
                            // Actualizar el estado de es_activa en la lista local
                            _skins.value = _skins.value.map {
                                it.copy(es_activa = it.skin_id == msg.skin_activa)
                            }
                        } catch (e: Exception) {
                            Log.e("ViewModelTableros", "Error al decodificar SKIN_ACTIVADA", e)
                        }
                    }
                }
            }
        }
    }

    fun obtenerSkins(usuario: String) {
        viewModelScope.launch {
            skinApi.obtenerTiendaSkins(usuario)
        }
    }

    fun activarSkin(usuario: String, skinId: String) {
        viewModelScope.launch {
            skinApi.activarSkin(usuario, skinId)
        }
    }
}