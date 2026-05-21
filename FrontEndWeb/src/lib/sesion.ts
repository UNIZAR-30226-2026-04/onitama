/**
 * Gestión de la sesión del jugador en el navegador.
 *
 * Tras un inicio de sesión o registro exitoso, el servidor devuelve los datos
 * del jugador. Esta utilidad los guarda en localStorage para que el resto
 * de la aplicación (buscar partida, pantalla de juego, etc.) pueda leerlos
 * también desde pestañas nuevas del mismo navegador.
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

/** Guarda los datos del jugador en localStorage tras el login. */
export function guardarSesion(datos: DatosSesion): void {
  if (typeof window !== "undefined") {
    localStorage.setItem(CLAVE_SESION, JSON.stringify(datos));
    sessionStorage.removeItem(CLAVE_SESION);
  }
}

/** Lee los datos del jugador desde localStorage. Devuelve null si no hay sesión activa. */
export function leerSesion(): DatosSesion | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(CLAVE_SESION) ?? sessionStorage.getItem(CLAVE_SESION);
  if (!raw) return null;
  try {
    const datos = JSON.parse(raw) as DatosSesion;
    localStorage.setItem(CLAVE_SESION, JSON.stringify(datos));
    sessionStorage.removeItem(CLAVE_SESION);
    return datos;
  } catch {
    localStorage.removeItem(CLAVE_SESION);
    sessionStorage.removeItem(CLAVE_SESION);
    return null;
  }
}

/** Elimina la sesión (cierre de sesión). */
export function cerrarSesion(): void {
  if (typeof window !== "undefined") {
    localStorage.removeItem(CLAVE_SESION);
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
