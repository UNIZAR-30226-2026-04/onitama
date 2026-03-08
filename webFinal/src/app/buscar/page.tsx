"use client";

/**
 * Pantalla – Buscar Partida Pública
 * La búsqueda comienza automáticamente al cargar la pantalla.
 * Cuando el servidor (o el mock) responde con ENCONTRADA, navega a /partida.
 *
 * Flujo:
 *  1. Al montar el componente → llamar a buscarPartida()
 *  2. ENCONTRADA → router.push("/partida?id=<partida_id>")
 *  3. ERROR / TIMEOUT → mostrar mensaje y botón de reintento
 */
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import HeaderLogo from "@/components/HeaderLogo";
import FondoPantalla from "@/components/FondoPantalla";
import { buscarPartida, type RespuestaBuscarPartida } from "@/api/buscarpartida";

type EstadoUI = "buscando" | "error" | "timeout";

export default function BuscarPartidaPage() {
  const router = useRouter();
  const [estadoUI, setEstadoUI] = useState<EstadoUI>("buscando");
  const [respuesta, setRespuesta] = useState<RespuestaBuscarPartida | null>(null);

  /** Inicia la búsqueda de partida (se llama al montar y en reintentos) */
  const iniciarBusqueda = () => {
    setEstadoUI("buscando");
    setRespuesta(null);

    buscarPartida().then((resultado) => {
      setRespuesta(resultado);

      if (resultado.estado === "ENCONTRADA") {
        // Navegar a la pantalla de partida con el id recibido
        const id = resultado.partida_id ?? "local";
        router.push(`/partida?id=${encodeURIComponent(id)}`);
      } else if (resultado.estado === "TIMEOUT") {
        setEstadoUI("timeout");
      } else {
        setEstadoUI("error");
      }
    });
  };

  // Iniciar búsqueda al montar el componente
  useEffect(() => {
    iniciarBusqueda();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
    <div className="min-h-screen flex flex-col relative">
      <FondoPantalla />
      <HeaderLogo />

      <main className="flex-1 flex flex-col items-center justify-center gap-8 px-4 py-12">
        <div className="w-full max-w-md bg-[#1a2d4a]/80 backdrop-blur-sm border border-white/10 rounded-2xl shadow-2xl p-10 flex flex-col items-center gap-6">
          <h1 className="text-3xl font-bold text-white uppercase tracking-widest text-center">
            Partida Pública
          </h1>

          {/* Estado: buscando */}
          {estadoUI === "buscando" && (
            <>
              <p className="text-white/70 text-sm text-center">
                Buscando un oponente en línea…
              </p>
              {/* Spinner animado */}
              <svg
                className="animate-spin h-14 w-14 text-white/60"
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                aria-hidden
              >
                <circle
                  className="opacity-20"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                />
                <path
                  className="opacity-80"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"
                />
              </svg>
              <p className="text-white/50 text-xs animate-pulse">
                Esto puede tardar unos segundos…
              </p>
            </>
          )}

          {/* Estado: error o timeout */}
          {(estadoUI === "error" || estadoUI === "timeout") && respuesta && (
            <>
              <div className="w-full rounded-xl border-2 border-red-400 bg-red-900/30 px-6 py-4 text-red-200 text-sm text-center">
                <p className="font-bold mb-1 uppercase tracking-wide">
                  {estadoUI === "timeout" ? "Tiempo agotado" : "Error de conexión"}
                </p>
                <p>{respuesta.mensaje}</p>
              </div>

              <button
                type="button"
                onClick={iniciarBusqueda}
                className="w-full py-4 rounded-xl font-bold uppercase tracking-widest text-lg bg-[#e8e8e8] text-[#1a2d4a] hover:bg-white hover:scale-[1.02] active:scale-100 transition-all duration-200"
              >
                Reintentar
              </button>

              <button
                type="button"
                onClick={() => router.push("/partidas")}
                className="text-white/50 text-sm hover:text-white/80 transition-colors"
              >
                ← Volver a partidas
              </button>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
