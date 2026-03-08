/**
 * Cliente WebSocket para la partida en curso.
 * Gestiona todos los mensajes de juego según el contrato acordado
 * con el servidor Java (ver LEEME.md y mensaje del backend dev).
 *
 * Tipos de mensaje cliente → servidor: BUSCAR_PARTIDA, MOVER, ACCION
 * Tipos de mensaje servidor → cliente: PARTIDA_ENCONTRADA, MOVER, SOLICITUD_AMISTAD, etc.
 *
 * Estado actual: el servidor (Servidor.java) aún está en desarrollo.
 * Se incluyen mocks para poder avanzar sin él.
 *
 * TODO (servidor): cuando el servidor gestione MOVER, eliminar el mock
 * y descomentar la lógica real de envío/recepción.
 */

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "";
const usarServidor = !!WS_URL;

// ─── Tipos de mensajes ────────────────────────────────────────────────────────

/** Mensaje que envía el cliente al servidor para registrar un movimiento */
export interface MensajeMover {
  tipo: "MOVER";
  /** ID de la partida (necesario para identificar la sesión si hay varias conexiones) */
  partida_id: string;
  /** Columna de origen (0-6) */
  col_origen: number;
  /** Fila de origen (0-6) */
  fila_origen: number;
  /** Columna de destino (0-6) */
  col_destino: number;
  /** Fila de destino (0-6) */
  fila_destino: number;
  /** Nombre de la carta de movimiento usada */
  carta: string;
}

/** Mensaje que recibe el cliente cuando el oponente mueve */
export interface RespuestaMover {
  tipo: "MOVER";
  col_origen: number;
  fila_origen: number;
  col_destino: number;
  fila_destino: number;
  carta: string;
}

/** Mensaje que recibe el cliente cuando el servidor ha encontrado partida */
export interface RespuestaPartidaEncontrada {
  tipo: "PARTIDA_ENCONTRADA";
  partida_id: string;
  /** Número de equipo asignado al cliente: 1 (arriba) o 2 (abajo) */
  equipo: 1 | 2;
  /** Nombre de usuario del oponente */
  oponente: string;
  /** Cartas iniciales para el equipo local (2 cartas) */
  cartas_jugador?: string[];
  /** Cartas iniciales para el oponente (2 cartas) */
  cartas_oponente?: string[];
  /** Carta siguiente en la pila */
  carta_siguiente?: string;
}

/** Unión de todos los mensajes posibles del servidor */
export type MensajeServidor =
  | RespuestaMover
  | RespuestaPartidaEncontrada
  | { tipo: string; [key: string]: unknown };

// ─── Conexión compartida (singleton por sesión) ───────────────────────────────

/**
 * Referencia a la conexión WebSocket activa.
 * El backend dev propone mantener una única conexión desde el inicio de sesión.
 * Por ahora la abrimos al entrar en la partida y la cerramos al salir.
 *
 * TODO: mover la apertura del WS al inicio de sesión según la propuesta del backend.
 */
let wsActivo: WebSocket | null = null;

export function getWsActivo(): WebSocket | null {
  return wsActivo;
}

// ─── Funciones principales ────────────────────────────────────────────────────

/**
 * Abre una nueva conexión WebSocket para la partida indicada
 * y registra el callback para mensajes entrantes.
 *
 * @param onMensaje  Función llamada cada vez que llega un mensaje del servidor
 * @returns  Función para cerrar la conexión (llamar al desmontar el componente)
 */
export function conectarPartida(
  onMensaje: (msg: MensajeServidor) => void
): () => void {
  if (!usarServidor) {
    // Sin servidor: la lógica de juego es totalmente local (ver page.tsx)
    return () => {};
  }

  if (wsActivo && wsActivo.readyState === WebSocket.OPEN) {
    // Reusar conexión existente, simplemente actualizar el listener
    wsActivo.onmessage = (ev) => {
      try {
        onMensaje(JSON.parse(ev.data as string) as MensajeServidor);
      } catch {
        console.error("[partida] Mensaje no válido:", ev.data);
      }
    };
    return () => desconectarPartida();
  }

  try {
    wsActivo = new WebSocket(WS_URL);
  } catch {
    console.error("[partida] No se pudo abrir WebSocket.");
    return () => {};
  }

  wsActivo.onmessage = (ev) => {
    try {
      onMensaje(JSON.parse(ev.data as string) as MensajeServidor);
    } catch {
      console.error("[partida] Mensaje no válido:", ev.data);
    }
  };

  wsActivo.onerror = () => console.error("[partida] Error en el WebSocket.");
  wsActivo.onclose = () => { wsActivo = null; };

  return () => desconectarPartida();
}

/** Cierra la conexión WebSocket activa */
export function desconectarPartida(): void {
  if (wsActivo) {
    wsActivo.close();
    wsActivo = null;
  }
}

/**
 * Envía un movimiento al servidor.
 * Solo se usa cuando el servidor está configurado (NEXT_PUBLIC_WS_URL).
 *
 * Según el backend dev: solo pasar la carta jugada, posición origen y destino.
 * El servidor valida que el movimiento es legal y lo aplica en la base de datos.
 */
export function enviarMovimiento(mensaje: MensajeMover): boolean {
  if (!usarServidor || !wsActivo || wsActivo.readyState !== WebSocket.OPEN) {
    return false;
  }
  try {
    wsActivo.send(JSON.stringify(mensaje));
    return true;
  } catch {
    return false;
  }
}
