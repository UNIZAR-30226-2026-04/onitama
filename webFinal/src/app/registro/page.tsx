"use client";

/**
 * Pantalla de Registro de nuevo usuario.
 *
 * Flujo en 2 pasos:
 *   Paso 1: Rellenar correo, nombre de usuario y contraseña.
 *   Paso 2: Confirmación de datos antes de enviar.
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
import HeaderLogo from "@/components/HeaderLogo";
import FondoPantalla from "@/components/FondoPantalla";
import { validarContrasena, HINT_CONTRASENA } from "@/lib/validacion";
import { registrarUsuario } from "@/api/auth";

type Paso = 1 | 2;

export default function RegistroPage() {
  const [paso, setPaso] = useState<Paso>(1);
  const [correo, setCorreo] = useState("");
  const [nombre, setNombre] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [errorCorreo, setErrorCorreo] = useState("");
  const [errorNombre, setErrorNombre] = useState("");
  const [errorContrasena, setErrorContrasena] = useState("");
  const [errorRegistro, setErrorRegistro] = useState("");
  const [cargando, setCargando] = useState(false);

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

  // ─── Paso 2: enviar al servidor ───────────────────────────────────────────
  const handleFinalizar = async () => {
    setErrorRegistro("");
    setCargando(true);
    try {
      await registrarUsuario(
        correo.trim(),
        nombre.trim().replace(/^@/, ""),
        contrasena
      );
      window.location.href = "/iniciar-sesion";
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

      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">
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
                <input
                  id="contrasena"
                  type="password"
                  value={contrasena}
                  onChange={(e) => { setContrasena(e.target.value); setErrorContrasena(""); }}
                  placeholder={HINT_CONTRASENA}
                  autoComplete="new-password"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                />
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
            <div className="space-y-6">
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
                <p className="text-sm">
                  <span className="font-medium">Contraseña:</span>{" "}
                  {"•".repeat(Math.min(contrasena.length, 12))}
                </p>
              </div>

              {errorRegistro && (
                <p className="text-sm text-red-600 bg-red-50 px-3 py-2 rounded-lg">
                  {errorRegistro}
                </p>
              )}

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
                  onClick={handleFinalizar}
                  disabled={cargando}
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-gray-600 text-white hover:bg-gray-700 disabled:opacity-50 transition-colors"
                >
                  {cargando ? "Registrando…" : "Confirmar"}
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
