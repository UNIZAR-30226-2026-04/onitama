package com.example.onitama

import com.example.onitama.api.BuscarPartida
import com.example.onitama.api.Partida
import okhttp3.WebSocket


/**
 * Este objeto se usará porque en kotlin no se puede cambiar el listener de un websocket,
 * así que se tiene que guardar en un objeto global para que se pueda acceder al servidor desde
 * múltiples pantallas o activities.
 * **/
object PartidaActiva {

    var wsEstoyListoEnviado: Boolean = false
    var datosPartida: Partida.RespuestaPartidaEncontrada? = null

}