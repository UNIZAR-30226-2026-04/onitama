"use client";
// OJO: La accion principal de este overlay es no usar opacidades raras ni cosas z-index raras para tapar los fondos.
// Lo que hacemos es que el spotlight colapsa a 0x0 cuando pasa de un paso a otro y se vuelve a inflar.
// Así nos libramos de los fogonazos espantosos que pegaba la pantalla antes al saltar componentes.
// El tooltip hace un fade suave y ya, no tocar los estilos en linea que se rompen facil.
import React, { useEffect, useState, useCallback, useRef } from "react";

export interface PasoTutorial {
  targetId: string;
  titulo: string;
  descripcion: string;
  icono?: string;
  preferencia?: "arriba" | "abajo" | "izquierda" | "derecha";
  padding?: number;
  antesDeIr?: () => void;
  esperaMs?: number;
}

interface Rect { top: number; left: number; width: number; height: number; bottom: number; right: number; }
type TPos = { top?: number | string; left?: number | string; right?: number | string; bottom?: number | string; transform?: string; };

const TW = 340;
const SHADOW = "rgba(0,0,0,0.80)";
const TR = 300;

function getRect(id: string, pad: number): Rect | null {
  const el = document.getElementById(id);
  if (!el) return null;
  const r = el.getBoundingClientRect();
  return { top: r.top - pad, left: r.left - pad, width: r.width + pad * 2, height: r.height + pad * 2, bottom: r.bottom + pad, right: r.right + pad };
}

function calcPos(rect: Rect, pref: PasoTutorial["preferencia"] = "abajo"): TPos {
  const vw = window.innerWidth, vh = window.innerHeight, m = 16, est = 210;
  const cx = rect.left + rect.width / 2;
  const tries: Array<PasoTutorial["preferencia"]> = [pref, "abajo", "arriba", "derecha", "izquierda"];
  for (const l of tries) {
    if (l === "abajo" && vh - rect.bottom >= est + m) return { top: rect.bottom + 14, left: Math.max(m, Math.min(cx - TW / 2, vw - TW - m)) };
    if (l === "arriba" && rect.top >= est + m) return { bottom: vh - rect.top + 14, left: Math.max(m, Math.min(cx - TW / 2, vw - TW - m)) };
    if (l === "derecha" && vw - rect.right >= TW + m) return { top: Math.max(m, Math.min(rect.top + rect.height / 2 - est / 2, vh - est - m)), left: rect.right + 14 };
    if (l === "izquierda" && rect.left >= TW + m) return { top: Math.max(m, Math.min(rect.top + rect.height / 2 - est / 2, vh - est - m)), right: vw - rect.left + 14 };
  }
  return { top: "50%", left: "50%", transform: "translate(-50%,-50%)" };
}

interface Props { pasos: PasoTutorial[]; activo: boolean; onFinish: (completado: boolean) => void; }

export default function TutorialOverlay({ pasos, activo, onFinish }: Props) {
  const [idx, setIdx] = useState(0);
  const [rect, setRect] = useState<Rect | null>(null);
  const [tpos, setTpos] = useState<TPos>({ top: "50%", left: "50%", transform: "translate(-50%,-50%)" });
  const [expandido, setExpandido] = useState(false); // spotlight abierto
  const [tooltipVis, setTooltipVis] = useState(false);
  const navegandoRef = useRef(false);
  const rafRef = useRef<number | null>(null);

  const paso = pasos[idx];
  const esUltimo = idx === pasos.length - 1;

  // RAF: seguir el elemento con scroll
  useEffect(() => {
    if (!activo || !expandido || !paso) return;
    let last = "";
    const loop = () => {
      const r = getRect(paso.targetId, paso.padding ?? 8);
      const key = r ? `${r.top}|${r.left}|${r.width}|${r.height}` : "";
      if (key && key !== last && r) { last = key; setRect(r); setTpos(calcPos(r, paso.preferencia)); }
      rafRef.current = requestAnimationFrame(loop);
    };
    rafRef.current = requestAnimationFrame(loop);
    return () => { if (rafRef.current) cancelAnimationFrame(rafRef.current); };
  }, [activo, expandido, paso]);

  const irA = useCallback(async (nuevoIdx: number) => {
    if (nuevoIdx < 0 || nuevoIdx >= pasos.length || navegandoRef.current) return;
    navegandoRef.current = true;

    // 1. Fade out tooltip + colapsar spotlight (fondo oscuro permanece)
    setTooltipVis(false);
    setExpandido(false);
    await new Promise(r => setTimeout(r, 220)); // esperar fade y colapso

    const p = pasos[nuevoIdx];

    // 2. Ejecutar antesDeIr
    if (p.antesDeIr) {
      p.antesDeIr();
      await new Promise(r => setTimeout(r, p.esperaMs ?? 480));
    } else {
      await new Promise(r => setTimeout(r, 60));
    }

    // 3. Scroll instantáneo al elemento
    const el = document.getElementById(p.targetId);
    if (el) el.scrollIntoView({ behavior: "instant", block: "nearest" });
    await new Promise(r => setTimeout(r, 60));

    // 4. Calcular posición
    const pad = p.padding ?? 8;
    const r = getRect(p.targetId, pad);
    if (r) { setRect(r); setTpos(calcPos(r, p.preferencia)); }
    else { setRect(null); setTpos({ top: "50%", left: "50%", transform: "translate(-50%,-50%)" }); }

    setIdx(nuevoIdx);

    // 5. Expandir spotlight
    setExpandido(true);
    await new Promise(r => setTimeout(r, TR + 40));

    // 6. Mostrar tooltip
    setTooltipVis(true);
    navegandoRef.current = false;
  }, [pasos]);

  // Iniciar / resetear
  useEffect(() => {
    if (activo) {
      setExpandido(false);
      setTooltipVis(false);
      irA(0);
    } else {
      setExpandido(false);
      setTooltipVis(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activo]);

  // Teclado
  useEffect(() => {
    if (!activo) return;
    const h = (e: KeyboardEvent) => {
      if (e.key === "Escape") onFinish(false);
      if (e.key === "ArrowRight" && !esUltimo) irA(idx + 1);
      if (e.key === "ArrowLeft" && idx > 0) irA(idx - 1);
    };
    window.addEventListener("keydown", h);
    return () => window.removeEventListener("keydown", h);
  }, [activo, esUltimo, idx, irA, onFinish]);

  if (!activo || !paso) return null;

  // Spotlight: cuando expandido=false → 0×0 → fondo oscuro via box-shadow siempre presente
  const sw = expandido && rect ? rect.width : 0;
  const sh = expandido && rect ? rect.height : 0;
  const st = rect ? rect.top + (expandido ? 0 : rect.height / 2) : window.innerHeight / 2;
  const sl = rect ? rect.left + (expandido ? 0 : rect.width / 2) : window.innerWidth / 2;

  const tipStyle: React.CSSProperties = {
    position: "fixed", ...tpos, width: TW, zIndex: 9999,
    background: "linear-gradient(135deg, #1e3352 0%, #1a2d4a 100%)",
    border: "1px solid rgba(255,255,255,0.13)", borderRadius: 16,
    padding: "22px 22px 16px",
    boxShadow: "0 28px 70px rgba(0,0,0,0.7), 0 0 0 1px rgba(184,92,56,0.1)",
    transition: `opacity 200ms ease`,
    opacity: tooltipVis ? 1 : 0,
    pointerEvents: tooltipVis ? "auto" : "none",
    userSelect: "none",
  };

  return (
    <>
      {/* Click para salir */}
      <div style={{ position: "fixed", inset: 0, zIndex: 9996 }} onClick={() => onFinish(false)} aria-hidden />

      {/* Spotlight — colapsa a 0×0 entre pasos, box-shadow = fondo oscuro */}
      <div style={{
        position: "fixed", top: st, left: sl, width: sw, height: sh,
        borderRadius: expandido ? 10 : 0,
        boxShadow: `0 0 0 9999px ${SHADOW}`,
        zIndex: 9997, pointerEvents: "none",
        transition: `top ${TR}ms ease, left ${TR}ms ease, width ${TR}ms ease, height ${TR}ms ease, border-radius 200ms ease`,
      }} aria-hidden />

      {/* Tooltip */}
      <div style={tipStyle} role="dialog" aria-label={`Tutorial paso ${idx + 1} de ${pasos.length}`}>
        {/* Accent line */}
        <div style={{ position: "absolute", top: 0, left: 20, right: 20, height: 2, borderRadius: 99, background: "linear-gradient(to right, transparent, #b85c38 30%, #c9a84c 70%, transparent)" }} />

        {/* Header */}
        <div style={{ display: "flex", alignItems: "flex-start", justifyContent: "space-between", marginBottom: 10 }}>
          <div style={{ display: "flex", alignItems: "center", gap: 8, minWidth: 0 }}>
            {paso.icono && <span style={{ fontSize: 18, flexShrink: 0 }}>{paso.icono}</span>}
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: 9, fontWeight: 700, letterSpacing: ".22em", textTransform: "uppercase", color: "#b85c38", marginBottom: 2, fontFamily: "var(--font-rajdhani),sans-serif" }}>
                {idx + 1} / {pasos.length}
              </div>
              <h3 style={{ margin: 0, fontSize: 14, fontWeight: 700, color: "#f0ebe1", fontFamily: "var(--font-rajdhani),sans-serif", textTransform: "uppercase", letterSpacing: ".04em", lineHeight: 1.2 }}>
                {paso.titulo}
              </h3>
            </div>
          </div>
          <button
            onClick={() => onFinish(false)}
            style={{ background: "rgba(255,255,255,0.06)", border: "1px solid rgba(255,255,255,0.12)", borderRadius: 6, color: "rgba(255,255,255,0.4)", cursor: "pointer", fontSize: 14, padding: "3px 7px", flexShrink: 0, marginLeft: 8 }}
            aria-label="Cerrar tutorial"
          >✕</button>
        </div>

        {/* Barra progreso */}
        <div style={{ height: 2, background: "rgba(255,255,255,0.07)", borderRadius: 99, marginBottom: 12, overflow: "hidden" }}>
          <div style={{ height: "100%", width: `${((idx + 1) / pasos.length) * 100}%`, background: "linear-gradient(to right,#b85c38,#c9a84c)", borderRadius: 99, transition: `width ${TR}ms ease` }} />
        </div>

        {/* Descripción */}
        <p style={{ margin: "0 0 16px", color: "#8fa3ba", fontSize: 13, lineHeight: 1.75, fontFamily: "var(--font-geist-sans),sans-serif" }}>
          {paso.descripcion}
        </p>

        {/* Controles */}
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <button
            onClick={() => irA(idx - 1)}
            disabled={idx === 0}
            style={{ background: "transparent", border: "1px solid rgba(255,255,255,0.12)", borderRadius: 8, color: idx === 0 ? "rgba(255,255,255,0.15)" : "rgba(255,255,255,0.55)", cursor: idx === 0 ? "default" : "pointer", fontSize: 12, fontWeight: 600, padding: "7px 12px", fontFamily: "var(--font-rajdhani),sans-serif", transition: "color .2s" }}
          >← Ant.</button>

          {/* Puntos */}
          <div style={{ flex: 1, display: "flex", justifyContent: "center", gap: 4, flexWrap: "wrap" }}>
            {pasos.map((_, i) => (
              <button
                key={i} onClick={() => irA(i)}
                style={{ width: i === idx ? 18 : 5, height: 5, borderRadius: 99, background: i === idx ? "#b85c38" : i < idx ? "rgba(184,92,56,0.35)" : "rgba(255,255,255,0.15)", border: "none", cursor: "pointer", padding: 0, transition: "all .3s" }}
                aria-label={`Paso ${i + 1}`}
              />
            ))}
          </div>

          <button
            onClick={() => esUltimo ? onFinish(true) : irA(idx + 1)}
            style={{ background: "linear-gradient(135deg,#c96a3e,#b85c38)", border: "none", borderRadius: 8, color: "#f0ebe1", cursor: "pointer", fontSize: 12, fontWeight: 700, letterSpacing: ".1em", padding: "7px 15px", fontFamily: "var(--font-rajdhani),sans-serif", textTransform: "uppercase", whiteSpace: "nowrap", boxShadow: "0 4px 12px rgba(184,92,56,0.35)", transition: "transform .15s, opacity .15s" }}
            onMouseEnter={e => (e.currentTarget.style.transform = "translateY(-1px)")}
            onMouseLeave={e => (e.currentTarget.style.transform = "none")}
          >{esUltimo ? "¡Listo! →" : "Sig. →"}</button>
        </div>

        <div style={{ marginTop: 10, textAlign: "center", color: "rgba(255,255,255,0.15)", fontSize: 10, fontFamily: "var(--font-geist-sans),sans-serif" }}>
          ← → flechas · Esc para cerrar
        </div>
      </div>
    </>
  );
}
