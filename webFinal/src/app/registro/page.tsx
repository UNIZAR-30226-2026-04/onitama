"use client";

/**
 * Pantalla de Registro de nuevo usuario (prototipos 11.x)
 * Flujo en 3 pasos: 1) email + usuario, 2) contraseña, 3) confirmación.
 * Sin servidor: no se crea usuario real, solo se muestra el flujo completo.
 */
import { useState } from "react";
import Link from "next/link";
import Header from "@/components/Header";
import FondoPantalla from "@/components/FondoPantalla";
import { validarContrasena, HINT_CONTRASENA } from "@/lib/validacion";
import { registrar } from "@/api/auth";

type Paso = 1 | 2 | 3;

export default function RegistroPage() {
  const [paso, setPaso] = useState<Paso>(1);
  const [email, setEmail] = useState("");
  const [usuario, setUsuario] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [errorEmail, setErrorEmail] = useState("");
  const [errorUsuario, setErrorUsuario] = useState("");
  const [errorContrasena, setErrorContrasena] = useState("");
  const [errorRegistro, setErrorRegistro] = useState("");
  const [cargando, setCargando] = useState(false);

  const handleContinuarPaso1 = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorEmail("");
    setErrorUsuario("");

    const emailVal = email.trim();
    const usuarioVal = usuario.trim();

    if (!emailVal) {
      setErrorEmail("El correo electrónico es obligatorio.");
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailVal)) {
      setErrorEmail("Introduce un correo electrónico válido.");
      return;
    }
    if (!usuarioVal) {
      setErrorUsuario("El nombre de usuario es obligatorio.");
      return;
    }
    setPaso(2);
  };

  const handleContinuarPaso2 = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorContrasena("");
    if (!validarContrasena(contrasena)) {
      setErrorContrasena(
        "La contraseña debe tener al menos 8 caracteres, una letra y un número."
      );
      return;
    }
    setPaso(3);
  };

  const handleVolverPaso2 = () => {
    setPaso(2);
    setErrorRegistro("");
  };
  const handleVolverPaso1 = () => setPaso(1);

  const handleFinalizarContinuar = async () => {
    setErrorRegistro("");
    setCargando(true);
    try {
      await registrar({
        email: email.trim(),
        usuario: usuario.trim().replace(/^@/, ""),
        password: contrasena,
      });
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
      <Header />
      <FondoPantalla />

      <main className="flex-1 flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-md bg-white rounded-2xl shadow-xl p-8">
          <h1 className="text-2xl font-bold text-center text-gray-900 uppercase mb-6">
            Regístrate
          </h1>

          {paso === 1 && (
            <form onSubmit={handleContinuarPaso1} className="space-y-4">
              <div>
                <label
                  htmlFor="email"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Introduce tu correo electrónico *
                </label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    setErrorEmail("");
                  }}
                  placeholder="ejemplo@correo.com"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                  autoComplete="email"
                />
                {errorEmail && (
                  <p className="mt-2 text-sm text-red-600">{errorEmail}</p>
                )}
              </div>

              <div>
                <label
                  htmlFor="usuario"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Introduce tu nombre de usuario *
                </label>
                <input
                  id="usuario"
                  type="text"
                  value={usuario}
                  onChange={(e) => {
                    setUsuario(e.target.value);
                    setErrorUsuario("");
                  }}
                  placeholder="@ejemplodeusuario"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                  autoComplete="username"
                />
                {errorUsuario && (
                  <p className="mt-2 text-sm text-red-600">{errorUsuario}</p>
                )}
              </div>

              <p className="text-center text-sm">
                <Link href="/iniciar-sesion" className="text-blue-600 hover:underline">
                  ¿Ya tienes cuenta? Inicia sesión
                </Link>
              </p>

              <button
                type="submit"
                className="w-full py-3 rounded-xl font-semibold uppercase bg-gray-600 text-white hover:bg-gray-700"
              >
                Continuar
              </button>
            </form>
          )}

          {paso === 2 && (
            <form onSubmit={handleContinuarPaso2} className="space-y-4">
              <div>
                <label
                  htmlFor="contrasena"
                  className="block text-sm font-medium text-gray-700 mb-1"
                >
                  Introduce una contraseña *
                </label>
                <input
                  id="contrasena"
                  type="password"
                  value={contrasena}
                  onChange={(e) => {
                    setContrasena(e.target.value);
                    setErrorContrasena("");
                  }}
                  placeholder={HINT_CONTRASENA}
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
                  autoComplete="new-password"
                />
                {errorContrasena && (
                  <p className="mt-2 text-sm text-red-600">{errorContrasena}</p>
                )}
              </div>

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={handleVolverPaso1}
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-red-600 text-white hover:bg-red-700"
                >
                  Volver
                </button>
                <button
                  type="submit"
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-gray-400 text-gray-800 hover:bg-gray-500"
                >
                  Continuar
                </button>
              </div>
            </form>
          )}

          {paso === 3 && (
            <div className="space-y-6">
              <p className="text-gray-700">
                ¡Todo listo! Confirma que tus datos son correctos.
              </p>
              <p className="text-gray-600 text-sm">
                Recuerda que puedes editar tu perfil en todo momento.
              </p>

              <div className="bg-gray-50 rounded-xl p-4 space-y-2">
                <p className="text-sm">
                  <span className="font-medium">Correo electrónico:</span>{" "}
                  {email || "ejemplo@gmail.com"}
                </p>
                <p className="text-sm">
                  <span className="font-medium">Nombre de usuario:</span>{" "}
                  {usuario ? `@${usuario.replace(/^@/, "")}` : "@ejemplodeusuario"}
                </p>
              </div>

              {errorRegistro && (
                <p className="text-sm text-red-600">{errorRegistro}</p>
              )}

              <div className="flex gap-3">
                <button
                  type="button"
                  onClick={handleVolverPaso2}
                  disabled={cargando}
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-red-600 text-white hover:bg-red-700 disabled:opacity-50"
                >
                  Volver
                </button>
                <button
                  type="button"
                  onClick={handleFinalizarContinuar}
                  disabled={cargando}
                  className="flex-1 py-3 rounded-xl font-semibold uppercase bg-gray-400 text-gray-800 hover:bg-gray-500 disabled:opacity-50"
                >
                  {cargando ? "Registrando…" : "Continuar"}
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
