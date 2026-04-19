"use client";

/**
 * WsReconectar – Se monta una sola vez en el RootLayout.
 * Si al cargar (o recargar) la página hay una sesión activa en sessionStorage
 * pero el WebSocket no está abierto, lo reconecta automáticamente.
 * Así el usuario no pierde las notificaciones ni la capacidad de buscar partida
 * después de un F5.
 */

import { useEffect } from "react";
import { leerSesion } from "@/lib/sesion";
import * as WS from "@/api/ws";

export default function WsReconectar() {
  useEffect(() => {
    if (!WS.usarServidor) return;
    // Solo reconectar si hay sesión guardada (el jugador ya hizo login)
    if (leerSesion() && !WS.estaConectado()) {
      WS.conectar().catch(() => {
        // Silencioso: si el servidor no está disponible no bloqueamos la UI
      });
    }
  }, []);

  return null;
}
