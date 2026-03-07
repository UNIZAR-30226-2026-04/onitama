"use client";

/**
 * Pantalla – Buscar Partida
 * Muestra un botón para iniciar la búsqueda de partida.
 * La búsqueda se realiza enviando un mensaje vía WebSocket al servidor Java
 * (ver src/api/buscarpartida.ts).
 * Mientras el servidor no esté listo se usa la respuesta mock.
 */
import { useState } from "react";
import Header from "@/components/Header";
import FondoPantalla from "@/components/FondoPantalla";
import { buscarPartida, type RespuestaBuscarPartida } from "@/api/buscarpartida";

type EstadoUI = "idle" | "buscando" | "resultado";

export default function BuscarPartidaPage() {
  const [estadoUI, setEstadoUI] = useState<EstadoUI>("idle");
  const [respuesta, setRespuesta] = useState<RespuestaBuscarPartida | null>(null);

  const handleBuscar = async () => {
    setEstadoUI("buscando");
    setRespuesta(null);

    const resultado = await buscarPartida();

    setRespuesta(resultado);
    setEstadoUI("resultado");
  };

  const handleReintentar = () => {
    setEstadoUI("idle");
    setRespuesta(null);
  };

  // Color de la tarjeta de respuesta según el estado recibido
  const colorEstado: Record<string, string> = {
    ENCONTRADA: "border-green-400 bg-green-900/40 text-green-200",
    BUSCANDO:   "border-blue-400  bg-blue-900/40  text-blue-200",
    ERROR:      "border-red-400   bg-red-900/40   text-red-200",
    TIMEOUT:    "border-yellow-400 bg-yellow-900/40 text-yellow-200",
  };

  const claseEstado = respuesta
    ? (colorEstado[respuesta.estado] ?? "border-gray-400 bg-gray-900/40 text-gray-200")
    : "";

  return (
    <div className="min-h-screen flex flex-col relative">
      <Header />
      <FondoPantalla />

      <main className="flex-1 flex flex-col items-center justify-center gap-8 px-4 py-12">
        {/* Tarjeta central */}
        <div className="w-full max-w-md bg-[#1a2d4a]/80 backdrop-blur-sm border border-white/10 rounded-2xl shadow-2xl p-10 flex flex-col items-center gap-6">
          <h1 className="text-3xl font-bold text-white uppercase tracking-widest text-center">
            Buscar Partida
          </h1>

          <p className="text-white/70 text-sm text-center">
            Pulsa el botón para buscar un oponente.<br />
            El servidor responderá en cuanto encuentre una partida disponible.
          </p>

          {/* Botón principal */}
          {estadoUI !== "buscando" && (
            <button
              type="button"
              onClick={estadoUI === "resultado" ? handleReintentar : handleBuscar}
              className={`w-full py-4 rounded-xl font-bold uppercase tracking-widest text-lg transition-all duration-200 ${
                estadoUI === "resultado"
                  ? "bg-white/10 text-white/80 border border-white/20 hover:bg-white/20"
                  : "bg-[#e8e8e8] text-[#1a2d4a] hover:bg-white hover:scale-[1.02] active:scale-100"
              }`}
            >
              {estadoUI === "resultado" ? "Buscar de nuevo" : "Buscar partida"}
            </button>
          )}

          {/* Estado: buscando */}
          {estadoUI === "buscando" && (
            <div className="flex flex-col items-center gap-4 py-4">
              {/* Spinner */}
              <svg
                className="animate-spin h-12 w-12 text-white/70"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                aria-hidden
              >
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
                />
              </svg>
              <p className="text-white/80 text-sm animate-pulse">Buscando partida…</p>
            </div>
          )}

          {/* Respuesta del servidor */}
          {estadoUI === "resultado" && respuesta && (
            <div
              className={`w-full rounded-xl border-2 px-6 py-5 flex flex-col gap-2 ${claseEstado}`}
            >
              <p className="text-xs font-bold uppercase tracking-widest opacity-70">
                Respuesta del servidor
              </p>
              <p className="font-semibold text-base break-words">{respuesta.mensaje}</p>

              <div className="flex flex-wrap gap-x-6 gap-y-1 mt-1 text-xs opacity-80">
                <span>
                  Estado:{" "}
                  <span className="font-bold">{respuesta.estado}</span>
                </span>
                {respuesta.partida_id && (
                  <span>
                    ID partida:{" "}
                    <span className="font-mono font-bold">{respuesta.partida_id}</span>
                  </span>
                )}
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}

