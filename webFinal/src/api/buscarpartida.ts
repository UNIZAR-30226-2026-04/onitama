/**
 * buscarpartida.ts – Búsqueda de partida pública.
 *
 * Envía BUSCAR_PARTIDA al servidor mediante el WS compartido (ws.ts) y
 * espera PARTIDA_ENCONTRADA. Ya no abre ni transfiere su propia conexión:
 * usa la misma que se abrió al hacer login.
 *
 * Si el servidor no está disponible, el fallback mock sigue funcionando igual.
 */

import * as WS from "./ws";

// ─── Tipos ────────────────────────────────────────────────────────────────────

export interface MensajeBuscarPartida {
  tipo: "BUSCAR_PARTIDA";
  nombre: string;
  puntos: number;
}

export type EstadoPartida = "BUSCANDO" | "ENCONTRADA" | "ERROR" | "TIMEOUT" | "CANCELADO";

export interface RespuestaBuscarPartida {
  estado: EstadoPartida;
  mensaje: string;
  partida_id?: string;
  oponente?: string;
  oponentePt?: number;
}

export interface ResultadoBusqueda {
  promise: Promise<RespuestaBuscarPartida>;
  cancel: () => void;
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
 * Devuelve { promise, cancel } para poder cancelar la búsqueda enviando CANCELAR.
 *
 * Requiere que el WS esté ya abierto (tras el login o registro).
 * Si no hay conexión activa, cae al mock.
 *
 * @param nombre    Nombre de usuario del jugador (para el matchmaking)
 * @param puntos    Puntuación del jugador (matchmaking ±100 puntos)
 * @param timeoutMs Tiempo máximo de espera en ms (default: 45 s)
 */
export function buscarPartida(
  nombre = "Jugador",
  puntos = 0,
  timeoutMs = 45_000
): ResultadoBusqueda {
  if (!WS.usarServidor || !WS.estaConectado()) {
    return {
      promise: mockBuscarPartida(),
      cancel: () => {},
    };
  }

  let resolvePromise!: (r: RespuestaBuscarPartida) => void;
  const promise = new Promise<RespuestaBuscarPartida>((resolve) => {
    resolvePromise = resolve;
  });

  let resuelto = false;
  let unsub: (() => void) | null = null;

  const resolver = (r: RespuestaBuscarPartida) => {
    if (resuelto) return;
    resuelto = true;
    unsub?.();
    resolvePromise(r);
  };

  const timer = setTimeout(() => {
    resolver({
      estado: "TIMEOUT",
      mensaje: "Sin respuesta del servidor. Inténtalo de nuevo.",
    });
  }, timeoutMs);

  const cancel = () => {
    if (resuelto) return;
    clearTimeout(timer);
    WS.enviar({ tipo: "CANCELAR" });
    resolver({ estado: "CANCELADO", mensaje: "Búsqueda cancelada" });
  };

  unsub = WS.suscribirTodos((msg) => {
    if (msg.tipo !== "PARTIDA_ENCONTRADA" && msg.tipo !== "PARTIDA_PRIVADA_ENCONTRADA") return;

    clearTimeout(timer);
    if (resuelto) return;

    // Guardar todos los datos para que /partida/page.tsx los lea al inicializarse
    sessionStorage.setItem("datosPartida", JSON.stringify(msg));

    resolver({
      estado: "ENCONTRADA",
      mensaje: `¡Partida encontrada! Rivalizarás contra @${msg.oponente as string}`,
      partida_id: msg.partida_id as string,
      oponente: msg.oponente as string,
      oponentePt: msg.oponentePt as number,
    });
  });

  const enviado = WS.enviar({ tipo: "BUSCAR_PARTIDA", nombre, puntos } satisfies MensajeBuscarPartida);
  if (!enviado) {
    clearTimeout(timer);
    unsub();
    return {
      promise: mockBuscarPartida(),
      cancel: () => {},
    };
  }

  return { promise, cancel };
}
