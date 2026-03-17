package com.example.onitama

import com.example.onitama.api.BuscarPartida
import okhttp3.WebSocket


/**
 * Este objeto se usará porque en kotlin no se puede cambiar el listener de un websocket,
 * así que se tiene que guardar en un objeto global para que se pueda acceder al servidor desde
 * múltiples pantallas o activities.
 * **/
object PartidaActiva {

    var wsEstoyListoEnviado: Boolean = false
    var datosPartida: BuscarPartida.RespuestaBuscarPartida? = null
    var wsActivo: WebSocket? = null

    // Usaremos esto en la pantalla del juego para recibir los mensajes
    var onMensajeJuegoRecibido: ((String) -> Unit)? = null
}