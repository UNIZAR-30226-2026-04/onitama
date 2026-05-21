package com.example.onitama.ui.notificaciones

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.api.Amigos
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.api.ManejadorPartidaAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class ViewModelNotificaciones : ViewModel() {
    private val amigosApi = Amigos()
    private val partidaApi = ManejadorPartidaAPI()

    val notificaciones = ManejadorGlobal.notificaciones
    val notificacionesPartida = ManejadorGlobal.notificacionesPartida

    /**
     * Acepta una solicitud de amistad.
     */
    fun aceptar(solicitud: Amigos.MensajeSolicitudAmistadS, destinatario: String) {
        viewModelScope.launch {
            amigosApi.aceptarAmistad(solicitud.remitente, destinatario)
            ManejadorGlobal.eliminarNotificacion(solicitud.idNotificacion)
        }
    }

    /**
     * Rechaza una solicitud de amistad.
     */
    fun rechazar(solicitud: Amigos.MensajeSolicitudAmistadS) {
        viewModelScope.launch {
            amigosApi.rechazarAmistad(solicitud.idNotificacion)
            ManejadorGlobal.eliminarNotificacion(solicitud.idNotificacion)
        }
    }

    fun aceptarInvitacionPartida(idNotificacion: Int, nombreUsuario: String) {
         viewModelScope.launch {
            partidaApi.responderInvitacion(
                idNotificacion,
                nombreUsuario,
                aceptada = true
            )
            ManejadorGlobal.eliminarNotificacion(idNotificacion)
        }
    }

    fun rechazarInvitacionPartida(idNotificacion: Int, nombreUsuario: String) {
         viewModelScope.launch {
            partidaApi.responderInvitacion(
                idNotificacion,
                nombreUsuario,
                aceptada = false
            )
            ManejadorGlobal.eliminarNotificacion(idNotificacion)
        }
    }

    fun aceptarReanudacionPartida(idNotificacion: Int, nombreUsuario: String) {
         viewModelScope.launch {
            partidaApi.responderReanudacion(
                idNotificacion,
                nombreUsuario,
                aceptada = true
            )
            ManejadorGlobal.eliminarNotificacion(idNotificacion)
        }
    }

    fun rechazarReanudacionPartida(idNotificacion: Int, nombreUsuario: String) {
         viewModelScope.launch {
            partidaApi.responderReanudacion(
                idNotificacion,
                nombreUsuario,
                aceptada = false
            )
            ManejadorGlobal.eliminarNotificacion(idNotificacion)
        }
    }
}