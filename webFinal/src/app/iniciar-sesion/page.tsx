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

export default function IniciarSesionPage() {
  const router = useRouter();
  const [nombre, setNombre] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [errorNombre, setErrorNombre] = useState("");
  const [errorContrasena, setErrorContrasena] = useState("");
  const [errorGeneral, setErrorGeneral] = useState("");
  const [cargando, setCargando] = useState(false);

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
              <input
                id="contrasena"
                type="password"
                value={contrasena}
                onChange={(e) => {
                  setContrasena(e.target.value);
                  setErrorContrasena("");
                  setErrorGeneral("");
                }}
                placeholder="Al menos 8 caracteres con letras y números"
                autoComplete="current-password"
                className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-[#1a2d4a] focus:border-transparent"
              />
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
              <button type="button" className="text-blue-600 hover:underline">
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
