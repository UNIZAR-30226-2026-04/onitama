package com.example.onitama.api

/**
 * Cliente API – Buscar Partida Pública.
 * Envía BUSCAR_PARTIDA al servidor vía WebSocket y espera PARTIDA_ENCONTRADA.
 *
 * Cambios respecto a la versión inicial:
 *  - Ahora envía `nombre` y `puntos` para el sistema de matchmaking por puntuación.
 *  - NO cierra el WebSocket al recibir PARTIDA_ENCONTRADA: lo traspasa a
 *    api/partida.ts (setWsActivo) para que la pantalla de partida lo reutilice.
 *  - Guarda los datos completos de la partida en sessionStorage para que
 *    /partida/page.tsx los lea al inicializarse.
 *
 * Si el servidor no está disponible, el fallback mock sigue funcionando igual.
 */