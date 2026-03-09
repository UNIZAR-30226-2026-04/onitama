"use client";

/**
 * Pantalla – Buscar Partida Pública
 * La búsqueda comienza automáticamente al cargar la pantalla.
 * Cuando el servidor (o el mock) responde con ENCONTRADA, muestra una
 * pantalla de "VS" (presentación) durante unos segundos y luego navega a /partida.
 *
 * Flujo:
 *  1. Al montar el componente → llamar a buscarPartida()
 *  2. ENCONTRADA → estadoUI = "presentacion" → mostrar pantalla VS por 3.5s
 *  3. TIMEOUT → router.push("/partida?id=<partida_id>")
 *  4. ERROR / TIMEOUT → mostrar mensaje y botón de reintento
 */
import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import HeaderLogo from "@/components/HeaderLogo";
import FondoPantalla from "@/components/FondoPantalla";
import { buscarPartida, type RespuestaBuscarPartida } from "@/api/buscarpartida";
import { mockJugador } from "@/lib/mockJugador";

type EstadoUI = "buscando" | "error" | "timeout" | "presentacion";

export default function BuscarPartidaPage() {
  const router = useRouter();
  const [estadoUI, setEstadoUI] = useState<EstadoUI>("buscando");
  const [respuesta, setRespuesta] = useState<RespuestaBuscarPartida | null>(null);

  /** Inicia la búsqueda de partida (se llama al montar y en reintentos) */
  const iniciarBusqueda = () => {
    setEstadoUI("buscando");
    setRespuesta(null);
    // Limpiar datos de partida anterior (por si el jugador vuelve a buscar)
    sessionStorage.removeItem("datosPartida");

    // Pasar nombre y puntos para que el servidor haga matchmaking por puntuación
    buscarPartida(mockJugador.nombre, mockJugador.puntos).then((resultado) => {
      setRespuesta(resultado);

      if (resultado.estado === "ENCONTRADA") {
        // En lugar de navegar inmediatamente, mostramos la pantalla VS
        setEstadoUI("presentacion");

        // Esperamos 3.5 segundos en la pantalla de presentación antes de ir a jugar
        setTimeout(() => {
          const id = resultado.partida_id ?? "local";
          router.push(`/partida?id=${encodeURIComponent(id)}`);
        }, 3500);

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

  // ─── PANTALLA INTERMEDIA DE PRESENTACIÓN (VS) ─────────────────────────────
  if (estadoUI === "presentacion" && respuesta) {
    return (
      <div className="min-h-screen flex flex-col relative overflow-hidden bg-[#111d2c]">
        <FondoPantalla />
        
        {/* Header con el logo centrado (sin botones ni katanas/cores extra) */}
        <header className="relative z-10 px-8 py-8 flex justify-center mt-4">
          <Image src="/nombre.png" alt="Onitama" width={220} height={66} priority className="h-16 w-auto object-contain" />
        </header>

        {/* Contenido principal (Luchadores y VS) */}
        <main className="relative z-10 flex-1 flex items-center justify-center px-4 pb-20">
          <div className="flex items-center justify-center gap-4 sm:gap-12 w-full max-w-4xl">
            
            {/* Oponente (Rojo) - Ahora a la izquierda */}
            <div className="flex flex-col items-center gap-6 flex-1">
              <div className="relative w-40 h-40 sm:w-56 sm:h-56 drop-shadow-[0_0_25px_rgba(239,68,68,0.4)]">
                <Image src="/luchadorRojo.PNG" alt="Oponente" fill className="object-contain" priority />
              </div>
              <div className="bg-[#1a2d4a]/80 backdrop-blur-md px-8 py-4 rounded-xl border-2 border-red-500/40 flex flex-col items-center shadow-[0_0_15px_rgba(239,68,68,0.2)]">
                <span className="text-white font-bold text-xl tracking-wider">@{respuesta.oponente || "Oponente"}</span>
                <div className="flex items-center gap-2 mt-2">
                  <Image src="/katanas.png" alt="Katanas" width={28} height={28} className="h-6 w-auto drop-shadow-md" />
                  <span className="text-red-200 font-mono font-bold text-lg">{(respuesta.oponentePt || 1000).toLocaleString()}</span>
                </div>
              </div>
            </div>

            {/* VS Logo (Centro) - Mucho más grande */}
            <div className="flex flex-col items-center shrink-0 mx-[-20px] sm:mx-[-100px] z-20">
              <div className="relative w-40 h-40 sm:w-100 sm:h-100 drop-shadow-[0_0_40px_rgba(255,255,255,0.4)] animate-pulse">
                <Image src="/vs.png" alt="VS" fill className="object-contain" priority />
              </div>
            </div>

            {/* Jugador Local (Azul) - Ahora a la derecha */}
            <div className="flex flex-col items-center gap-6 flex-1">
              <div className="relative w-40 h-40 sm:w-56 sm:h-56 drop-shadow-[0_0_25px_rgba(59,130,246,0.4)]">
                <Image src="/luchadorAzul.PNG" alt="Mi luchador" fill className="object-contain" priority />
              </div>
              <div className="bg-[#1a2d4a]/80 backdrop-blur-md px-8 py-4 rounded-xl border-2 border-blue-500/40 flex flex-col items-center shadow-[0_0_15px_rgba(59,130,246,0.2)]">
                <span className="text-white font-bold text-xl tracking-wider">@{mockJugador.nombre}</span>
                <div className="flex items-center gap-2 mt-2">
                  <Image src="/katanas.png" alt="Katanas" width={28} height={28} className="h-6 w-auto drop-shadow-md" />
                  <span className="text-blue-200 font-mono font-bold text-lg">{mockJugador.puntos.toLocaleString()}</span>
                </div>
              </div>
            </div>

          </div>
        </main>

        {/* Indicador de carga inferior */}
        <div className="absolute bottom-10 left-0 right-0 flex justify-center z-10">
          <p className="text-white/60 text-sm tracking-widest uppercase animate-bounce">
            Preparando el tablero...
          </p>
        </div>
      </div>
    );
  }

  // ─── PANTALLA DE BÚSQUEDA NORMAL ────────────────────────────────────────────
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
