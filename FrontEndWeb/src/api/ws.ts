/**
 * ws.ts – Gestor de conexión WebSocket persistente.
 *
 * Mantiene una única conexión WebSocket por sesión de navegador (singleton
 * por pestaña). Se abre al hacer login / registro y se cierra al cerrar sesión
 * o al abandonar la aplicación.
 *
 * Todos los módulos (auth, buscarpartida, partida, social…) usan este gestor
 * en lugar de abrir sus propias conexiones, de modo que el servidor ve
 * siempre un único WebSocket por usuario.
 *
 * API pública:
 *   conectar()            → abre la conexión (idempotente), devuelve Promise<void>
 *   desconectar()         → cierra la conexión (llamar al cerrar sesión)
 *   enviar(msg)           → serializa y envía un objeto JSON; devuelve boolean
 *   suscribir(tipo, fn)   → escucha mensajes de un tipo concreto; devuelve unsub
 *   suscribirTodos(fn)    → escucha todos los mensajes; devuelve unsub
 *   estaConectado()       → true si el WS está en estado OPEN
 */

export type MensajeWS = Record<string, unknown> & { tipo: string };
type Listener = (msg: MensajeWS) => void;

const WS_URL = process.env.NEXT_PUBLIC_WS_URL ?? "";
export const usarServidor = !!WS_URL;

let ws: WebSocket | null = null;

/** Suscriptores por tipo de mensaje. */
const suscripciones = new Map<string, Set<Listener>>();
/** Suscriptores que reciben todos los mensajes, independientemente del tipo. */
const suscripcionesGlobales = new Set<Listener>();

function despachar(msg: MensajeWS): void {
  suscripciones.get(msg.tipo)?.forEach((fn) => {
    try {
      fn(msg);
    } catch (e) {
      console.error("[ws] Error en listener tipado:", msg.tipo, e);
    }
  });
  suscripcionesGlobales.forEach((fn) => {
    try {
      fn(msg);
    } catch (e) {
      console.error("[ws] Error en listener global:", msg.tipo, e);
    }
  });
}

/** Registra los manejadores de mensajes/errores/cierre sobre el WS activo. */
function adjuntarHandlers(): void {
  if (!ws) return;
  ws.onmessage = (ev) => {
    try {
      despachar(JSON.parse(ev.data as string) as MensajeWS);
    } catch {
      console.error("[ws] Mensaje inválido:", ev.data);
    }
  };
  ws.onerror = () => console.error("[ws] Error en la conexión.");
  ws.onclose = () => {
    ws = null;
  };
}

/** Devuelve true si la conexión WebSocket está activa y lista. */
export function estaConectado(): boolean {
  return ws?.readyState === WebSocket.OPEN;
}

/**
 * Abre la conexión WebSocket si no está ya abierta.
 * - Si ya está OPEN, resuelve inmediatamente.
 * - Si está CONNECTING, espera a que termine.
 * - Si no hay conexión, abre una nueva.
 */
export function conectar(): Promise<void> {
  if (!usarServidor) return Promise.resolve();
  if (ws?.readyState === WebSocket.OPEN) return Promise.resolve();

  if (ws?.readyState === WebSocket.CONNECTING) {
    return new Promise((resolve, reject) => {
      const ref = ws!;
      const onOpen = () => {
        ref.removeEventListener("open", onOpen);
        ref.removeEventListener("error", onErr);
        resolve();
      };
      const onErr = () => {
        ref.removeEventListener("open", onOpen);
        ref.removeEventListener("error", onErr);
        reject(new Error("No se pudo conectar al servidor."));
      };
      ref.addEventListener("open", onOpen);
      ref.addEventListener("error", onErr);
    });
  }

  return new Promise((resolve, reject) => {
    try {
      ws = new WebSocket(WS_URL);
      ws.onopen = () => {
        adjuntarHandlers();
        resolve();
      };
      ws.onerror = () => {
        ws = null;
        reject(new Error("No se pudo conectar al servidor."));
      };
    } catch {
      reject(new Error("URL del servidor no válida."));
    }
  });
}

/** Cierra la conexión WebSocket. Llamar al cerrar sesión o salir de la app. */
export function desconectar(): void {
  ws?.close();
  ws = null;
}

/** Envía un mensaje JSON al servidor. Devuelve false si no hay conexión abierta. */
export function enviar(msg: object): boolean {
  if (!ws || ws.readyState !== WebSocket.OPEN) return false;
  try {
    ws.send(JSON.stringify(msg));
    return true;
  } catch {
    return false;
  }
}

/**
 * Suscribe un listener a mensajes de un tipo concreto.
 * Devuelve una función que cancela la suscripción al ser invocada.
 */
export function suscribir(tipo: string, fn: Listener): () => void {
  if (!suscripciones.has(tipo)) suscripciones.set(tipo, new Set());
  suscripciones.get(tipo)!.add(fn);
  return () => suscripciones.get(tipo)?.delete(fn);
}

/**
 * Suscribe un listener a TODOS los mensajes del servidor.
 * Devuelve una función que cancela la suscripción al ser invocada.
 */
export function suscribirTodos(fn: Listener): () => void {
  suscripcionesGlobales.add(fn);
  return () => suscripcionesGlobales.delete(fn);
}
