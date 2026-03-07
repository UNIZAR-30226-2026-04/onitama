/**
 * Cliente API – Buscar Partida.
 * Envía una solicitud al servidor Java a través de WebSocket.
 *
 * Cuando NEXT_PUBLIC_WS_URL no está configurada, se usa lógica mock para desarrollo.
 * Cuando el servidor esté listo: crear/actualizar .env.local con:
 *   NEXT_PUBLIC_WS_URL=ws://localhost:8080/ws   (ajustar host/puerto/path)
 * y revisar el contrato de mensajes con el equipo de backend.
 */

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "";

/** Indica si hay un servidor WebSocket configurado */
const usarServidor = !!WS_URL;

// =============================================================================
// Tipos de mensajes (ajustar según el contrato del servidor Java)
// =============================================================================

export interface MensajeBuscarPartida {
  tipo: "BUSCAR_PARTIDA";
  /** Opcional: datos extra que pida el servidor (token, usuario, etc.) */
  payload?: Record<string, unknown>;
}

export type EstadoPartida =
  | "BUSCANDO"       // Buscando oponente
  | "ENCONTRADA"     // Partida encontrada
  | "ERROR"          // Error del servidor
  | "TIMEOUT";       // Sin respuesta a tiempo

export interface RespuestaBuscarPartida {
  estado: EstadoPartida;
  mensaje: string;
  /** ID de la partida asignada por el servidor (solo cuando estado = ENCONTRADA) */
  partida_id?: string;
}

// =============================================================================
// MOCK – Solo para desarrollo sin servidor. Eliminar o ignorar cuando esté listo.
// =============================================================================

function mockBuscarPartida(): Promise<RespuestaBuscarPartida> {
  return new Promise((resolve) => {
    // Simula ~1.5 s de búsqueda antes de responder
    setTimeout(() => {
      resolve({
        estado: "ENCONTRADA",
        mensaje: "¡Partida encontrada! (respuesta mock – servidor aún no conectado)",
        partida_id: "mock-partida-001",
      });
    }, 1500);
  });
}

// =============================================================================
// BUSCAR PARTIDA VÍA WEBSOCKET
// =============================================================================

/**
 * Abre una conexión WebSocket con el servidor Java, envía la solicitud de
 * búsqueda de partida y devuelve la primera respuesta recibida.
 *
 * TODO: Ajustar `WS_URL`, el formato de `MensajeBuscarPartida` y el parseo de
 *       la respuesta cuando el servidor esté implementado.
 *
 * @param timeoutMs  Milisegundos antes de considerar la solicitud como TIMEOUT (default: 30 000)
 */
export function buscarPartida(
  timeoutMs = 30_000
): Promise<RespuestaBuscarPartida> {
  // Sin servidor configurado → usar mock
  if (!usarServidor) {
    return mockBuscarPartida();
  }

  return new Promise((resolve, reject) => {
    let ws: WebSocket;

    try {
      ws = new WebSocket(WS_URL);
    } catch {
      reject(new Error("No se pudo abrir la conexión WebSocket."));
      return;
    }

    // Temporizador de seguridad
    const timer = setTimeout(() => {
      ws.close();
      resolve({
        estado: "TIMEOUT",
        mensaje: "Sin respuesta del servidor. Inténtalo de nuevo.",
      });
    }, timeoutMs);

    ws.onopen = () => {
      // Enviar solicitud al servidor en cuanto se abre la conexión
      const mensaje: MensajeBuscarPartida = { tipo: "BUSCAR_PARTIDA" };
      ws.send(JSON.stringify(mensaje));
    };

    ws.onmessage = (event) => {
      clearTimeout(timer);
      ws.close();

      try {
        const datos: RespuestaBuscarPartida = JSON.parse(event.data as string);
        resolve(datos);
      } catch {
        resolve({
          estado: "ERROR",
          mensaje: "Respuesta del servidor inválida.",
        });
      }
    };

    ws.onerror = () => {
      clearTimeout(timer);
      resolve({
        estado: "ERROR",
        mensaje: "Error de conexión con el servidor.",
      });
    };

    ws.onclose = (event) => {
      // Si se cerró sin que onmessage resolviera la promesa
      if (!event.wasClean) {
        clearTimeout(timer);
        resolve({
          estado: "ERROR",
          mensaje: `Conexión cerrada inesperadamente (código ${event.code}).`,
        });
      }
    };
  });
}

