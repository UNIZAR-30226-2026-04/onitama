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

// ── Respuestas del servidor ───────────────────────────────────────────────────

/** Datos de la partida enviados por el servidor al emparejar dos jugadores */
export interface RespuestaPartidaEncontrada {
  tipo: "PARTIDA_ENCONTRADA";
  partida_id: string;
  /** Equipo asignado a este cliente: 1 = arriba (rojo), 2 = abajo (azul) */
  equipo: 1 | 2;
  oponente: string;
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

export type MensajeServidor =
  | RespuestaTuTurno
  | RespuestaMover
  | RespuestaPartidaEncontrada
  | RespuestaVictoria
  | RespuestaDerrota
  | RespuestaTerminarPartida
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
