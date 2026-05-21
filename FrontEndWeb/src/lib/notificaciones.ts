/**
 * notificaciones.ts – Almacenamiento persistente de notificaciones pendientes.
 *
 * Guarda las notificaciones en sessionStorage para que sobrevivan a
 * navegaciones dentro de la misma sesión (ej. /partidas → /buscar → /partidas).
 *
 * El listener de WS (inicializado en auth.ts tras el login) llama a
 * guardarNotificacion() cuando el servidor envía SOLICITUD_AMISTAD o
 * INVITACION_PARTIDA, independientemente de en qué pantalla esté el usuario.
 */

export interface Notificacion {
  idNotificacion: number;
  tipo: "SOLICITUD_AMISTAD" | "INVITACION_PARTIDA";
  remitente: string;
  avatar_id?: string | null;
  fecha_ini?: string;
  fecha_fin?: string;
}

const CLAVE = "onitama_notificaciones";

function deduplicar(lista: Notificacion[]): Notificacion[] {
  const salida: Notificacion[] = [];
  const remitentesAmistad = new Set<string>();
  const ids = new Set<number>();

  for (const n of lista) {
    if (ids.has(n.idNotificacion)) continue;
    ids.add(n.idNotificacion);

    if (n.tipo === "SOLICITUD_AMISTAD") {
      if (remitentesAmistad.has(n.remitente)) continue;
      remitentesAmistad.add(n.remitente);
    }

    salida.push(n);
  }

  return salida;
}

function leer(): Notificacion[] {
  if (typeof window === "undefined") return [];
  try {
    const lista = JSON.parse(sessionStorage.getItem(CLAVE) ?? "[]") as Notificacion[];
    const limpia = deduplicar(lista);
    if (limpia.length !== lista.length) {
      sessionStorage.setItem(CLAVE, JSON.stringify(limpia));
    }
    return limpia;
  } catch {
    return [];
  }
}

function escribir(lista: Notificacion[]): void {
  if (typeof window !== "undefined") {
    sessionStorage.setItem(CLAVE, JSON.stringify(lista));
  }
}

/** Devuelve la lista actual de notificaciones pendientes. */
export function leerNotificaciones(): Notificacion[] {
  return leer();
}

/**
 * Añade una notificación al almacén si no existe ya (por idNotificacion).
 * Llamado desde el listener WS en auth.ts.
 */
export function guardarNotificacion(n: Notificacion): void {
  const lista = leer();
  if (lista.find((x) => x.idNotificacion === n.idNotificacion)) return;
  if (n.tipo === "SOLICITUD_AMISTAD" && lista.find((x) => x.tipo === "SOLICITUD_AMISTAD" && x.remitente === n.remitente)) return;
  escribir(deduplicar([...lista, n]));
}

/** Elimina una notificación por id (tras aceptar, rechazar o expirar). */
export function eliminarNotificacion(id: number): void {
  escribir(leer().filter((n) => n.idNotificacion !== id));
}

/** Elimina todas las solicitudes de amistad pendientes de un remitente concreto. */
export function eliminarSolicitudesAmistadPorRemitente(remitente: string): void {
  escribir(
    leer().filter(
      (n) => !(n.tipo === "SOLICITUD_AMISTAD" && n.remitente === remitente)
    )
  );
}

/** Limpia todas las notificaciones (al cerrar sesión). */
export function limpiarNotificaciones(): void {
  if (typeof window !== "undefined") {
    sessionStorage.removeItem(CLAVE);
  }
}
