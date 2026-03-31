/**
 * auth.ts – Autenticación y perfil del jugador.
 *
 * Usa el gestor WS compartido (ws.ts) en lugar de abrir conexiones temporales,
 * de modo que el WebSocket permanece abierto desde el login / registro hasta
 * que el usuario cierra sesión o abandona la aplicación.
 *
 * Esto permite recibir notificaciones pendientes (solicitudes de amistad,
 * invitaciones a partida…) que el servidor vuelca justo después del login.
 *
 * Mensajes enviados:
 *   INICIAR_SESION  { tipo, nombre, password }
 *   REGISTRARSE     { tipo, correo, nombre, password }
 *   OBTENER_PERFIL  { tipo, nombre }
 *
 * Respuestas del servidor:
 *   INICIO_SESION_EXITOSO  { tipo, nombre, correo, puntos, partidas_ganadas, partidas_jugadas, cores, skin_activa }
 *   ERROR_SESION_USS       { tipo }  ← usuario no encontrado
 *   ERROR_SESION_PSSWD     { tipo }  ← contraseña incorrecta
 *   REGISTRO_EXITOSO       { tipo }
 *   REGISTRO_ERRONEO       { tipo }
 *   PERFIL_ACTUALIZADO     { tipo, nombre, correo, puntos, … }
 */

import { DatosSesion, obtenerJugadorActivo } from "@/lib/sesion";
import { guardarNotificacion } from "@/lib/notificaciones";
import * as WS from "./ws";

export const usarServidor = WS.usarServidor;

// Evita peticiones duplicadas de OBTENER_PERFIL (p. ej. StrictMode en desarrollo).
let perfilEnCurso: { nombre: string; promise: Promise<DatosSesion> } | null = null;

// ─── Listener de notificaciones ───────────────────────────────────────────────

/**
 * Registra suscripciones WS para capturar notificaciones (SOLICITUD_AMISTAD,
 * INVITACION_PARTIDA) y guardarlas en sessionStorage.
 *
 * Se llama una vez tras un login o registro exitoso. Las notificaciones llegan
 * justo después del INICIO_SESION_EXITOSO (el servidor las vuelca en ese momento)
 * y también pueden llegar en cualquier momento mientras el WS esté abierto.
 * Al guardarlas en sessionStorage sobreviven a la navegación interna de la app.
 */
function configurarListenerNotificaciones(): void {
  WS.suscribir("SOLICITUD_AMISTAD", (msg) => {
    guardarNotificacion({
      idNotificacion: msg.idNotificacion as number,
      tipo: "SOLICITUD_AMISTAD",
      remitente: msg.remitente as string,
      fecha_ini: msg.fecha_ini as string | undefined,
      fecha_fin: msg.fecha_fin as string | undefined,
    });
  });

  WS.suscribir("INVITACION_PARTIDA", (msg) => {
    guardarNotificacion({
      idNotificacion: msg.idNotificacion as number,
      tipo: "INVITACION_PARTIDA",
      remitente: msg.remitente as string,
    });
  });
}

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
    skin_activa: "Skin0",
  },
};

// ─── Inicio de sesión ─────────────────────────────────────────────────────────

/**
 * Inicia sesión con nombre de usuario y contraseña.
 * Abre (o reutiliza) la conexión WS compartida y la mantiene abierta
 * para que el servidor pueda enviar las notificaciones pendientes y
 * para los mensajes del resto de la aplicación.
 *
 * Lanza Error si las credenciales son incorrectas o hay fallo de red.
 */
export async function iniciarSesion(
  nombre: string,
  password: string
): Promise<DatosSesion> {
  if (!usarServidor) {
    const u = MOCK_USUARIOS[nombre];
    if (!u) throw new Error("Usuario no encontrado.");
    if (u.password !== password) throw new Error("Contraseña incorrecta.");
    const { password: _pw, ...datos } = u;
    void _pw;
    return datos;
  }

  await WS.conectar();

  return new Promise<DatosSesion>((resolve, reject) => {
    const timeout = setTimeout(() => {
      unsub();
      WS.desconectar();
      reject(new Error("El servidor no respondió a tiempo."));
    }, 10_000);

    const TIPOS_RESPUESTA = [
      "INICIO_SESION_EXITOSO",
      "ERROR_SESION_USS",
      "ERROR_SESION_PSSWD",
      "ERROR_BD",
    ];

    const unsub = WS.suscribirTodos((msg) => {
      if (!TIPOS_RESPUESTA.includes(msg.tipo)) return;

      clearTimeout(timeout);
      unsub();

      if (msg.tipo === "INICIO_SESION_EXITOSO") {
        // Configurar listener antes de resolver para capturar notificaciones
        // que el servidor envía inmediatamente después de INICIO_SESION_EXITOSO
        configurarListenerNotificaciones();
        resolve(msg as unknown as DatosSesion);
      } else {
        WS.desconectar();
        if (msg.tipo === "ERROR_SESION_USS") {
          reject(new Error("Usuario no encontrado."));
        } else if (msg.tipo === "ERROR_SESION_PSSWD") {
          reject(new Error("Contraseña incorrecta."));
        } else {
          reject(new Error("Error del servidor."));
        }
      }
    });

    WS.enviar({ tipo: "INICIAR_SESION", nombre, password });
  });
}

// ─── Registro ────────────────────────────────────────────────────────────────

/**
 * Registra un nuevo usuario y devuelve los datos de sesión iniciales.
 * El WS permanece abierto tras el registro, igual que tras el login.
 * Los datos de sesión se construyen con los valores del formulario (puntos y
 * cores comienzan en 0 para una cuenta nueva).
 *
 * Lanza Error si el registro falla (usuario o correo ya existe, etc.).
 */
export async function registrarUsuario(
  correo: string,
  nombre: string,
  password: string
): Promise<DatosSesion> {
  if (!usarServidor) {
    return {
      nombre,
      correo,
      puntos: 0,
      partidas_ganadas: 0,
      partidas_jugadas: 0,
      cores: 0,
      skin_activa: "Skin0",
    };
  }

  await WS.conectar();

  return new Promise<DatosSesion>((resolve, reject) => {
    const timeout = setTimeout(() => {
      unsub();
      WS.desconectar();
      reject(new Error("El servidor no respondió a tiempo."));
    }, 10_000);

    const unsub = WS.suscribirTodos((msg) => {
      if (msg.tipo !== "REGISTRO_EXITOSO" && msg.tipo !== "REGISTRO_ERRONEO") return;

      clearTimeout(timeout);
      unsub();

      if (msg.tipo === "REGISTRO_EXITOSO") {
        // Configurar listener de notificaciones igual que en el login
        configurarListenerNotificaciones();
        resolve({
          nombre,
          correo,
          puntos: 0,
          partidas_ganadas: 0,
          partidas_jugadas: 0,
          cores: 0,
          skin_activa: "Skin0",
        });
      } else {
        WS.desconectar();
        reject(
          new Error("No se pudo registrar. El usuario o correo ya podría existir.")
        );
      }
    });

    WS.enviar({ tipo: "REGISTRARSE", correo, nombre, password });
  });
}

// ─── Obtener perfil ───────────────────────────────────────────────────────────

/**
 * Pide al servidor los datos actualizados del jugador (puntos, cores, etc.).
 * Útil para refrescar la UI al entrar a la pantalla de partidas o tras una partida.
 * Usa la conexión WS compartida; si no hay conexión activa, devuelve los datos
 * almacenados en sesión sin lanzar error.
 */
export async function obtenerPerfil(nombre: string): Promise<DatosSesion> {
  if (!usarServidor) {
    return obtenerJugadorActivo();
  }

  if (!WS.estaConectado()) {
    return obtenerJugadorActivo();
  }

  if (perfilEnCurso && perfilEnCurso.nombre === nombre) {
    return perfilEnCurso.promise;
  }

  const promise = new Promise<DatosSesion>((resolve, reject) => {
    const timeout = setTimeout(() => {
      unsub();
      reject(new Error("Timeout al obtener perfil."));
    }, 8_000);

    const unsub = WS.suscribir("PERFIL_ACTUALIZADO", (msg) => {
      clearTimeout(timeout);
      unsub();
      resolve(msg as unknown as DatosSesion);
    });

    WS.enviar({ tipo: "OBTENER_PERFIL", nombre });
  });

  perfilEnCurso = { nombre, promise };
  return promise.finally(() => {
    if (perfilEnCurso?.promise === promise) {
      perfilEnCurso = null;
    }
  });
}
