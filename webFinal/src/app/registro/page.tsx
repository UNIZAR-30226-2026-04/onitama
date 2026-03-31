"use client";

/**
 * Pantalla de Registro de nuevo usuario.
 *
 * Flujo en 3 pasos:
 *   Paso 1: Rellenar correo, nombre de usuario y contraseña.
 *   Paso 2: Confirmación de datos antes de enviar.
 *   Paso 3: Selección de avatar antes de registrar.
 *
 * Mensaje enviado al servidor:
 *   { tipo: "REGISTRARSE", correo: string, nombre: string, password: string }
 *
 * Respuestas:
 *   REGISTRO_EXITOSO → redirigir a /iniciar-sesion
 *   REGISTRO_ERRONEO → mostrar error
 */
import { useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useRouter } from "next/navigation";
import HeaderLogo from "@/components/HeaderLogo";
import FondoPantalla from "@/components/FondoPantalla";
import { validarContrasena, HINT_CONTRASENA } from "@/lib/validacion";
import { registrarUsuario } from "@/api/auth";
import { guardarSesion } from "@/lib/sesion";

type Paso = 1 | 2 | 3;
const AVATARES = Array.from({ length: 12 }, (_, i) =>
  `avatar_${String(i + 1).padStart(2, "0")}`
);

export default function RegistroPage() {
  const router = useRouter();
  const [paso, setPaso] = useState<Paso>(1);
  const [correo, setCorreo] = useState("");
  const [nombre, setNombre] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [errorCorreo, setErrorCorreo] = useState("");
  const [errorNombre, setErrorNombre] = useState("");
  const [errorContrasena, setErrorContrasena] = useState("");
  const [errorRegistro, setErrorRegistro] = useState("");
  const [cargando, setCargando] = useState(false);
  const [mostrarContrasena, setMostrarContrasena] = useState(false);
  const [avatarSeleccionado, setAvatarSeleccionado] = useState<string | null>(null);

  // ─── Paso 1: validar datos y pasar a confirmación ─────────────────────────
  const handleContinuar = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorCorreo("");
    setErrorNombre("");
    setErrorContrasena("");

    const correoVal = correo.trim();
    const nombreVal = nombre.trim().replace(/^@/, "");

    if (!correoVal) {
      setErrorCorreo("El correo electrónico es obligatorio.");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correoVal)) {
      setErrorCorreo("Introduce un correo electrónico válido.");
      return;
    }
    if (!nombreVal) {
      setErrorNombre("El nombre de usuario es obligatorio.");
      return;
    }
    if (!validarContrasena(contrasena)) {
      setErrorContrasena("La contraseña debe tener al menos 8 caracteres, una letra y un número.");
      return;
    }

    setPaso(2);
  };

  // ─── Paso 3: enviar al servidor ───────────────────────────────────────────
  const handleFinalizar = async () => {
    setErrorRegistro("");
    setCargando(true);
    try {
      const datos = await registrarUsuario(
        correo.trim(),
        nombre.trim().replace(/^@/, ""),
        contrasena,
        avatarSeleccionado
      );
      guardarSesion(datos);
      router.push("/partidas");
    } catch (err) {
      setErrorRegistro(
        err instanceof Error ? err.message : "Error al registrarse."
      );
    } finally {
      setCargando(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col">
      <FondoPantalla />
      <HeaderLogo />

      <main className="flex-1 flex items-center justify-center px-4 py-6 md:py-8">
        <div className={`w-full bg-white rounded-2xl shadow-xl p-6 md:p-8 ${paso === 3 ? "max-w-3xl" : "max-w-md"}`}>
          <h1 className="text-2xl font-bold text-center text-gray-900 uppercase mb-6">
            Regístrate
          </h1>

          {/* ─── Paso 1: formulario ──────────────────────────────────────── */}
          {paso === 1 && (
            <form onSubmit={handleContinuar} className="space-y-4">
              {/* Correo */}
              <div>
                <label htmlFor="correo" className="block text-sm font-medium text-gray-700 mb-1">
                  Correo electrónico *
                </label>
                <input
                  id="correo"
                  type="email"
                  value={correo}
                  onChange={(e) => { setCorreo(e.target.value); setErrorCorreo(""); }}
                  placeholder="ejemplo@correo.com"
                  autoComplete="email"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                />
                {errorCorreo && <p className="mt-1 text-sm text-red-600">{errorCorreo}</p>}
              </div>

              {/* Nombre de usuario */}
              <div>
                <label htmlFor="nombre" className="block text-sm font-medium text-gray-700 mb-1">
                  Nombre de usuario *
                </label>
                <input
                  id="nombre"
                  type="text"
                  value={nombre}
                  onChange={(e) => { setNombre(e.target.value); setErrorNombre(""); }}
                  placeholder="@ejemplodeusuario"
                  autoComplete="username"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                />
                {errorNombre && <p className="mt-1 text-sm text-red-600">{errorNombre}</p>}
              </div>

              {/* Contraseña */}
              <div>
                <label htmlFor="contrasena" className="block text-sm font-medium text-gray-700 mb-1">
                  Contraseña *
                </label>
                <div className="relative">
                  <input
                    id="contrasena"
                    type={mostrarContrasena ? "text" : "password"}
                    value={contrasena}
                    onChange={(e) => { setContrasena(e.target.value); setErrorContrasena(""); }}
                    placeholder={HINT_CONTRASENA}
                    autoComplete="new-password"
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
                {errorContrasena && <p className="mt-1 text-sm text-red-600">{errorContrasena}</p>}
              </div>

              <p className="text-center text-sm">
                <Link href="/iniciar-sesion" className="text-blue-600 hover:underline">
                  ¿Ya tienes cuenta? Inicia sesión
                </Link>
              </p>

              <button
                type="submit"
                className="w-full py-3 rounded-xl font-semibold uppercase bg-gray-600 text-white hover:bg-gray-700 transition-colors"
              >
                Revisar datos
              </button>
            </form>
          )}

          {/* ─── Paso 2: confirmación ─────────────────────────────────────── */}
          {paso === 2 && (
            <div className="space-y-4">
              <p className="text-gray-600 text-sm">
                Revisa que tus datos sean correctos antes de registrarte.
              </p>

              <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                <p className="text-sm">
                  <span className="font-medium">Correo electrónico:</span>{" "}
                  {correo}
                </p>
                <p className="text-sm">
                  <span className="font-medium">Nombre de usuario:</span>{" "}
                  @{nombre.replace(/^@/, "")}
                </p>
                <p className="text-sm flex items-center gap-2">
                  <span className="font-medium">Contraseña:</span>{" "}
                  <span className="font-mono">
                    {contrasena.length > 0
                      ? mostrarContrasena
                        ? contrasena
                        : "•".repeat(Math.min(contrasena.length, 12))
                      : "(vacía)"}
                  </span>
                  <button
                    type="button"
                    onClick={() => setMostrarContrasena((v) => !v)}
                    className="p-1 rounded-md text-gray-500 hover:text-gray-700 hover:bg-gray-200 transition-colors"
                    title={mostrarContrasena ? "Ocultar contraseña" : "Mostrar contraseña"}
                    aria-label={mostrarContrasena ? "Ocultar contraseña" : "Mostrar contraseña"}
                  >
                    {mostrarContrasena ? (
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                        <line x1="1" y1="1" x2="23" y2="23" />
                      </svg>
                    ) : (
                      <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    )}
                  </button>
                </p>
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setPaso(1)}
                  disabled={cargando}
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-red-600 text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
                >
                  Corregir
                </button>
                <button
                  type="button"
                  onClick={() => setPaso(3)}
                  disabled={cargando}
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-gray-600 text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
                >
                  Siguiente
                </button>
              </div>
            </div>
          )}

          {/* ─── Paso 3: selección de avatar y registro final ─────────────── */}
          {paso === 3 && (
            <div className="space-y-6">
              <p className="text-gray-600 text-sm text-center">
                Último paso: elige tu foto de perfil para completar el registro.
              </p>

              <div className="flex justify-center">
                <button
                  type="button"
                  onClick={() => setAvatarSeleccionado(null)}
                  className={`rounded-xl border px-4 py-2 text-sm font-medium transition-colors ${
                    avatarSeleccionado === null
                      ? "border-[#1a2d4a] bg-[#1a2d4a]/10 text-[#1a2d4a]"
                      : "border-gray-300 text-gray-700 hover:bg-gray-50"
                  }`}
                >
                  No elegir foto (usar inicial del nombre)
                </button>
              </div>

              <div className="grid grid-cols-3 md:grid-cols-4 gap-4 md:gap-5 justify-items-center">
                {AVATARES.map((avatarId) => {
                  const activo = avatarSeleccionado === avatarId;
                  return (
                    <button
                      key={avatarId}
                      type="button"
                      onClick={() => setAvatarSeleccionado(avatarId)}
                      className={`rounded-full p-1 transition-all ${
                        activo
                          ? "ring-4 ring-[#1a2d4a] ring-offset-2"
                          : "hover:scale-105"
                      }`}
                      title={`Elegir ${avatarId}`}
                      aria-label={`Elegir ${avatarId}`}
                    >
                      <span className="block w-20 h-20 md:w-24 md:h-24 rounded-full overflow-hidden">
                        <Image
                          src={`/${avatarId}.png`}
                          alt={avatarId}
                          width={224}
                          height={224}
                          className="w-full h-full object-cover"
                        />
                      </span>
                    </button>
                  );
                })}
              </div>

              {errorRegistro && (
                <p className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-lg">
                  {errorRegistro}
                </p>
              )}

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={() => setPaso(2)}
                  disabled={cargando}
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-red-600 text-white hover:bg-red-700 disabled:opacity-50 transition-colors"
                >
                  Atrás
                </button>
                <button
                  type="button"
                  onClick={handleFinalizar}
                  disabled={cargando}
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-gray-600 text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
                >
                  {cargando ? "Registrando…" : "Confirmar registro"}
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
