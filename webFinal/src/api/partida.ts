/**
 * Cliente WebSocket para la partida en curso.
 *
 * Mensajes cliente → servidor:
 *   ESTOY_LISTO  – la pantalla de partida está cargada y lista
 *   MOVER        – el jugador mueve una ficha
 *   ABANDONAR    – el jugador abandona voluntariamente la partida
 *
 * Mensajes servidor → cliente:
 *   TU_TURNO         – el servidor autoriza al cliente a mover (primer turno)
 *   MOVER            – el oponente ha movido; se retransmite al otro cliente
 *   TERMINAR_PARTIDA – la partida ha terminado (tiempo, victoria, abandono)
 *
 * NOTA sobre nomenclatura: el diagrama de secuencia del backend usa
 * QUIERO_JUGAR / EMPIEZA_PARTIDA, pero el servidor (Servidor.java) implementa
 * BUSCAR_PARTIDA / PARTIDA_ENCONTRADA. Se usan estos últimos para coherencia
 * con el código del servidor existente.
 */

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "";

/** true cuando hay una URL de servidor configurada en .env.local */
export const usarServidor = !!WS_URL;

// ─── Tipos de mensajes ────────────────────────────────────────────────────────

/** El cliente indica al servidor que la pantalla de partida está lista */
export interface MensajeEstoyListo {
  tipo: "ESTOY_LISTO";
}

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

/** El servidor declara el fin de la partida (versión futura con TERMINAR_PARTIDA) */
export interface RespuestaTerminarPartida {
  tipo: "TERMINAR_PARTIDA";
  /** Nombre de usuario del ganador */
  ganador: string;
  /** Razón: TIEMPO_AGOTADO | REY_CAPTURADO | TRONO | ABANDONO */
  razon: string;
}

/**
 * El servidor notifica al ganador de la partida.
 * El servidor actual (Ciro) envía VICTORIA al ganador y DERROTA al perdedor
 * en lugar de TERMINAR_PARTIDA. Ambos mensajes son manejados en partida/page.tsx.
 */
export interface RespuestaVictoria {
  tipo: "VICTORIA";
}

/** El servidor notifica al perdedor de la partida. */
export interface RespuestaDerrota {
  tipo: "DERROTA";
}

export type MensajeServidor =
  | RespuestaTuTurno
  | RespuestaMover
  | RespuestaPartidaEncontrada
  | RespuestaTerminarPartida
  | RespuestaVictoria
  | RespuestaDerrota
  | { tipo: string; [key: string]: unknown };

// ─── Conexión compartida (singleton por pestaña) ──────────────────────────────

/**
 * WebSocket activo. Se comparte entre buscarpartida.ts (lo crea y alimenta
 * con PARTIDA_ENCONTRADA) y partida/page.tsx (lo usa para MOVER / ESTOY_LISTO).
 * Persiste durante toda la navegación dentro de la misma pestaña.
 */
let wsActivo: WebSocket | null = null;
/** Para evitar enviar ESTOY_LISTO más de una vez por conexión (p. ej. React StrictMode) */
let wsEstoyListoEnviado: WebSocket | null = null;

export function getWsActivo(): WebSocket | null {
  return wsActivo;
}

/**
 * Permite a buscarpartida.ts inyectar el WebSocket ya abierto para que
 * la pantalla de partida lo reutilice sin abrir una segunda conexión.
 */
export function setWsActivo(ws: WebSocket): void {
  wsActivo = ws;
  wsEstoyListoEnviado = null; // Nueva conexión: permitir enviar ESTOY_LISTO al entrar a partida
  wsActivo.onclose = () => {
    wsActivo = null;
    wsEstoyListoEnviado = null;
  };
}

// ─── Escucha de mensajes ──────────────────────────────────────────────────────

/**
 * Registra un listener de mensajes sobre el WebSocket activo.
 * Devuelve una función de cleanup para eliminar el listener al desmontar.
 *
 * Si no hay WebSocket activo, abre uno nuevo como fallback.
 * En modo mock (sin servidor configurado) no hace nada.
 */
export function conectarPartida(
  onMensaje: (msg: MensajeServidor) => void
): () => void {
  if (!usarServidor) return () => {};

  const listener = (ev: MessageEvent) => {
    try {
      onMensaje(JSON.parse(ev.data as string) as MensajeServidor);
    } catch {
      console.error("[partida] Mensaje no válido:", ev.data);
    }
  };

  // Reusar el WebSocket existente (inyectado desde buscarpartida.ts)
  if (wsActivo && wsActivo.readyState === WebSocket.OPEN) {
    wsActivo.addEventListener("message", listener);
    return () => wsActivo?.removeEventListener("message", listener);
  }

  // Fallback: abrir conexión nueva (si por alguna razón no existe)
  try {
    wsActivo = new WebSocket(WS_URL);
    wsActivo.addEventListener("message", listener);
    wsActivo.onerror = () => console.error("[partida] Error en el WebSocket.");
    wsActivo.onclose = () => {
      wsActivo = null;
    };
    return () => wsActivo?.removeEventListener("message", listener);
  } catch {
    console.error("[partida] No se pudo abrir WebSocket.");
    return () => {};
  }
}

export function desconectarPartida(): void {
  if (wsActivo) {
    wsActivo.close();
    wsActivo = null;
    wsEstoyListoEnviado = null;
  }
}

// ─── Helpers de envío ─────────────────────────────────────────────────────────

function enviar(msg: object): boolean {
  if (!wsActivo || wsActivo.readyState !== WebSocket.OPEN) return false;
  try {
    wsActivo.send(JSON.stringify(msg));
    return true;
  } catch {
    return false;
  }
}

/** Avisa al servidor que el cliente tiene la pantalla de partida lista (solo una vez por conexión) */
export function enviarEstoyListo(): boolean {
  if (!wsActivo || wsActivo.readyState !== WebSocket.OPEN) return false;
  if (wsEstoyListoEnviado === wsActivo) return true; // Ya enviado para esta conexión
  const ok = enviar({ tipo: "ESTOY_LISTO" } satisfies MensajeEstoyListo);
  if (ok) wsEstoyListoEnviado = wsActivo;
  return ok;
}

/** Envía un movimiento al servidor (sin partida_id, el servidor lo identifica por conexión) */
export function enviarMovimiento(
  datos: Omit<MensajeMover, "tipo">
): boolean {
  return enviar({ tipo: "MOVER", ...datos } as MensajeMover);
}

/** Notifica al servidor que el jugador abandona la partida voluntariamente */
export function enviarAbandonar(equipo: 1 | 2): boolean {
  return enviar({ tipo: "ABANDONAR", equipo } satisfies MensajeAbandonar);
}
