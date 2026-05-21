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
 * No hay timeout en el cliente: el servidor gestiona sus propios tiempos.
 * El usuario puede cancelar manualmente en cualquier momento.
 */
export function buscarPartida(
  nombre = "Jugador",
  puntos = 0
): ResultadoBusqueda {
  if (!WS.usarServidor) {
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

  const cancel = () => {
    if (resuelto) return;
    WS.enviar({ tipo: "CANCELAR" });
    resolver({ estado: "CANCELADO", mensaje: "Búsqueda cancelada" });
  };

  void (async () => {
    try {
      await WS.conectar();
      if (resuelto) return;

      if (!WS.estaConectado()) {
        resolver(await mockBuscarPartida());
        return;
      }

      const handler = (msg: WS.MensajeWS) => {
        if (resuelto) return;
        // Guardar todos los datos para que /partida/page.tsx los lea al inicializarse.
        // partida_nueva: true indica que es un juego nuevo → frontend arranca en COLOCAR_TRAMPA
        sessionStorage.setItem("datosPartida", JSON.stringify({ ...msg, partida_nueva: true }));

        resolver({
          estado: "ENCONTRADA",
          mensaje: `¡Partida encontrada! Rivalizarás contra @${msg.oponente as string}`,
          partida_id: msg.partida_id as string,
          oponente: msg.oponente as string,
          oponentePt: msg.oponentePt as number,
        });
      };

      unsub = WS.suscribir("PARTIDA_ENCONTRADA", handler);

      if (resuelto) {
        unsub();
        return;
      }

      const enviado = WS.enviar({ tipo: "BUSCAR_PARTIDA", nombre, puntos } satisfies MensajeBuscarPartida);
      if (!enviado) {
        resolver(await mockBuscarPartida());
      }
    } catch {
      if (!resuelto) {
        resolver({
          estado: "ERROR",
          mensaje: "No se pudo conectar al servidor. Inténtalo de nuevo.",
        });
      }
    }
  })();

  return { promise, cancel };
}
