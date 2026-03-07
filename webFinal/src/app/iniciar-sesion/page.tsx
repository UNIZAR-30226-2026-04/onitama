"use client";

/**
 * Pantalla de Inicio de Sesión (prototipos 10.x)
 * Flujo en 2 pasos: 1) correo/usuario, 2) contraseña.
 * Usa src/api/auth.ts para conectar con el servidor (o mock si no está configurado).
 */
import { useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import FondoPantalla from "@/components/FondoPantalla";
import { verificarEmail, login } from "@/api/auth";

export default function IniciarSesionPage() {
  const [paso, setPaso] = useState<1 | 2>(1);
  const [email, setEmail] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [errorEmail, setErrorEmail] = useState("");
  const [errorContrasena, setErrorContrasena] = useState("");
  const [cargando, setCargando] = useState(false);

  const handleContinuarPaso1 = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorEmail("");
    const valor = email.trim();
    if (!valor) {
      setErrorEmail("Introduce tu correo electrónico o nombre de usuario.");
      return;
    }
    setCargando(true);
    try {
      const { existe } = await verificarEmail(valor);
      if (!existe) {
        setErrorEmail("El correo electrónico que ha introducido no existe.");
        return;
      }
      setPaso(2);
    } catch (err) {
      setErrorEmail(
        err instanceof Error ? err.message : "Error al verificar. ¿El servidor está en marcha?"
      );
    } finally {
      setCargando(false);
    }
  };

  const handleContinuarPaso2 = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorContrasena("");
    setCargando(true);
    try {
      await login(email.trim(), contrasena);
      window.location.href = "/";
    } catch (err) {
      setErrorContrasena(
        err instanceof Error ? err.message : "Error al iniciar sesión."
      );
    } finally {
      setCargando(false);
    }
  };

  const hayError = !!errorEmail || !!errorContrasena;

  return (
    <div className="min-h-screen flex flex-col">
      <Header />
      <FondoPantalla />

      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">
          <h1 className="text-2xl font-bold text-center text-gray-900 uppercase mb-6">
            Inicio de sesión
          </h1>

          {paso === 1 ? (
            <form onSubmit={handleContinuarPaso1} className="space-y-4">
              <div>
                <label
                  htmlFor="email"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Introduce tu correo electrónico o nombre de usuario:
                </label>
                <input
                  id="email"
                  type="text"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setErrorEmail("");
                  }}
                  placeholder="ejemplo@correo.com / ejemplodeusuario"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                  autoComplete="email username"
                />
                {errorEmail && (
                  <p className="mt-2 text-sm text-red-600">{errorEmail}</p>
                )}
              </div>

              <div className="flex justify-between text-sm">
                <button type="button" className="text-blue-600 hover:underline">
                  ¿Has olvidado tu contraseña?
                </button>
                <Link
                  href="/registro"
                  className="text-blue-600 hover:underline"
                >
                  ¿No tienes cuenta? Regístrate
                </Link>
              </div>

              <button
                type="submit"
                disabled={hayError || cargando}
                className={`w-full py-3 rounded-xl font-semibold uppercase ${
                  hayError || cargando
                    ? "bg-gray-300 text-gray-500 cursor-not-allowed"
                    : "bg-gray-600 text-white hover:bg-gray-700"
                }`}
              >
                {cargando ? "Comprobando…" : "Continuar"}
              </button>
            </form>
          ) : (
            <form onSubmit={handleContinuarPaso2} className="space-y-4">
              <div>
                <label
                  htmlFor="contrasena"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Introduce la contraseña:
                </label>
                <input
                  id="contrasena"
                  type="password"
                  value={contrasena}
                  onChange={(e) => {
                    setContrasena(e.target.value);
                    setErrorContrasena("");
                  }}
                  placeholder="Usa al menos 8 caracteres con letras y números"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                  autoComplete="current-password"
                />
                {errorContrasena && (
                  <p className="mt-2 text-sm text-red-600">{errorContrasena}</p>
                )}
              </div>

              <button
                type="button"
                onClick={() => setPaso(1)}
                className="block w-full text-blue-600 text-sm hover:underline"
              >
                ← Volver atrás
              </button>

              <button
                type="submit"
                disabled={cargando}
                className="w-full py-3 rounded-xl font-semibold uppercase bg-gray-600 text-white hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {cargando ? "Iniciando sesión…" : "Continuar"}
              </button>
            </form>
          )}
        </div>
      </main>
    </div>
  );
}
