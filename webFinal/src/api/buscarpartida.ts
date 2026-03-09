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

import { setWsActivo } from "./partida";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "";
const usarServidor = !!WS_URL;

// ─── Tipos ────────────────────────────────────────────────────────────────────

export interface MensajeBuscarPartida {
  tipo: "BUSCAR_PARTIDA";
  nombre: string;
  puntos: number;
}

export type EstadoPartida = "BUSCANDO" | "ENCONTRADA" | "ERROR" | "TIMEOUT";

export interface RespuestaBuscarPartida {
  estado: EstadoPartida;
  mensaje: string;
  partida_id?: string;
  oponente?: string;
  oponentePt?: number;
}

// ─── Mock ─────────────────────────────────────────────────────────────────────

function mockBuscarPartida(): Promise<RespuestaBuscarPartida> {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        estado: "ENCONTRADA",
        mensaje: "¡Partida encontrada! (modo local sin servidor)",
        partida_id: "mock-partida-001",
        oponente: "granluchador",
        oponentePt: 1200,
      });
    }, 1500);
  });
}

// ─── Función principal ────────────────────────────────────────────────────────

/**
 * Busca una partida pública enviando BUSCAR_PARTIDA al servidor.
 *
 * @param nombre    Nombre de usuario del jugador (para el matchmaking)
 * @param puntos    Puntuación del jugador (matchmaking ±100 puntos)
 * @param timeoutMs Tiempo máximo de espera en ms (default: 30 s)
 */
export function buscarPartida(
  nombre = "Jugador",
  puntos = 0,
  timeoutMs = 30_000
): Promise<RespuestaBuscarPartida> {
  if (!usarServidor) return mockBuscarPartida();

  return new Promise((resolve) => {
    let ws: WebSocket;

    try {
      ws = new WebSocket(WS_URL);
    } catch {
      console.warn("[buscar] No se pudo abrir WebSocket. Usando mock.");
      mockBuscarPartida().then(resolve);
      return;
    }

    const timer = setTimeout(() => {
      ws.onmessage = null;
      ws.close();
      resolve({ estado: "TIMEOUT", mensaje: "Sin respuesta del servidor. Inténtalo de nuevo." });
    }, timeoutMs);

    ws.onopen = () => {
      const mensaje: MensajeBuscarPartida = {
        tipo: "BUSCAR_PARTIDA",
        nombre,
        puntos,
      };
      ws.send(JSON.stringify(mensaje));
    };

    ws.onmessage = (event) => {
      try {
        const datos = JSON.parse(event.data as string);

        if (datos.tipo === "PARTIDA_ENCONTRADA") {
          clearTimeout(timer);

          // Guardar todos los datos de la partida para que /partida/page.tsx los lea
          sessionStorage.setItem("datosPartida", JSON.stringify(datos));

          // Traspasar el WS a partida.ts (sin cerrarlo) para reutilizarlo en el juego
          ws.onmessage = null; // Limpiar este handler antes de traspasar
          setWsActivo(ws);

          resolve({
            estado: "ENCONTRADA",
            mensaje: `¡Partida encontrada! Rivalizarás contra @${datos.oponente as string}`,
            partida_id: datos.partida_id as string,
            oponente: datos.oponente as string,
            oponentePt: datos.oponentePt as number,
          });
        }
      } catch {
        clearTimeout(timer);
        console.warn("[buscar] Respuesta inválida del servidor. Usando mock.");
        mockBuscarPartida().then(resolve);
      }
    };

    ws.onerror = () => {
      clearTimeout(timer);
      // Servidor no disponible → fallback al mock para no bloquear el desarrollo
      console.warn("[buscar] Servidor no disponible. Usando mock de partida.");
      mockBuscarPartida().then(resolve);
    };

    ws.onclose = (event) => {
      if (!event.wasClean) {
        clearTimeout(timer);
        console.warn("[buscar] Conexión cerrada inesperadamente. Usando mock.");
        mockBuscarPartida().then(resolve);
      }
    };
  });
}
