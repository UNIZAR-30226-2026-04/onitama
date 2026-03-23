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
  if (!WS.usarServidor || !WS.estaConectado() || raiz.trim().length < 2) {
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
