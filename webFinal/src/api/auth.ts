/**
 * Cliente de autenticación – Onitama Web Frontend.
 *
 * Toda la comunicación con el servidor se hace por WebSocket (igual que la partida).
 *
 * Mensajes que envía el cliente:
 *   INICIAR_SESION  { tipo, nombre, password }
 *   REGISTRARSE     { tipo, correo, nombre, password }
 *
 * Respuestas del servidor:
 *   INICIO_SESION_EXITOSO  { tipo, nombre, correo, puntos, partidas_ganadas, partidas_jugadas, cores }
 *   ERROR_SESION_USS       { tipo }  ← usuario no encontrado
 *   ERROR_SESION_PSSWD     { tipo }  ← contraseña incorrecta
 *   REGISTRO_EXITOSO       { tipo }
 *   REGISTRO_ERRONEO       { tipo }
 *
 * Si NEXT_PUBLIC_WS_URL no está configurado, se usa lógica mock para desarrollo.
 *
 * NOTA: cada llamada de auth abre una conexión WS temporal solo para ese mensaje.
 * No se reutiliza con la conexión de partida (que se abre después en buscarpartida.ts).
 */

import { DatosSesion } from "@/lib/sesion";

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || "";

/** true cuando hay URL de servidor configurada */
export const usarServidor = !!WS_URL;

// ─── Datos mock para desarrollo sin servidor ──────────────────────────────────

const MOCK_USUARIOS: Record<string, DatosSesion & { password: string }> = {
  IronMaster: {
    password: "password123",
    nombre: "IronMaster",
    correo: "jugador@onitama.com",
    puntos: 1372,
    partidas_ganadas: 5,
    partidas_jugadas: 10,
    cores: 430,
  },
};

// ─── Helper: abrir WebSocket y esperar a que esté listo ───────────────────────

function abrirWS(): Promise<WebSocket> {
  return new Promise((resolve, reject) => {
    try {
      const ws = new WebSocket(WS_URL);
      ws.onopen = () => resolve(ws);
      ws.onerror = () => reject(new Error("No se pudo conectar al servidor."));
      ws.onclose = (e) => {
        if (!e.wasClean) reject(new Error("Conexión cerrada inesperadamente."));
      };
    } catch {
      reject(new Error("URL del servidor no válida."));
    }
  });
}

// ─── Inicio de sesión ─────────────────────────────────────────────────────────

/**
 * Inicia sesión con nombre de usuario y contraseña.
 * Devuelve los datos del jugador si tiene éxito; lanza Error en caso contrario.
 *
 * Mensaje enviado:
 *   { tipo: "INICIAR_SESION", nombre: string, password: string }
 *
 * Respuestas esperadas:
 *   INICIO_SESION_EXITOSO → OK
 *   ERROR_SESION_USS      → usuario no encontrado
 *   ERROR_SESION_PSSWD    → contraseña incorrecta
 */
export async function iniciarSesion(
  nombre: string,
  password: string
): Promise<DatosSesion> {
  // ── Mock ──────────────────────────────────────────────────────────────────
  if (!usarServidor) {
    const u = MOCK_USUARIOS[nombre];
    if (!u) throw new Error("Usuario no encontrado.");
    if (u.password !== password) throw new Error("Contraseña incorrecta.");
    const { password: _pw, ...datos } = u;
    void _pw;
    return datos;
  }

  // ── Servidor ──────────────────────────────────────────────────────────────
  const ws = await abrirWS();

  return new Promise<DatosSesion>((resolve, reject) => {
    const timeout = setTimeout(() => {
      ws.close();
      reject(new Error("El servidor no respondió a tiempo."));
    }, 10_000);

    ws.onmessage = (ev) => {
      clearTimeout(timeout);
      ws.close();
      try {
        const resp = JSON.parse(ev.data as string) as { tipo: string } & Record<string, unknown>;
        if (resp.tipo === "INICIO_SESION_EXITOSO") {
          resolve(resp as unknown as DatosSesion);
        } else if (resp.tipo === "ERROR_SESION_USS") {
          reject(new Error("Usuario no encontrado."));
        } else if (resp.tipo === "ERROR_SESION_PSSWD") {
          reject(new Error("Contraseña incorrecta."));
        } else {
          reject(new Error("Respuesta inesperada del servidor."));
        }
      } catch {
        reject(new Error("Respuesta inválida del servidor."));
      }
    };

    ws.onerror = () => {
      clearTimeout(timeout);
      reject(new Error("Error de conexión con el servidor."));
    };

    ws.send(JSON.stringify({ tipo: "INICIAR_SESION", nombre, password }));
  });
}

// ─── Registro ────────────────────────────────────────────────────────────────

/**
 * Registra un nuevo usuario.
 * Lanza Error si el registro falla (usuario o correo ya existe, etc.).
 *
 * Mensaje enviado:
 *   { tipo: "REGISTRARSE", correo: string, nombre: string, password: string }
 *
 * Respuestas esperadas:
 *   REGISTRO_EXITOSO  → OK
 *   REGISTRO_ERRONEO  → error (usuario/correo ya existe)
 */
export async function registrarUsuario(
  correo: string,
  nombre: string,
  password: string
): Promise<void> {
  // ── Mock ──────────────────────────────────────────────────────────────────
  if (!usarServidor) {
    // Simulamos éxito siempre en modo desarrollo
    return;
  }

  // ── Servidor ──────────────────────────────────────────────────────────────
  const ws = await abrirWS();

  return new Promise<void>((resolve, reject) => {
    const timeout = setTimeout(() => {
      ws.close();
      reject(new Error("El servidor no respondió a tiempo."));
    }, 10_000);

    ws.onmessage = (ev) => {
      clearTimeout(timeout);
      ws.close();
      try {
        const resp = JSON.parse(ev.data as string) as { tipo: string };
        if (resp.tipo === "REGISTRO_EXITOSO") {
          resolve();
        } else if (resp.tipo === "REGISTRO_ERRONEO") {
          reject(new Error("No se pudo registrar. El usuario o correo ya podría existir."));
        } else {
          reject(new Error("Respuesta inesperada del servidor."));
        }
      } catch {
        reject(new Error("Respuesta inválida del servidor."));
      }
    };

    ws.onerror = () => {
      clearTimeout(timeout);
      reject(new Error("Error de conexión con el servidor."));
    };

    ws.send(JSON.stringify({ tipo: "REGISTRARSE", correo, nombre, password }));
  });
}
