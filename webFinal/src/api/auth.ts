/**
 * Cliente API de autenticación.
 * Conecta el frontend con el servidor Java para login y registro.
 *
 * Cuando NEXT_PUBLIC_API_URL no está configurada, se usa lógica mock para desarrollo.
 * Cuando el servidor esté listo: crear .env.local con NEXT_PUBLIC_API_URL y ajustar
 * las rutas/contratos según la API que exponga el servidor.
 */

const API_BASE = process.env.NEXT_PUBLIC_API_URL || "";

/** Indica si hay un servidor configurado (usa API real en lugar de mock) */
const usarServidor = !!API_BASE;

// =============================================================================
// MOCK - Solo para desarrollo sin servidor. Eliminar cuando el servidor esté listo.
// =============================================================================
const EMAIL_NO_EXISTE_MOCK = "noexiste@test.com";
const CONTRASENA_CORRECTA_MOCK = "password123";

// =============================================================================
// VERIFICAR EMAIL (Paso 1 del login)
// =============================================================================

/**
 * Verifica si el correo existe en el servidor.
 * TODO: Reemplazar por el endpoint real cuando el servidor esté listo.
 * Ejemplo esperado: POST /api/auth/verificar-email con body { email: string }
 * Respuesta: { existe: boolean }
 */
export async function verificarEmail(
  identificador: string
): Promise<{ existe: boolean }> {
  if (!usarServidor) {
    const esEmail = identificador.includes("@");
    const existe = !(esEmail && identificador === EMAIL_NO_EXISTE_MOCK);
    return { existe };
  }

  const res = await fetch(`${API_BASE}/api/auth/verificar-email`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: identificador.includes("@") ? identificador : undefined,
      usuario: !identificador.includes("@") ? identificador : undefined,
    }),
  });

  if (!res.ok) {
    const error = await res.text();
    throw new Error(error || "Error al verificar el correo");
  }
  return res.json();
}

// =============================================================================
// LOGIN (Paso 2 del login)
// =============================================================================

/**
 * Inicia sesión con identificador (email o usuario) y contraseña.
 * TODO: Ajustar endpoint y body según la API del servidor.
 * Ejemplo esperado: POST /api/auth/login con body { identificador, password }
 * Respuesta: { token: string } o similar
 */
export async function login(
  identificador: string,
  password: string
): Promise<{ token?: string }> {
  if (!usarServidor) {
    if (password !== CONTRASENA_CORRECTA_MOCK) {
      throw new Error("La contraseña que ha introducido es incorrecta.");
    }
    return { token: "mock-token" };
  }

  const res = await fetch(`${API_BASE}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      identificador,
      password,
    }),
  });

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    const error =
      data?.mensaje || data?.message || (await res.text()) || "Error al iniciar sesión";
    throw new Error(error);
  }
  return res.json();
}

// =============================================================================
// REGISTRO
// =============================================================================

export interface DatosRegistro {
  email: string;
  usuario: string;
  password: string;
}

/**
 * Registra un nuevo usuario.
 * TODO: Ajustar endpoint y body según la API del servidor.
 * Ejemplo esperado: POST /api/auth/registro con body { email, usuario, password }
 */
export async function registrar(datos: DatosRegistro): Promise<void> {
  if (!usarServidor) {
    // Mock: no hace nada, solo simula éxito
    return;
  }

  const res = await fetch(`${API_BASE}/api/auth/registro`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(datos),
  });

  if (!res.ok) {
    const data = await res.json().catch(() => ({}));
    const error =
      data?.mensaje || data?.message || (await res.text()) || "Error al registrarse";
    throw new Error(error);
  }
}
