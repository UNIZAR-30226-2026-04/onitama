"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { obtenerJugadorActivo } from "@/lib/sesion";
import {
  equipoFondoEsClaro,
  getEquipoBannerBg,
  getEquipoBannerSombra,
  getEquipoClaseTexto,
  getEquipoColorBase,
  getEquipoGlow,
  getEquipoNombre,
  getPiezaSrc,
  normalizarSkinId,
} from "@/lib/skins";

export default function PresentacionPartidaPage() {
  const router = useRouter();

  let equipoLocal: 1 | 2 = 1;
  let oponente = "Oponente";
  let oponentePt = 0;
  let partidaId = "local";

  try {
    const raw = sessionStorage.getItem("datosPartida");
    if (raw) {
      const datos = JSON.parse(raw) as {
        equipo?: 1 | 2;
        oponente?: string;
        oponentePt?: number;
        partida_id?: string | number;
      };
      if (datos.equipo === 1 || datos.equipo === 2) equipoLocal = datos.equipo;
      if (typeof datos.oponente === "string" && datos.oponente.trim().length > 0) oponente = datos.oponente;
      if (typeof datos.oponentePt === "number") oponentePt = datos.oponentePt;
      if (typeof datos.partida_id === "string" || typeof datos.partida_id === "number") {
        partidaId = String(datos.partida_id);
      }
    }
  } catch {
    // Si falla la lectura, dejamos los valores por defecto.
  }

  useEffect(() => {
    const t = window.setTimeout(() => {
      router.push(`/partida?id=${encodeURIComponent(partidaId)}`);
    }, 3500);
    return () => window.clearTimeout(t);
  }, [partidaId, router]);

  const esAzul = equipoLocal === 1;
  const jugador = obtenerJugadorActivo();
  const skinActiva = normalizarSkinId(jugador.skin_activa);

  const imgJugadorDr = getPiezaSrc("rey", esAzul ? 1 : 2, skinActiva);
  const imgOponenteIz = getPiezaSrc("rey", esAzul ? 2 : 1, skinActiva);
  const nombreEquipoLocal = getEquipoNombre(skinActiva, esAzul ? 1 : 2);
  const nombreEquipoInicial = getEquipoNombre(skinActiva, 1);
  const claseEquipoLocal = getEquipoClaseTexto(skinActiva, esAzul ? 1 : 2);
  const claseJugadorPuntos = getEquipoClaseTexto(skinActiva, esAzul ? 1 : 2);
  const claseOponentePuntos = getEquipoClaseTexto(skinActiva, esAzul ? 2 : 1);

  const colorJugador    = getEquipoColorBase(skinActiva, esAzul ? 1 : 2);
  const colorOponente   = getEquipoColorBase(skinActiva, esAzul ? 2 : 1);
  const fondoClaroLocal = equipoFondoEsClaro(skinActiva, esAzul ? 1 : 2);
  const glowJugador     = getEquipoGlow(skinActiva, esAzul ? 1 : 2);
  const shadowJugador   = getEquipoGlow(skinActiva, esAzul ? 1 : 2).replace("0.45", "0.2");
  const glowOponente    = getEquipoGlow(skinActiva, esAzul ? 2 : 1);
  const shadowOponente  = getEquipoGlow(skinActiva, esAzul ? 2 : 1).replace("0.45", "0.2");
  const bannerBg        = getEquipoBannerBg(skinActiva, esAzul ? 1 : 2);
  const bannerSombra    = getEquipoBannerSombra(skinActiva, esAzul ? 1 : 2);

  return (
    <div className="min-h-screen flex flex-col relative overflow-hidden bg-[#111d2c]">
      <header className="relative z-10 px-8 py-6 flex justify-center mt-2">
        <Image src="/nombre.png" alt="Onitama" width={220} height={66} priority className="h-14 w-auto object-contain" />
      </header>

      <div className="relative z-10 flex justify-center px-4">
        <div
          className="flex flex-col items-center gap-1 px-8 py-3 rounded-xl border-2 backdrop-blur-sm"
          style={{
            borderColor: colorJugador + "99",
            background: bannerBg,
            boxShadow: bannerSombra,
          }}
        >
          <span className={`font-bold text-lg uppercase tracking-widest ${fondoClaroLocal ? "text-stone-800" : claseEquipoLocal}`}>
            ⚔ Eres el Equipo {nombreEquipoLocal}
          </span>
          <span className={fondoClaroLocal ? "text-stone-700 text-sm tracking-wide" : "text-white/60 text-sm tracking-wide"}>
            {esAzul ? "¡Tu comienzas la partida!" : `El equipo ${nombreEquipoInicial} comienza`}
          </span>
        </div>
      </div>

      <main className="relative z-10 flex-1 flex items-center justify-center px-4 pb-16">
        <div className="flex items-center justify-center gap-4 sm:gap-12 w-full max-w-4xl">
          <div className="flex flex-col items-center gap-5 flex-1">
            <div className="relative w-40 h-40 sm:w-52 sm:h-52" style={{ filter: `drop-shadow(0 0 22px ${glowOponente})` }}>
              <Image src={imgOponenteIz} alt="Oponente" fill className="object-contain" priority />
            </div>
            <div
              className="bg-[#1a2d4a]/80 backdrop-blur-md px-7 py-3 rounded-xl border-2 flex flex-col items-center"
              style={{ borderColor: colorOponente + "66", boxShadow: `0 0 15px ${shadowOponente}` }}
            >
              <span className="text-white font-bold text-lg tracking-wider">@{oponente}</span>
              <div className="flex items-center gap-2 mt-1">
                <Image src="/katanas.png" alt="Katanas" width={24} height={24} className="h-5 w-auto" />
                <span className={`${claseOponentePuntos} font-mono font-bold text-base`}>{oponentePt.toLocaleString()}</span>
              </div>
            </div>
          </div>

          <div className="flex flex-col items-center shrink-0 mx-[-20px] sm:mx-[-100px] z-20">
            <div className="relative w-40 h-40 sm:w-100 sm:h-100 drop-shadow-[0_0_40px_rgba(255,255,255,0.4)] animate-pulse">
              <Image src="/vs.png" alt="VS" fill className="object-contain" priority />
            </div>
          </div>

          <div className="flex flex-col items-center gap-5 flex-1">
            <div className="relative w-40 h-40 sm:w-52 sm:h-52" style={{ filter: `drop-shadow(0 0 22px ${glowJugador})` }}>
              <Image src={imgJugadorDr} alt="Mi luchador" fill className="object-contain" priority />
            </div>
            <div
              className="bg-[#1a2d4a]/80 backdrop-blur-md px-7 py-3 rounded-xl border-2 flex flex-col items-center"
              style={{ borderColor: colorJugador + "66", boxShadow: `0 0 15px ${shadowJugador}` }}
            >
              <span className="text-white font-bold text-lg tracking-wider">@{jugador.nombre}</span>
              <div className="flex items-center gap-2 mt-1">
                <Image src="/katanas.png" alt="Katanas" width={24} height={24} className="h-5 w-auto" />
                <span className={`${claseJugadorPuntos} font-mono font-bold text-base`}>{jugador.puntos.toLocaleString()}</span>
              </div>
            </div>
          </div>
        </div>
      </main>

      <div className="absolute bottom-8 left-0 right-0 flex justify-center z-10">
        <p className="text-white/60 text-sm tracking-widest uppercase animate-bounce">Preparando el tablero...</p>
      </div>
    </div>
  );
}
