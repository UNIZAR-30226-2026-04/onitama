"use client";

/**
 * Pantalla – Buscar Partida Pública
 * Rediseñada con estética Japandi-Tech coherente con la landing.
 */
import { useState, useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { buscarPartida, type RespuestaBuscarPartida } from "@/api/buscarpartida";
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

const DISPLAY = "var(--font-rajdhani), var(--font-geist-sans), sans-serif";

type EstadoUI = "buscando" | "error" | "presentacion";

/** Spinner hexagonal temático */
function HexSpinner() {
  return (
    <div style={{ position: "relative", width: 80, height: 80 }}>
      <svg
        style={{ position: "absolute", inset: 0, animation: "oni-hex-spin 2s linear infinite" }}
        viewBox="0 0 80 80" fill="none" aria-hidden
      >
        <style>{`@keyframes oni-hex-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
        <polygon points="40,4 72,22 72,58 40,76 8,58 8,22" stroke="#b85c38" strokeWidth="1.5" opacity="0.7"/>
      </svg>
      <svg
        style={{ position: "absolute", inset: 8, animation: "oni-hex-spin 3s linear infinite reverse" }}
        viewBox="0 0 64 64" fill="none" aria-hidden
      >
        <polygon points="32,4 58,18 58,46 32,60 6,46 6,18" stroke="#c9a84c" strokeWidth="1" opacity="0.5"/>
      </svg>
      <div style={{ position: "absolute", inset: 0, display: "flex", alignItems: "center", justifyContent: "center" }}>
        <div style={{ width: 16, height: 16, background: "#b85c38", borderRadius: "50%", opacity: 0.8, animation: "oni-glow 2.4s ease-in-out infinite" }} />
      </div>
    </div>
  );
}

export default function BuscarPartidaPage() {
  const router = useRouter();
  const [estadoUI, setEstadoUI] = useState<EstadoUI>("buscando");
  const [respuesta, setRespuesta] = useState<RespuestaBuscarPartida | null>(null);
  const cancelarBusquedaRef = useRef<(() => void) | null>(null);

  const iniciarBusqueda = () => {
    setEstadoUI("buscando");
    setRespuesta(null);
    sessionStorage.removeItem("datosPartida");
    const jugador = obtenerJugadorActivo();
    const { promise, cancel } = buscarPartida(jugador.nombre, jugador.puntos);
    cancelarBusquedaRef.current = cancel;

    promise.then((resultado) => {
      setRespuesta(resultado);
      cancelarBusquedaRef.current = null;
      if (resultado.estado === "ENCONTRADA") {
        setEstadoUI("presentacion");
        setTimeout(() => {
          const id = resultado.partida_id ?? "local";
          router.push(`/partida?id=${encodeURIComponent(id)}`);
        }, 3500);
      } else if (resultado.estado === "CANCELADO") {
        router.push("/partidas");
      } else {
        setEstadoUI("error");
      }
    });
  };

  const handleCancelarBusqueda = () => {
    cancelarBusquedaRef.current?.();
  };

  const yaIniciadoRef = useRef(false);
  useEffect(() => {
    if (yaIniciadoRef.current) return;
    yaIniciadoRef.current = true;
    iniciarBusqueda();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // ─── PANTALLA VS ──────────────────────────────────────────────────────────────
  if (estadoUI === "presentacion" && respuesta) {
    let equipoLocal: 1 | 2 = 1;
    try {
      const raw = sessionStorage.getItem("datosPartida");
      if (raw) equipoLocal = (JSON.parse(raw) as { equipo: 1 | 2 }).equipo;
    } catch { /* mantener valor por defecto */ }

    const esAzul = equipoLocal === 1;
    const jugador = obtenerJugadorActivo();
    const skinActiva = normalizarSkinId(jugador.skin_activa);

    const imgJugadorDr   = getPiezaSrc("rey", esAzul ? 1 : 2, skinActiva);
    const imgOponenteIz  = getPiezaSrc("rey", esAzul ? 2 : 1, skinActiva);
    const nombreEquipoLocal   = getEquipoNombre(skinActiva, esAzul ? 1 : 2);
    const nombreEquipoInicial = getEquipoNombre(skinActiva, 1);
    const claseEquipoLocal    = getEquipoClaseTexto(skinActiva, esAzul ? 1 : 2);
    const claseJugadorPuntos  = getEquipoClaseTexto(skinActiva, esAzul ? 1 : 2);
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
      <div translate="no" style={{ minHeight: "100vh", display: "flex", flexDirection: "column", position: "relative", overflow: "hidden", backgroundColor: "#0a1520" }}>
        {/* Fondo radial */}
        <div style={{ position: "absolute", inset: 0, background: "radial-gradient(ellipse at center, rgba(184,92,56,0.12) 0%, transparent 65%)", pointerEvents: "none" }} aria-hidden />

        <header style={{ position: "relative", zIndex: 10, padding: "24px 32px", display: "flex", justifyContent: "center" }}>
          <Image src="/nombre.png" alt="Onitama" width={220} height={66} priority style={{ height: 52, width: "auto", objectFit: "contain" }} />
        </header>

        {/* Banner equipo */}
        <div style={{ position: "relative", zIndex: 10, display: "flex", justifyContent: "center", padding: "0 16px" }}>
          <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 4, padding: "12px 32px", border: `2px solid ${colorJugador}99`, background: bannerBg, boxShadow: bannerSombra }}>
            <span style={{ fontFamily: DISPLAY, fontWeight: 700, fontSize: 18, textTransform: "uppercase", letterSpacing: "0.15em", color: fondoClaroLocal ? "#1a1108" : "#f0ebe1" }}>
              ⚔ Eres el Equipo {nombreEquipoLocal}
            </span>
            <span className={fondoClaroLocal ? "" : "text-white/60"} style={{ fontSize: 13, letterSpacing: "0.08em", color: fondoClaroLocal ? "rgba(26,17,8,0.7)" : "rgba(255,255,255,0.6)" }}>
              {esAzul ? "¡Tú comienzas la partida!" : `El equipo ${nombreEquipoInicial} comienza`}
            </span>
          </div>
        </div>

        {/* Luchadores VS */}
        <main style={{ position: "relative", zIndex: 10, flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "16px 16px 64px" }}>
          <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 32, width: "100%", maxWidth: 900 }}>
            {/* Oponente */}
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 20, flex: 1 }}>
              <div style={{ position: "relative", width: 180, height: 180, filter: `drop-shadow(0 0 22px ${glowOponente})` }}>
                <Image src={imgOponenteIz} alt="Oponente" fill style={{ objectFit: "contain" }} priority />
              </div>
              <div style={{ background: "rgba(13,26,42,0.9)", backdropFilter: "blur(12px)", padding: "12px 28px", border: `2px solid ${colorOponente}66`, boxShadow: `0 0 15px ${shadowOponente}`, display: "flex", flexDirection: "column", alignItems: "center" }}>
                <span style={{ color: "#f0ebe1", fontFamily: DISPLAY, fontWeight: 700, fontSize: 18, letterSpacing: "0.08em" }}>@{respuesta.oponente ?? "Oponente"}</span>
                <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 6 }}>
                  <Image src="/katanas.png" alt="Katanas" width={20} height={20} style={{ height: 18, width: "auto" }} />
                  <span className={claseOponentePuntos} style={{ fontFamily: "monospace", fontWeight: 700, fontSize: 16 }}>{(respuesta.oponentePt ?? 0).toLocaleString()}</span>
                </div>
              </div>
            </div>

            {/* VS */}
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", flexShrink: 0, margin: "0 -40px", zIndex: 20 }}>
              <div style={{ position: "relative", width: 160, height: 160, filter: "drop-shadow(0 0 40px rgba(255,255,255,0.3))", animation: "oni-vs-pulse 1.8s ease-in-out infinite" }}>
                <Image src="/vs.png" alt="VS" fill style={{ objectFit: "contain" }} priority />
              </div>
            </div>

            {/* Jugador */}
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 20, flex: 1 }}>
              <div style={{ position: "relative", width: 180, height: 180, filter: `drop-shadow(0 0 22px ${glowJugador})` }}>
                <Image src={imgJugadorDr} alt="Mi luchador" fill style={{ objectFit: "contain" }} priority />
              </div>
              <div style={{ background: "rgba(13,26,42,0.9)", backdropFilter: "blur(12px)", padding: "12px 28px", border: `2px solid ${colorJugador}66`, boxShadow: `0 0 15px ${shadowJugador}`, display: "flex", flexDirection: "column", alignItems: "center" }}>
                <span style={{ color: "#f0ebe1", fontFamily: DISPLAY, fontWeight: 700, fontSize: 18, letterSpacing: "0.08em" }}>@{jugador.nombre}</span>
                <div style={{ display: "flex", alignItems: "center", gap: 8, marginTop: 6 }}>
                  <Image src="/katanas.png" alt="Katanas" width={20} height={20} style={{ height: 18, width: "auto" }} />
                  <span className={claseJugadorPuntos} style={{ fontFamily: "monospace", fontWeight: 700, fontSize: 16 }}>{jugador.puntos.toLocaleString()}</span>
                </div>
              </div>
            </div>
          </div>
        </main>

        {/* Indicador inferior */}
        <div style={{ position: "absolute", bottom: 28, left: 0, right: 0, display: "flex", justifyContent: "center", zIndex: 10 }}>
          <p style={{ color: "rgba(196,181,160,0.6)", fontSize: 12, letterSpacing: "0.35em", textTransform: "uppercase", animation: "oni-scroll-pulse 2.2s ease-in-out infinite" }}>
            Preparando el tablero...
          </p>
        </div>
      </div>
    );
  }

  // ─── PANTALLA DE BÚSQUEDA ─────────────────────────────────────────────────────
  return (
    <div translate="no" style={{ minHeight: "100vh", display: "flex", flexDirection: "column", backgroundColor: "#0a1520", position: "relative", overflow: "hidden" }}>
      {/* Fondo grid */}
      <div className="oni-grid-drift" style={{ position: "fixed", inset: "-10%", width: "120%", height: "120%", pointerEvents: "none" }} aria-hidden>
        <svg style={{ width: "100%", height: "100%", opacity: 0.04 }} preserveAspectRatio="xMidYMid slice">
          <defs><pattern id="buscar-grid" width={72} height={72} patternUnits="userSpaceOnUse"><path d="M 72 0 L 0 0 0 72" fill="none" stroke="#00c8ff" strokeWidth="0.6"/></pattern></defs>
          <rect width="200%" height="200%" fill="url(#buscar-grid)"/>
        </svg>
      </div>
      <div style={{ position: "fixed", inset: 0, background: "radial-gradient(ellipse at center, rgba(184,92,56,0.08) 0%, transparent 60%)", pointerEvents: "none" }} aria-hidden />

      <header style={{ position: "relative", zIndex: 10, background: "rgba(10,21,32,0.97)", backdropFilter: "blur(12px)", borderBottom: "1px solid rgba(0,200,255,0.08)", padding: "0 28px", height: 64, display: "flex", alignItems: "center", justifyContent: "space-between", flexShrink: 0 }}>
        <div aria-hidden style={{ position: "absolute", top: 0, left: 0, right: 0, height: 2, background: "linear-gradient(to right, transparent, rgba(184,92,56,0.7) 30%, rgba(201,168,76,0.5) 60%, transparent)", pointerEvents: "none" }} />
        <Image src="/nombre.png" alt="Onitama" width={130} height={36} priority style={{ height: 34, width: "auto", objectFit: "contain" }} />
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <span style={{ fontFamily: DISPLAY, fontSize: 13, fontWeight: 700, color: "#c4b5a0", letterSpacing: "0.1em" }}>
            @{obtenerJugadorActivo().nombre}
          </span>
          <button
            type="button"
            onClick={() => router.push("/partidas")}
            style={{ fontFamily: DISPLAY, background: "transparent", border: "1px solid rgba(196,181,160,0.25)", color: "#8a9bb0", padding: "7px 18px", fontSize: 12, fontWeight: 600, letterSpacing: "0.18em", textTransform: "uppercase", cursor: "pointer", transition: "border-color 0.2s, color 0.2s" }}
            onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.borderColor = "rgba(196,181,160,0.6)"; (e.currentTarget as HTMLButtonElement).style.color = "#f0ebe1"; }}
            onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.borderColor = "rgba(196,181,160,0.25)"; (e.currentTarget as HTMLButtonElement).style.color = "#8a9bb0"; }}
          >
            ← Volver
          </button>
        </div>
      </header>

      <main style={{ flex: 1, display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", gap: 32, padding: "60px 16px", position: "relative", zIndex: 1 }}>
        <div className="glass-card oni-anim-1" style={{ width: "100%", maxWidth: 440, padding: "52px 44px", display: "flex", flexDirection: "column", alignItems: "center", gap: 28, position: "relative" }}>
          <div className="oni-copper-line" style={{ position: "absolute", top: 0, left: 0, right: 0, borderRadius: "4px 4px 0 0" }} />

          {/* Brackets decorativos */}
          <svg style={{ position: "absolute", top: 16, left: 16, opacity: 0.25 }} width="24" height="24" viewBox="0 0 24 24" aria-hidden>
            <path d="M0 24 L0 0 L24 0" fill="none" stroke="#b85c38" strokeWidth="1.2"/>
          </svg>
          <svg style={{ position: "absolute", bottom: 16, right: 16, opacity: 0.25 }} width="24" height="24" viewBox="0 0 24 24" aria-hidden>
            <path d="M24 0 L24 24 L0 24" fill="none" stroke="#b85c38" strokeWidth="1.2"/>
          </svg>

          <h1 style={{ fontFamily: DISPLAY, fontSize: 28, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.1em", textTransform: "uppercase", textAlign: "center" }}>
            Partida Pública
          </h1>

          {/* Estado: buscando */}
          {estadoUI === "buscando" && (
            <>
              <p style={{ color: "#8a9bb0", fontSize: 13, textAlign: "center", lineHeight: 1.6, letterSpacing: "0.02em" }}>
                Buscando un oponente en línea…
              </p>
              <HexSpinner />
              <p style={{ color: "rgba(138,155,176,0.5)", fontSize: 11, letterSpacing: "0.2em", textTransform: "uppercase", animation: "oni-glitch 8s ease-in-out infinite" }}>
                Esto puede tardar unos segundos…
              </p>
              <div style={{ width: "100%", height: 1, background: "rgba(196,181,160,0.08)" }} />
              <button
                type="button"
                onClick={handleCancelarBusqueda}
                className="btn-oni-danger"
                style={{ width: "100%", fontFamily: DISPLAY }}
              >
                Cancelar búsqueda
              </button>
            </>
          )}

          {/* Estado: error */}
          {estadoUI === "error" && (
            <>
              <div style={{ width: "100%", background: "rgba(239,68,68,0.08)", border: "1px solid rgba(239,68,68,0.25)", padding: "16px 20px", textAlign: "center" }}>
                <p style={{ fontFamily: DISPLAY, fontWeight: 700, fontSize: 14, color: "#f87171", letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 6 }}>Error de conexión</p>
                <p style={{ color: "rgba(248,113,113,0.8)", fontSize: 13 }}>{respuesta?.mensaje ?? "No se pudo conectar al servidor."}</p>
              </div>
              <button type="button" onClick={iniciarBusqueda} className="btn-oni-primary" style={{ width: "100%", fontFamily: DISPLAY }}>
                Reintentar
              </button>
              <button
                type="button"
                onClick={() => router.push("/partidas")}
                style={{ background: "none", border: "none", color: "rgba(138,155,176,0.6)", fontSize: 12, letterSpacing: "0.1em", cursor: "pointer", transition: "color 0.2s ease" }}
                onMouseEnter={e => { (e.currentTarget as HTMLButtonElement).style.color = "#8a9bb0"; }}
                onMouseLeave={e => { (e.currentTarget as HTMLButtonElement).style.color = "rgba(138,155,176,0.6)"; }}
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
