import * as WS from "./ws";
import { TODAS_LAS_CARTAS } from "@/lib/cartas";

export interface CartaEstado {
  nombre: string;
  puntos_necesarios: number;
  descripcion?: string;
}

export interface RespuestaListaCartas {
  tipo: "LISTA_CARTAS";
  cartas: CartaEstado[];
}

export async function obtenerCartas(): Promise<RespuestaListaCartas> {
  if (!WS.usarServidor) {
    return { tipo: "LISTA_CARTAS", cartas: [] };
  }

  // Re-establish socket if lost due to page refresh
  if (!WS.estaConectado()) {
    try {
      await WS.conectar();
    } catch {
      return { tipo: "LISTA_CARTAS", cartas: [] };
    }
  }

  return new Promise<RespuestaListaCartas>((resolve) => {
    const timeout = setTimeout(() => {
      unsubOk();
      resolve({ tipo: "LISTA_CARTAS", cartas: [] });
    }, 5000);

    const unsubOk = WS.suscribir("LISTA_CARTAS", (msg: any) => {
      clearTimeout(timeout);
      unsubOk();
      resolve(msg as RespuestaListaCartas);
    });

    WS.enviar({ tipo: "OBTENER_CARTAS" });
  });
}

export interface RespuestaListaCartasAccion {
  tipo: "LISTA_CARTAS_ACCION";
  cartas: CartaEstado[];
}

export async function obtenerCartasAccion(): Promise<RespuestaListaCartasAccion> {
  if (!WS.usarServidor) {
    return { tipo: "LISTA_CARTAS_ACCION", cartas: [] };
  }

  // Re-establish socket if lost due to page refresh
  if (!WS.estaConectado()) {
    try {
      await WS.conectar();
    } catch {
      return { tipo: "LISTA_CARTAS_ACCION", cartas: [] };
    }
  }

  return new Promise<RespuestaListaCartasAccion>((resolve) => {
    const timeout = setTimeout(() => {
      unsubOk();
      resolve({ tipo: "LISTA_CARTAS_ACCION", cartas: [] });
    }, 5000);

    const unsubOk = WS.suscribir("LISTA_CARTAS_ACCION", (msg: any) => {
      clearTimeout(timeout);
      unsubOk();
      resolve(msg as RespuestaListaCartasAccion);
    });

    WS.enviar({ tipo: "OBTENER_CARTAS_ACCION" });
  });
}
