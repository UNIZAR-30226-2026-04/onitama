/**
 * social.ts – Funciones sociales: buscar jugadores y gestión de amistades.
 *
 * Usa el gestor WS compartido (ws.ts). Requiere que la conexión esté activa
 * (es decir, que el usuario haya iniciado sesión).
 *
 * Mensajes que envía:
 *   BUSCAR_JUGADORES    { tipo, raiz }
 *   SOLICITUD_AMISTAD   { tipo, remitente, destinatario }
 *   ACEPTAR_AMISTAD     { tipo, remitente, destinatario, idNotificacion }
 *   RECHAZAR_AMISTAD    { tipo, idNotificacion }
 *
 * Respuestas que espera:
 *   INFORMACION_JUGADORES  { tipo, info: [{ nombre, puntos }] }
 *   NO_ENCONTRADOS         { tipo }
 *   AMISTAD_ACEPTADA       { tipo, amigo }          (llega a ambos jugadores)
 *   AMISTAD_RECHAZADA      { tipo, usuario }         (llega al remitente)
 *   ERROR_SOLICITUD_AMISTAD { tipo, destinatario }
 *
 * NOTA: La función buscarJugadores requiere que el servidor tenga corregida
 * la consulta SQL en JugadorJDBC.buscarJugadoresPorRaiz (actualmente apunta
 * a la tabla "jugadores" en lugar de "Jugador" y al campo "nombre" en lugar
 * de "Nombre_US"). Hasta que Ciro lo corrija, los resultados serán vacíos.
 */

import * as WS from "./ws";

export interface InfoJugadorBusqueda {
  nombre: string;
  puntos: number;
  avatar_id?: string | null;
}

export interface InfoAmigo {
  nombre: string;
  puntos: number;
  avatar_id?: string | null;
}

export interface ResumenPartidaAmigo {
  partida_id?: number;
  oponente: string;
  estado: string;
  tiempo: number;
  ganador: string;
}

/** Partida pública del historial del jugador (respuesta PARTIDAS_PUBLICAS). */
export interface ResumenPartidaPublica {
  partida_id?: number;
  oponente: string;
  estado: string;
  tiempo: number;
  ganador: string;
}

// ─── Búsqueda de jugadores ────────────────────────────────────────────────────

/**
 * Envía BUSCAR_JUGADORES al servidor y espera INFORMACION_JUGADORES.
 * El servidor filtra por jugadores cuyo Nombre_US empiece por `raiz`.
 * Devuelve [] si no hay resultados, si la raíz es muy corta o si hay error.
 */
export async function buscarJugadores(
  raiz: string
): Promise<InfoJugadorBusqueda[]> {
  if (!WS.usarServidor || !WS.estaConectado() || raiz.trim().length < 1) {
    return [];
  }

  return new Promise<InfoJugadorBusqueda[]>((resolve) => {
    const timeout = setTimeout(() => {
      unsub();
      resolve([]);
    }, 5_000);

    const unsub = WS.suscribirTodos((msg) => {
      if (msg.tipo !== "INFORMACION_JUGADORES" && msg.tipo !== "NO_ENCONTRADOS") return;
      clearTimeout(timeout);
      unsub();
      if (msg.tipo === "NO_ENCONTRADOS") {
        resolve([]);
      } else {
        resolve((msg.info as InfoJugadorBusqueda[]) ?? []);
      }
    });

    WS.enviar({ tipo: "BUSCAR_JUGADORES", raiz });
  });
}

// ─── Amigos ───────────────────────────────────────────────────────────────────

/** Obtiene la lista de amigos del usuario desde el servidor. */
export async function obtenerAmigos(usuario: string): Promise<InfoAmigo[]> {
  if (!WS.usarServidor || !WS.estaConectado()) return [];

  return new Promise<InfoAmigo[]>((resolve) => {
    const timeout = setTimeout(() => {
      unsub();
      resolve([]);
    }, 6_000);

    const unsub = WS.suscribirTodos((msg) => {
      if (
        msg.tipo !== "INFORMACION_AMIGOS" &&
        msg.tipo !== "NO_AMIGOS" &&
        msg.tipo !== "ERROR_AMIGOS"
      ) {
        return;
      }
      clearTimeout(timeout);
      unsub();
      if (msg.tipo === "INFORMACION_AMIGOS") {
        resolve((msg.info as InfoAmigo[]) ?? []);
      } else {
        resolve([]);
      }
    });

    WS.enviar({ tipo: "OBTENER_AMIGOS", usuario });
  });
}

/** Borra una amistad entre usuario y amigo. */
export async function borrarAmigo(usuario: string, amigo: string): Promise<boolean> {
  if (!WS.usarServidor || !WS.estaConectado()) return false;

  return new Promise<boolean>((resolve) => {
    const timeout = setTimeout(() => {
      unsub();
      resolve(false);
    }, 6_000);

    const unsub = WS.suscribirTodos((msg) => {
      if (msg.tipo !== "AMIGO_BORRADO" && msg.tipo !== "ERROR_AL_BORRAR_AMIGO") return;
      clearTimeout(timeout);
      unsub();
      resolve(msg.tipo === "AMIGO_BORRADO");
    });

    WS.enviar({ tipo: "BORRAR_AMIGO", usuario, amigo });
  });
}

/** Obtiene partidas privadas recientes entre usuario y amigo. */
export async function obtenerPartidasConAmigo(
  usuario: string,
  amigo: string
): Promise<ResumenPartidaAmigo[]> {
  if (!WS.usarServidor || !WS.estaConectado()) return [];

  return new Promise<ResumenPartidaAmigo[]>((resolve) => {
    const timeout = setTimeout(() => {
      unsub();
      resolve([]);
    }, 6_000);

    const unsub = WS.suscribirTodos((msg) => {
      if (msg.tipo !== "PARTIDAS_PRIVADAS" && msg.tipo !== "ERROR_AL_BUSCAR_PARTIDAS_PRIV") return;
      clearTimeout(timeout);
      unsub();
      if (msg.tipo !== "PARTIDAS_PRIVADAS") {
        resolve([]);
        return;
      }
      const partidas = Array.isArray(msg.partidas) ? (msg.partidas as unknown[]) : [];
      resolve(
        partidas.map((p) => {
          const item = p as Record<string, unknown>;
          return {
            partida_id:
              typeof item.partida_id === "number"
                ? item.partida_id
                : Number(item.partida_id ?? 0) || undefined,
            oponente: (item.oponente as string) ?? amigo,
            estado: (item.estado as string) ?? "DESCONOCIDO",
            tiempo: Number(item.tiempo ?? 0),
            ganador: ((item.ganador as string) ?? "NO_HAY"),
          };
        })
      );
    });

    WS.enviar({ tipo: "SOLICITAR_PARTIDAS_PRIV", usuario, amigo });
  });
}

/** Historial de partidas públicas del jugador. */
export async function obtenerPartidasPublicas(usuario: string): Promise<ResumenPartidaPublica[]> {
  if (!WS.usarServidor || !WS.estaConectado()) return [];

  return new Promise<ResumenPartidaPublica[]>((resolve) => {
    const timeout = setTimeout(() => {
      unsub();
      resolve([]);
    }, 6_000);

    const unsub = WS.suscribirTodos((msg) => {
      if (msg.tipo !== "PARTIDAS_PUBLICAS" && msg.tipo !== "ERROR_AL_BUSCAR_PARTIDAS_PUB") return;
      clearTimeout(timeout);
      unsub();
      if (msg.tipo !== "PARTIDAS_PUBLICAS") {
        resolve([]);
        return;
      }
      const partidas = Array.isArray(msg.partidas) ? (msg.partidas as unknown[]) : [];
      resolve(
        partidas.map((p) => {
          const item = p as Record<string, unknown>;
          return {
            partida_id:
              typeof item.partida_id === "number"
                ? item.partida_id
                : Number(item.partida_id ?? 0) || undefined,
            oponente: (item.oponente as string) ?? "?",
            estado: (item.estado as string) ?? "DESCONOCIDO",
            tiempo: Number(item.tiempo ?? 0),
            ganador: (item.ganador as string) ?? "NO_HAY",
          };
        })
      );
    });

    WS.enviar({ tipo: "SOLICITAR_PARTIDAS_PUB", usuario });
  });
}

// ─── Solicitudes de amistad ───────────────────────────────────────────────────

/**
 * Envía una solicitud de amistad al destinatario.
 * El servidor guarda la notificación en BD y la envía al destinatario si
 * está conectado; si no, la recibirá en su próximo login.
 * Devuelve false si no hay conexión WS activa.
 */
export function enviarSolicitudAmistad(
  remitente: string,
  destinatario: string
): boolean {
  return WS.enviar({ tipo: "SOLICITUD_AMISTAD", remitente, destinatario });
}

/**
 * Acepta una solicitud de amistad recibida.
 * @param remitente       Quien envió la solicitud (el futuro amigo)
 * @param destinatario    El jugador actual (quien acepta)
 * @param idNotificacion  Id de la notificación a eliminar en BD
 */
export function aceptarSolicitudAmistad(
  remitente: string,
  destinatario: string,
  idNotificacion: number
): boolean {
  return WS.enviar({
    tipo: "ACEPTAR_AMISTAD",
    remitente,
    destinatario,
    idNotificacion,
  });
}

/**
 * Rechaza una solicitud de amistad.
 * El servidor elimina la notificación de la BD.
 */
export function rechazarSolicitudAmistad(idNotificacion: number): boolean {
  return WS.enviar({ tipo: "RECHAZAR_AMISTAD", idNotificacion });
}

// ─── Partidas privadas ────────────────────────────────────────────────────────

/** Envía una invitación de partida privada a un amigo. */
export function enviarInvitacionPartidaPrivada(
  remitente: string,
  destinatario: string
): boolean {
  return WS.enviar({ tipo: "INVITACION_PARTIDA", remitente, destinatario });
}

/** Acepta una invitación a partida privada. */
export function aceptarInvitacionPartidaPrivada(idNotificacion: number): boolean {
  return WS.enviar({ tipo: "ACEPTAR_INVITACION", idNotificacion });
}

/** Rechaza una invitación a partida privada. */
export function rechazarInvitacionPartidaPrivada(idNotificacion: number): boolean {
  return WS.enviar({ tipo: "RECHAZAR_INVITACION", idNotificacion });
}
