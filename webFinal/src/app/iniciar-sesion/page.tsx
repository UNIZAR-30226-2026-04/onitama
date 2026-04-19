"use client";

/**
 * Pantalla de Inicio de Sesión
 *
 * Formulario único con nombre de usuario y contraseña (en una sola pantalla).
 * El servidor espera:  { tipo: "INICIAR_SESION", nombre, password }
 * (Antes se pedía el correo; ahora el servidor identifica al usuario por nombre.)
 *
 * Tras el login exitoso, guarda los datos del jugador en sessionStorage (sesion.ts)
 * y redirige a la pantalla principal.
 */
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import HeaderLogo from "@/components/HeaderLogo";
import FondoPantalla from "@/components/FondoPantalla";
import { iniciarSesion } from "@/api/auth";
import { guardarSesion } from "@/lib/sesion";
import { validarContrasena } from "@/lib/validacion";
import * as WS from "@/api/ws";

export default function IniciarSesionPage() {
  const router = useRouter();
  const [nombre, setNombre] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [errorNombre, setErrorNombre] = useState("");
  const [errorContrasena, setErrorContrasena] = useState("");
  const [errorGeneral, setErrorGeneral] = useState("");
  const [cargando, setCargando] = useState(false);
  const [mostrarContrasena, setMostrarContrasena] = useState(false);

  // ── Modal recuperar contraseña ───────────────────────────────────────────
  const [mostrarModalRecuperar, setMostrarModalRecuperar] = useState(false);
  const [correoRecuperar, setCorreoRecuperar] = useState("");
  const [estadoRecuperar, setEstadoRecuperar] = useState<
    "idle" | "enviando" | "enviado" | "error_noexiste" | "error_email" | "error_timeout"
  >("idle");

  const handleRecuperarContrasena = async (e: React.FormEvent) => {
    e.preventDefault();
    const correo = correoRecuperar.trim();
    if (!correo) return;
    setEstadoRecuperar("enviando");

    try {
      await WS.conectar();
      await new Promise<void>((resolve, reject) => {
        const limpiar = () => { unsubOk(); unsubNoExiste(); unsubEmailErr(); };
        const unsubOk       = WS.suscribir("CONTRASENA_ENVIADA",   () => { limpiar(); resolve(); });
        const unsubNoExiste = WS.suscribir("CORREO_NO_ENCONTRADO", () => { limpiar(); reject(new Error("no_existe")); });
        const unsubEmailErr = WS.suscribir("ERROR_EMAIL",          () => { limpiar(); reject(new Error("email")); });
        const enviado = WS.enviar({ tipo: "RECUPERAR_CONTRASENA", correo });
        if (!enviado) { limpiar(); reject(new Error("sin_conexion")); }
        setTimeout(() => { limpiar(); reject(new Error("timeout")); }, 20_000);
      });
      setEstadoRecuperar("enviado");
    } catch (err) {
      const msg = err instanceof Error ? err.message : "";
      if (msg === "email")    setEstadoRecuperar("error_email");
      else if (msg === "timeout") setEstadoRecuperar("error_timeout");
      else                    setEstadoRecuperar("error_noexiste");
    }
  };

  const cerrarModalRecuperar = () => {
    setMostrarModalRecuperar(false);
    setCorreoRecuperar("");
    setEstadoRecuperar("idle");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorNombre("");
    setErrorContrasena("");
    setErrorGeneral("");

    // Validación local antes de enviar al servidor
    const nombreVal = nombre.trim();
    if (!nombreVal) {
      setErrorNombre("Introduce tu nombre de usuario.");
      return;
    }
    if (!contrasena) {
      setErrorContrasena("Introduce tu contraseña.");
      return;
    }
    if (!validarContrasena(contrasena)) {
      setErrorContrasena("La contraseña debe tener al menos 8 caracteres, una letra y un número.");
      return;
    }

    setCargando(true);
    try {
      const datos = await iniciarSesion(nombreVal, contrasena);
      // Guardar datos del jugador para toda la sesión
      guardarSesion(datos);
      router.push("/partidas");
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Error al iniciar sesión.";
      // Distribuir el error al campo correspondiente si es posible
      if (msg.toLowerCase().includes("usuario")) {
        setErrorNombre(msg);
      } else if (msg.toLowerCase().includes("contraseña") || msg.toLowerCase().includes("password")) {
        setErrorContrasena(msg);
      } else {
        setErrorGeneral(msg);
      }
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col">
      <FondoPantalla />
      <HeaderLogo />

      {/* ── Modal: recuperar contraseña ───────────────────────────────── */}
      {mostrarModalRecuperar && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4">
          <div className="bg-white rounded-2xl shadow-2xl p-8 w-full max-w-sm flex flex-col gap-5">
            <h2 className="text-xl font-bold text-gray-900 uppercase text-center">
              Recuperar contraseña
            </h2>

            {estadoRecuperar === "enviado" ? (
              <>
                <p className="text-green-700 bg-green-50 border border-green-200 rounded-xl px-4 py-3 text-sm text-center">
                  ✅ ¡Listo! Revisa tu correo — te hemos enviado tu nueva contraseña. Mira también la carpeta de spam.
                </p>
                <button
                  type="button"
                  onClick={cerrarModalRecuperar}
                  className="w-full py-3 rounded-xl font-semibold bg-gray-800 text-white hover:bg-gray-700 transition-colors"
                >
                  Cerrar
                </button>
              </>
            ) : (
              <form onSubmit={handleRecuperarContrasena} className="flex flex-col gap-4">
                <p className="text-gray-600 text-sm text-center">
                  Introduce tu correo y haz click en el botón para recibir tu nueva contraseña.
                </p>
                <input
                  type="email"
                  required
                  placeholder="tu@correo.com"
                  value={correoRecuperar}
                  onChange={(e) => { setCorreoRecuperar(e.target.value); setEstadoRecuperar("idle" as "idle"); }}
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a]"
                />
                {estadoRecuperar === "error_noexiste" && (
                  <p className="text-red-600 text-sm bg-red-50 border border-red-200 rounded-xl px-3 py-2 text-center">
                    No encontramos ninguna cuenta con ese correo.
                  </p>
                )}
                {estadoRecuperar === "error_email" && (
                  <p className="text-orange-600 text-sm bg-orange-50 border border-orange-200 rounded-xl px-3 py-2 text-center">
                    Cuenta encontrada pero no se pudo enviar el email. Inténtalo de nuevo.
                  </p>
                )}
                {estadoRecuperar === "error_timeout" && (
                  <p className="text-yellow-700 text-sm bg-yellow-50 border border-yellow-200 rounded-xl px-3 py-2 text-center">
                    El servidor tardó demasiado. Comprueba tu conexión e inténtalo de nuevo.
                  </p>
                )}
                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={cerrarModalRecuperar}
                    className="flex-1 py-3 rounded-xl font-semibold border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors"
                  >
                    Cancelar
                  </button>
                  <button
                    type="submit"
                    disabled={estadoRecuperar === "enviando"}
                    className="flex-1 py-3 rounded-xl font-semibold bg-gray-800 text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
                  >
                    {estadoRecuperar === "enviando" ? "Enviando…" : "Recibir nueva contraseña"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">
          <h1 className="text-2xl font-bold text-center text-gray-900 uppercase mb-6">
            Inicio de sesión
          </h1>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* Campo: Nombre de usuario */}
            <div>
              <label
                htmlFor="nombre"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Nombre de usuario
              </label>
              <input
                id="nombre"
                type="text"
                value={nombre}
                onChange={(e) => {
                  setNombre(e.target.value);
                  setErrorNombre("");
                  setErrorGeneral("");
                }}
                placeholder="Tu nombre de usuario"
                autoComplete="username"
                className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
              />
              {errorNombre && (
                <p className="mt-1 text-sm text-red-600">{errorNombre}</p>
              )}
            </div>

            {/* Campo: Contraseña */}
            <div>
              <label
                htmlFor="contrasena"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Contraseña
              </label>
              <div className="relative">
                <input
                  id="contrasena"
                  type={mostrarContrasena ? "text" : "password"}
                  value={contrasena}
                  onChange={(e) => {
                    setContrasena(e.target.value);
                    setErrorContrasena("");
                    setErrorGeneral("");
                  }}
                  placeholder="Al menos 8 caracteres con letras y números"
                  autoComplete="current-password"
                  className="w-full px-4 py-3 pr-12 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                />
                <button
                  type="button"
                  onClick={() => setMostrarContrasena((v) => !v)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 p-1 rounded-md text-gray-500 hover:text-gray-700 hover:bg-gray-100 transition-colors"
                  title={mostrarContrasena ? "Ocultar contraseña" : "Mostrar contraseña"}
                  aria-label={mostrarContrasena ? "Ocultar contraseña" : "Mostrar contraseña"}
                >
                  {mostrarContrasena ? (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  ) : (
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
              {errorContrasena && (
                <p className="mt-1 text-sm text-red-600">{errorContrasena}</p>
              )}
            </div>

            {/* Error general (de red, timeout, etc.) */}
            {errorGeneral && (
              <p className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-lg">
                {errorGeneral}
              </p>
            )}

            {/* Links secundarios */}
            <div className="flex justify-between text-sm pt-1">
              <button
                type="button"
                className="text-blue-600 hover:underline"
                onClick={() => setMostrarModalRecuperar(true)}
              >
                ¿Olvidaste tu contraseña?
              </button>
              <Link href="/registro" className="text-blue-600 hover:underline">
                ¿No tienes cuenta? Regístrate
              </Link>
            </div>

            {/* Botón de envío */}
            <button
              type="submit"
              disabled={cargando}
              className="w-full py-3 rounded-xl font-semibold uppercase bg-gray-600 text-white hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {cargando ? "Iniciando sesión…" : "Iniciar sesión"}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
}
