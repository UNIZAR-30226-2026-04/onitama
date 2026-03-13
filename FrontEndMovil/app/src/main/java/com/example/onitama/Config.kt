package com.example.onitama

object Config {
    // Para poder realizar la conexión con el servidro
    // Puerto e IP ( Modificar para realizar comprobaciones si usas móvil propio )
    // Por defecto la IP del emulador de AndroidEstudio
    private const val IP = "10.0.2.2"
    private const val PUERTO = "8080"

    const val WS_URL = "ws://$IP:PUERTO"
}