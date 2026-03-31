/**
 * Gestión de la sesión del jugador en el navegador.
 *
 * Tras un inicio de sesión o registro exitoso, el servidor devuelve los datos
 * del jugador. Esta utilidad los guarda en sessionStorage para que el resto
 * de la aplicación (buscar partida, pantalla de juego, etc.) pueda leerlos
 * sin necesidad de volver a consultar al servidor.
 *
 * Se usa sessionStorage (no localStorage) para que los datos se borren
 * automáticamente al cerrar el navegador o la pestaña.
 */

const CLAVE_SESION = "sesionJugador";

export interface DatosSesion {
  nombre: string;
  correo: string;
  puntos: number;
  partidas_ganadas: number;
  partidas_jugadas: number;
  cores: number;
  skin_activa: string;
  avatar_id: string | null;
}

/** Guarda los datos del jugador en sessionStorage tras el login. */
export function guardarSesion(datos: DatosSesion): void {
  if (typeof window !== "undefined") {
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(datos));
  }
}

/** Lee los datos del jugador desde sessionStorage. Devuelve null si no hay sesión activa. */
export function leerSesion(): DatosSesion | null {
  if (typeof window === "undefined") return null;
  const raw = sessionStorage.getItem(CLAVE_SESION);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as DatosSesion;
  } catch {
    return null;
  }
}

/** Elimina la sesión (cierre de sesión). */
export function cerrarSesion(): void {
  if (typeof window !== "undefined") {
    sessionStorage.removeItem(CLAVE_SESION);
  }
}

/**
 * Devuelve los datos del jugador activo, o un mock si no hay sesión.
 * Útil para pantallas que necesitan el nombre/puntos sin redirigir al login.
 */
export function obtenerJugadorActivo(): DatosSesion {
  return (
    leerSesion() ?? {
      nombre: "IronMaster",
      correo: "jugador@onitama.com",
      puntos: 1372,
      partidas_ganadas: 5,
      partidas_jugadas: 10,
      cores: 430,
      skin_activa: "Skin0",
      avatar_id: null,
    }
  );
}
