package com.example.onitama.api

/**
 * Cliente WebSocket para la partida en curso.
 *
 * Mensajes cliente → servidor:
 *   ESTOY_LISTO  – la pantalla de partida está cargada y lista
 *   MOVER        – el jugador mueve una ficha
 *   ABANDONAR    – el jugador abandona voluntariamente la partida
 *
 * Mensajes servidor → cliente:
 *   TU_TURNO         – el servidor autoriza al cliente a mover (primer turno)
 *   MOVER            – el oponente ha movido; se retransmite al otro cliente
 *   TERMINAR_PARTIDA – la partida ha terminado (tiempo, victoria, abandono)
 *
 * NOTA sobre nomenclatura: el diagrama de secuencia del backend usa
 * QUIERO_JUGAR / EMPIEZA_PARTIDA, pero el servidor (Servidor.java) implementa
 * BUSCAR_PARTIDA / PARTIDA_ENCONTRADA. Se usan estos últimos para coherencia
 * con el código del servidor existente.
 */

//const val WS_URL =