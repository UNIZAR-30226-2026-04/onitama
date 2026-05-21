/**
 * partida.ts – Mensajes WebSocket durante una partida en curso.
 *
 * Ahora usa el gestor WS compartido (ws.ts) en lugar de gestionar su propia
 * conexión. El WebSocket ya está abierto desde el login, por lo que este
 * módulo solo necesita suscribirse a los mensajes relevantes y enviar los suyos.
 *
 * Mensajes cliente → servidor:
 *   MOVER     – el jugador mueve una ficha
 *   ABANDONAR – el jugador abandona voluntariamente la partida
 *
 * Mensajes servidor → cliente (recibidos vía conectarPartida):
 *   MOVER    – el oponente ha movido
 *   VICTORIA – este cliente ha ganado
 *   DERROTA  – este cliente ha perdido
 */

import * as WS from "./ws";

export const usarServidor = WS.usarServidor;

// ─── Tipos de mensajes ────────────────────────────────────────────────────────

/**
 * Movimiento enviado al servidor.
 * No incluye partida_id porque el servidor identifica la partida
 * a partir de la conexión WebSocket (conn) de cada jugador.
 */
export interface MensajeMover {
  tipo: "MOVER";
  /** Equipo del jugador que mueve: 1 = arriba (rojo), 2 = abajo (azul) */
  equipo: 1 | 2;
  col_origen: number;
  fila_origen: number;
  col_destino: number;
  fila_destino: number;
  carta: string;
}

/** El jugador abandona voluntariamente; el servidor declarará ganador al rival */
export interface MensajeAbandonar {
  tipo: "ABANDONAR";
  /** Equipo del jugador que abandona: 1 = arriba (rojo), 2 = abajo (azul) */
  equipo: 1 | 2;
}

export interface MensajePonerTrampa {
  tipo: "PONER_TRAMPA";
  equipo: 1 | 2;
  fila: number;
  columna: number;
}

export interface MensajeCartaAccion {
  tipo: "CARTA_ACCION";
  equipo: 1 | 2;
  carta: string;
}

export interface MensajeJugarCartaAccion {
  tipo: "JUGAR_CARTA_ACCION";
  equipo: 1 | 2;
  cartaAccion: string;
  x: number;
  y: number;
  x_op: number;
  y_op: number;
  cartaRobar: string;
}

// ── Respuestas del servidor ───────────────────────────────────────────────────

/** Datos de la partida enviados por el servidor al emparejar dos jugadores */
export interface RespuestaPartidaEncontrada {
  tipo: "PARTIDA_ENCONTRADA";
  partida_id: string;
  /** Equipo asignado a este cliente: 1 = arriba (rojo), 2 = abajo (azul) */
  equipo: 1 | 2;
  oponente: string;
  oponente_avatar_id?: string | null;
  oponentePt: number;
  cartas_jugador: string[];
  cartas_oponente: string[];
  /** Cola de 3 cartas en espera (índice 0 = la siguiente en ser usada) */
  carta_siguiente: string[];
}

/** El servidor autoriza al cliente a mover (se envía al jugador que empieza) */
export interface RespuestaTuTurno {
  tipo: "TU_TURNO";
}

/** El servidor retransmite el movimiento del oponente */
export interface RespuestaMover {
  tipo: "MOVER";
  col_origen: number;
  fila_origen: number;
  col_destino: number;
  fila_destino: number;
  carta: string;
}

/** El servidor notifica al ganador de la partida. */
export interface RespuestaVictoria {
  tipo: "VICTORIA";
}

/** El servidor notifica al perdedor de la partida. */
export interface RespuestaDerrota {
  tipo: "DERROTA";
}

/** Fin de partida con detalle (si el servidor envía TERMINAR_PARTIDA). */
export interface RespuestaTerminarPartida {
  tipo: "TERMINAR_PARTIDA";
  ganador: string;
  razon: string;
}

export interface RespuestaSeleccioneCartaAccion {
  tipo: "SELECCIONE_CARTA_ACCION";
  cartas_accion: { nombre: string; accion: string }[];
}

export interface RespuestaPartidaLista {
  tipo: "PARTIDA_LISTA";
  cartas_accion: { nombre: string; accion: string }[];
}

export interface RespuestaCartaAccionJugada {
  tipo: "CARTA_ACCION_JUGADA";
  carta_accion: string;
  /** Tipo de acción (p. ej. ESPEJO, CEGAR); necesario para aplicar el efecto en el cliente rival */
  accion?: string;
  x: number;
  y: number;
  x_op: number;
  y_op: number;
  carta_robar: string;
}

export interface RespuestaTrampaActivada {
  tipo: "TRAMPA_ACTIVADA";
  columna: number;
  fila: number;
}

/** Un peón murió por causa especial (p. ej. revivir en casilla trampa). */
export interface RespuestaPeonMuerto {
  tipo: "PEON_MUERTO";
  pos_x: number;
  pos_y: number;
}

export interface RespuestaTrampaInvalida {
  tipo: "TRAMPA_INVALIDA";
}

export interface RespuestaCartaAccionInvalida {
  tipo: "CARTA_ACCION_INVALIDA";
}

export type MensajeServidor =
  | RespuestaTuTurno
  | RespuestaMover
  | RespuestaPartidaEncontrada
  | RespuestaVictoria
  | RespuestaDerrota
  | RespuestaTerminarPartida
  | RespuestaSeleccioneCartaAccion
  | RespuestaPartidaLista
  | RespuestaCartaAccionJugada
  | RespuestaTrampaActivada
  | RespuestaPeonMuerto
  | RespuestaTrampaInvalida
  | RespuestaCartaAccionInvalida
  | { tipo: string; [key: string]: unknown };

// ─── Conexión a la partida ────────────────────────────────────────────────────

/**
 * Registra un listener sobre el WS compartido para recibir mensajes de partida.
 * Devuelve una función de cleanup para eliminar el listener al desmontar.
 *
 * En modo mock (sin servidor configurado) devuelve un no-op.
 */
export function conectarPartida(
  onMensaje: (msg: MensajeServidor) => void
): () => void {
  if (!usarServidor) return () => {};
  return WS.suscribirTodos((msg) => onMensaje(msg as MensajeServidor));
}

/**
 * Limpia el estado local de la partida.
 * El WS compartido NO se cierra aquí: permanece abierto hasta el cierre de sesión.
 */
export function desconectarPartida(): void {
  // El WS persiste; solo limpiamos datos locales de la partida
  sessionStorage.removeItem("datosPartida");
}

// ─── Helpers de envío ─────────────────────────────────────────────────────────

/** Envía un movimiento al servidor */
export function enviarMovimiento(datos: Omit<MensajeMover, "tipo">): boolean {
  return WS.enviar({ tipo: "MOVER", ...datos } as MensajeMover);
}

/** Notifica al servidor que el jugador abandona la partida voluntariamente */
export function enviarAbandonar(equipo: 1 | 2): boolean {
  return WS.enviar({ tipo: "ABANDONAR", equipo } satisfies MensajeAbandonar);
}

export function enviarPonerTrampa(equipo: 1 | 2, fila: number, columna: number): boolean {
  return WS.enviar({ tipo: "PONER_TRAMPA", equipo, fila, columna } satisfies MensajePonerTrampa);
}

export function enviarSeleccionCartaAccion(equipo: 1 | 2, carta: string): boolean {
  return WS.enviar({ tipo: "CARTA_ACCION", equipo, carta } satisfies MensajeCartaAccion);
}

export function enviarJugarCartaAccion(datos: Omit<MensajeJugarCartaAccion, "tipo">): boolean {
  return WS.enviar({ tipo: "JUGAR_CARTA_ACCION", ...datos } as MensajeJugarCartaAccion);
}

/**
 * Avisa al servidor que la pantalla de partida está lista.
 * El servidor actual no maneja este mensaje, se mantiene por compatibilidad futura.
 */
export function enviarEstoyListo(): boolean {
  return WS.enviar({ tipo: "ESTOY_LISTO" });
}

// ─── Compatibilidad hacia atrás ───────────────────────────────────────────────
// Estas funciones existían en la versión anterior donde el WS se gestionaba aquí.
// Ahora son no-ops o delegados a ws.ts.

/** @deprecated El WS ahora se gestiona en ws.ts. Esta función no tiene efecto. */
export function setWsActivo(_ws: WebSocket): void {
  // No-op: la conexión se mantiene en ws.ts
}

/** @deprecated El WS ahora se gestiona en ws.ts. */
export function getWsActivo(): WebSocket | null {
  return null;
}

// ─── Pausa / Reanudar (partidas privadas) ─────────────────────────────────────

/** Solicita pausar la partida al oponente. */
export function enviarSolicitarPausa(
  remitente: string,
  destinatario: string,
  idPartida: string | number
): boolean {
  return WS.enviar({ tipo: "SOLICITAR_PAUSA", remitente, destinatario, idPartida: Number(idPartida) });
}

/** Acepta la solicitud de pausa recibida. */
export function enviarAceptarPausa(idNotificacion: number, nombre: string): boolean {
  return WS.enviar({ tipo: "ACEPTAR_PAUSA", idNotificacion, nombre });
}

/** Rechaza la solicitud de pausa recibida. */
export function enviarRechazarPausa(idNotificacion: number, nombre: string): boolean {
  return WS.enviar({ tipo: "RECHAZAR_PAUSA", idNotificacion, nombre });
}

/** Solicita reanudar una partida pausada. */
export function enviarSolicitarReanudar(
  remitente: string,
  destinatario: string,
  idPartida: number
): boolean {
  return WS.enviar({ tipo: "SOLICITAR_REANUDAR", remitente, destinatario, idPartida });
}

/** Acepta la solicitud de reanudar una partida. */
export function enviarAceptarReanudar(idNotificacion: number, nombre: string): boolean {
  return WS.enviar({ tipo: "ACEPTAR_REANUDAR", idNotificacion, nombre });
}

/** Rechaza la solicitud de reanudar una partida. */
export function enviarRechazarReanudar(idNotificacion: number, nombre: string): boolean {
  return WS.enviar({ tipo: "RECHAZAR_REANUDAR", idNotificacion, nombre });
}
