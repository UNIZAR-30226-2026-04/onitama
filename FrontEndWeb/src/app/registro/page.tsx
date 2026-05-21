"use client";

/**
 * Pantalla de Registro — estética Japandi-Tech, flujo en 3 pasos.
 */
import { useEffect, useState } from "react";
import Link from "next/link";
import Image from "next/image";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import { validarContrasena, HINT_CONTRASENA } from "@/lib/validacion";
import { registrarUsuario } from "@/api/auth";
import { guardarSesion, leerSesion } from "@/lib/sesion";

const DISPLAY = "var(--font-rajdhani), var(--font-geist-sans), sans-serif";
const BODY    = "var(--font-geist-sans), sans-serif";

type Paso = 1 | 2 | 3;
type EleccionAvatar = "sin_elegir" | string | null;
const AVATARES = Array.from({ length: 12 }, (_, i) => `avatar_${String(i + 1).padStart(2, "0")}`);

function AnimGrid() {
  return (
    <div className="oni-grid-drift" style={{ position: "absolute", inset: "-10%", width: "120%", height: "120%", pointerEvents: "none" }}>
      <svg style={{ width: "100%", height: "100%", opacity: 0.04 }} preserveAspectRatio="xMidYMid slice" aria-hidden>
        <defs>
          <pattern id="registro-grid" width={72} height={72} patternUnits="userSpaceOnUse">
            <path d="M 72 0 L 0 0 0 72" fill="none" stroke="#00c8ff" strokeWidth="0.6" />
          </pattern>
        </defs>
        <rect width="200%" height="200%" fill="url(#registro-grid)" />
      </svg>
    </div>
  );
}

function EyeIcon({ open }: { open: boolean }) {
  return open ? (
    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
      <line x1="1" y1="1" x2="23" y2="23" />
    </svg>
  ) : (
    <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

/** Indicador de progreso de 3 pasos */
function StepIndicator({ paso }: { paso: Paso }) {
  const steps = [
    { n: 1, label: "Datos" },
    { n: 2, label: "Revisión" },
    { n: 3, label: "Avatar" },
  ] as const;
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 0, marginBottom: 36 }}>
      {steps.map(({ n, label }, i) => {
        const activo   = paso === n;
        const completo = paso > n;
        return (
          <div key={n} style={{ display: "flex", alignItems: "center" }}>
            <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 6 }}>
              <div style={{
                width: 32, height: 32, borderRadius: "50%", display: "flex", alignItems: "center", justifyContent: "center",
                background: completo ? "#b85c38" : activo ? "rgba(184,92,56,0.15)" : "rgba(58,77,98,0.4)",
                border: activo ? "1px solid #b85c38" : completo ? "1px solid #b85c38" : "1px solid rgba(58,77,98,0.6)",
                transition: "all 0.3s ease",
              }}>
                {completo ? (
                  <svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M2 7L5.5 10.5L12 3.5" stroke="#f0ebe1" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
                ) : (
                  <span style={{ fontFamily: DISPLAY, fontSize: 13, fontWeight: 700, color: activo ? "#b85c38" : "#3a4d62" }}>{n}</span>
                )}
              </div>
              <span style={{ fontFamily: BODY, fontSize: 9, letterSpacing: "0.2em", textTransform: "uppercase", color: activo ? "#b85c38" : completo ? "#8a9bb0" : "#3a4d62" }}>{label}</span>
            </div>
            {i < steps.length - 1 && (
              <div style={{ width: 56, height: 1, background: completo ? "rgba(184,92,56,0.6)" : "rgba(58,77,98,0.4)", margin: "0 8px", marginBottom: 22, transition: "background 0.3s ease" }} />
            )}
          </div>
        );
      })}
    </div>
  );
}

export default function RegistroPage() {
  const router = useRouter();
  const [paso, setPaso]       = useState<Paso>(1);
  const [correo, setCorreo]   = useState("");
  const [nombre, setNombre]   = useState("");
  const [contrasena, setContrasena] = useState("");
  const [errorCorreo, setErrorCorreo]       = useState("");
  const [errorNombre, setErrorNombre]       = useState("");
  const [errorContrasena, setErrorContrasena] = useState("");
  const [errorRegistro, setErrorRegistro]   = useState("");
  const [cargando, setCargando] = useState(false);
  const [mostrarContrasena, setMostrarContrasena] = useState(false);
  const [avatarSeleccionado, setAvatarSeleccionado] = useState<EleccionAvatar>("sin_elegir");

  useEffect(() => {
    if (leerSesion()) {
      router.replace("/partidas");
    }
  }, [router]);

  // ─── Paso 1: validar datos y pasar a confirmación ─────────────────────────
  const handleContinuar = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorCorreo(""); setErrorNombre(""); setErrorContrasena("");
    const correoVal = correo.trim();
    const nombreVal = nombre.trim().replace(/^@/, "");
    if (!correoVal) { setErrorCorreo("El correo electrónico es obligatorio."); return; }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(correoVal)) { setErrorCorreo("Introduce un correo electrónico válido."); return; }
    if (!nombreVal) { setErrorNombre("El nombre de usuario es obligatorio."); return; }
    if (!validarContrasena(contrasena)) { setErrorContrasena("La contraseña debe tener al menos 8 caracteres, una letra y un número."); return; }
    setPaso(2);
  };

  const handleFinalizar = async () => {
    if (avatarSeleccionado === "sin_elegir") return;
    setErrorRegistro(""); setCargando(true);
    try {
      const datos = await registrarUsuario(correo.trim(), nombre.trim().replace(/^@/, ""), contrasena, avatarSeleccionado);
      guardarSesion(datos);
      router.push("/partidas");
    } catch (err) {
      setErrorRegistro(err instanceof Error ? err.message : "Error al registrarse.");
    } finally {
      setCargando(false);
    }
  };

  return (
    <div style={{ backgroundColor: "#0a1520", minHeight: "100vh", fontFamily: BODY, position: "relative", overflow: "hidden" }}>
      {/* Fondo animado */}
      <div style={{ position: "fixed", inset: 0, overflow: "hidden", pointerEvents: "none" }} aria-hidden>
        <AnimGrid />
        <div style={{ position: "absolute", inset: 0, background: "radial-gradient(ellipse at 70% 50%, rgba(184,92,56,0.07) 0%, transparent 60%)" }} />
        <div style={{ position: "absolute", inset: 0, background: "radial-gradient(ellipse at 20% 20%, rgba(0,200,255,0.04) 0%, transparent 50%)" }} />
      </div>

      <Header />

      <main style={{ display: "flex", alignItems: "flex-start", justifyContent: "center", padding: "52px 16px 80px", minHeight: "calc(100vh - 64px)" }}>
        <div
          className="glass-card oni-anim-1"
          style={{
            width: "100%",
            maxWidth: paso === 3 ? 760 : 460,
            padding: "52px 44px",
            position: "relative",
            transition: "max-width 0.4s cubic-bezier(0.22,1,0.36,1)",
          }}
        >
          {/* Accent line top */}
          <div className="oni-copper-line" style={{ position: "absolute", top: 0, left: 0, right: 0, borderRadius: "4px 4px 0 0" }} />

          {/* Corner brackets */}
          <svg style={{ position: "absolute", top: 16, left: 16, opacity: 0.25 }} width="24" height="24" viewBox="0 0 24 24" aria-hidden>
            <path d="M0 24 L0 0 L24 0" fill="none" stroke="#b85c38" strokeWidth="1.2" />
          </svg>
          <svg style={{ position: "absolute", bottom: 16, right: 16, opacity: 0.25 }} width="24" height="24" viewBox="0 0 24 24" aria-hidden>
            <path d="M24 0 L24 24 L0 24" fill="none" stroke="#b85c38" strokeWidth="1.2" />
          </svg>

          {/* Pre-label */}
          <p style={{ fontFamily: BODY, color: "#00c8ff", fontSize: 10, letterSpacing: "0.4em", textTransform: "uppercase", textAlign: "center", marginBottom: 12, opacity: 0.8 }}>
            ◈ &nbsp;Forja tu identidad&nbsp; ◈
          </p>

          <h1 style={{ fontFamily: DISPLAY, fontSize: 32, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.08em", textTransform: "uppercase", textAlign: "center", marginBottom: 36 }}>
            Registrarse
          </h1>

          <StepIndicator paso={paso} />

          {/* ─── Paso 1: formulario ─────────────────────────────────────── */}
          {paso === 1 && (
            <form onSubmit={handleContinuar} style={{ display: "flex", flexDirection: "column", gap: 20 }}>
              {/* Correo */}
              <div>
                <label htmlFor="correo" className="label-oni">Correo electrónico *</label>
                <input
                  id="correo" type="email" value={correo}
                  onChange={(e) => { setCorreo(e.target.value); setErrorCorreo(""); }}
                  placeholder="ejemplo@correo.com" autoComplete="email"
                  className="input-oni"
                />
                {errorCorreo && <p className="error-oni" style={{ marginTop: 6 }}>{errorCorreo}</p>}
              </div>

              {/* Nombre */}
              <div>
                <label htmlFor="nombre" className="label-oni">Nombre de usuario *</label>
                <input
                  id="nombre" type="text" value={nombre}
                  onChange={(e) => { setNombre(e.target.value); setErrorNombre(""); }}
                  placeholder="@ejemplodeusuario" autoComplete="username"
                  className="input-oni"
                />
                {errorNombre && <p className="error-oni" style={{ marginTop: 6 }}>{errorNombre}</p>}
              </div>

              {/* Contraseña */}
              <div>
                <label htmlFor="contrasena" className="label-oni">Contraseña *</label>
                <div style={{ position: "relative" }}>
                  <input
                    id="contrasena"
                    type={mostrarContrasena ? "text" : "password"}
                    value={contrasena}
                    onChange={(e) => { setContrasena(e.target.value); setErrorContrasena(""); }}
                    placeholder={HINT_CONTRASENA} autoComplete="new-password"
                    className="input-oni" style={{ paddingRight: 44 }}
                  />
                  <button
                    type="button"
                    onClick={() => setMostrarContrasena(v => !v)}
                    style={{ position: "absolute", right: 12, top: "50%", transform: "translateY(-50%)", background: "none", border: "none", color: "#8a9bb0", cursor: "pointer", padding: 4, display: "flex", alignItems: "center" }}
                    aria-label={mostrarContrasena ? "Ocultar contraseña" : "Mostrar contraseña"}
                  >
                    <EyeIcon open={mostrarContrasena} />
                  </button>
                </div>
                {errorContrasena && <p className="error-oni" style={{ marginTop: 6 }}>{errorContrasena}</p>}
              </div>

              <p style={{ textAlign: "center" }}>
                <Link href="/iniciar-sesion" style={{ color: "#00c8ff", fontSize: 12, textDecoration: "none", opacity: 0.85, letterSpacing: "0.02em" }}>
                  ¿Ya tienes cuenta? Inicia sesión
                </Link>
              </p>

              <div style={{ height: 1, background: "rgba(196,181,160,0.1)" }} />
              <button type="submit" className="btn-oni-primary" style={{ width: "100%", fontFamily: DISPLAY }}>
                Revisar datos
              </button>
            </form>
          )}

          {/* ─── Paso 2: confirmación ──────────────────────────────────── */}
          {paso === 2 && (
            <div style={{ display: "flex", flexDirection: "column", gap: 20 }}>
              <p style={{ color: "#8a9bb0", fontSize: 13, textAlign: "center", lineHeight: 1.6 }}>
                Revisa que tus datos sean correctos antes de continuar.
              </p>

              <div style={{ background: "rgba(10,21,32,0.7)", border: "1px solid rgba(196,181,160,0.1)", padding: "24px 20px", display: "flex", flexDirection: "column", gap: 14, borderRadius: 2 }}>
                {[
                  { label: "Correo", value: correo },
                  { label: "Usuario", value: `@${nombre.replace(/^@/, "")}` },
                  { label: "Contraseña", value: mostrarContrasena ? contrasena : "•".repeat(Math.min(contrasena.length, 14)) },
                ].map(({ label, value }) => (
                  <div key={label} style={{ display: "flex", justifyContent: "space-between", alignItems: "center", borderBottom: "1px solid rgba(196,181,160,0.06)", paddingBottom: 12 }}>
                    <span style={{ fontFamily: BODY, fontSize: 10, letterSpacing: "0.25em", textTransform: "uppercase", color: "#3a4d62" }}>{label}</span>
                    <span style={{ fontFamily: label === "Contraseña" ? "monospace" : BODY, color: "#c4b5a0", fontSize: 14 }}>{value}</span>
                  </div>
                ))}
                <button
                  type="button"
                  onClick={() => setMostrarContrasena(v => !v)}
                  style={{ alignSelf: "flex-end", background: "none", border: "none", color: "#8a9bb0", cursor: "pointer", fontSize: 11, letterSpacing: "0.1em", padding: 0, display: "flex", alignItems: "center", gap: 6 }}
                >
                  <EyeIcon open={mostrarContrasena} />
                  <span>{mostrarContrasena ? "Ocultar" : "Mostrar"} contraseña</span>
                </button>
              </div>

              <div style={{ display: "flex", gap: 10 }}>
                <button type="button" onClick={() => setPaso(1)} disabled={cargando} className="btn-oni-danger" style={{ flex: 1, fontFamily: DISPLAY }}>
                  Corregir
                </button>
                <button
                  type="button"
                  onClick={() => { setAvatarSeleccionado("sin_elegir"); setPaso(3); }}
                  disabled={cargando}
                  className="btn-oni-primary"
                  style={{ flex: 1, fontFamily: DISPLAY }}
                >
                  Siguiente
                </button>
              </div>
            </div>
          )}

          {/* ─── Paso 3: avatar ────────────────────────────────────────── */}
          {paso === 3 && (
            <div style={{ display: "flex", flexDirection: "column", gap: 24 }}>
              <p style={{ color: "#8a9bb0", fontSize: 13, textAlign: "center", lineHeight: 1.6 }}>
                Elige tu identidad visual. Tu avatar representará tu espíritu en el tablero.
              </p>

              <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(100px, 1fr))", gap: 14 }}>
                {AVATARES.map((avatarId) => {
                  const activo = avatarSeleccionado === avatarId;
                  return (
                    <button
                      key={avatarId}
                      type="button"
                      onClick={() => setAvatarSeleccionado(avatarId)}
                      style={{
                        background: activo ? "rgba(184,92,56,0.12)" : "rgba(10,21,32,0.5)",
                        border: activo ? "2px solid #b85c38" : "2px solid rgba(196,181,160,0.1)",
                        borderRadius: "50%",
                        padding: 4,
                        cursor: "pointer",
                        transition: "all 0.2s ease",
                        transform: activo ? "scale(1.08)" : "scale(1)",
                        boxShadow: activo ? "0 0 20px rgba(184,92,56,0.35)" : "none",
                      }}
                      title={`Elegir ${avatarId}`}
                      aria-label={`Elegir ${avatarId}`}
                    >
                      <span style={{ display: "block", width: 80, height: 80, borderRadius: "50%", overflow: "hidden" }}>
                        <Image src={`/${avatarId}.png`} alt={avatarId} width={80} height={80} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                      </span>
                    </button>
                  );
                })}
              </div>

              <div style={{ display: "flex", justifyContent: "center", paddingTop: 4, borderTop: "1px solid rgba(196,181,160,0.08)" }}>
                <button
                  type="button"
                  onClick={() => setAvatarSeleccionado(null)}
                  style={{
                    background: avatarSeleccionado === null ? "rgba(184,92,56,0.12)" : "transparent",
                    border: avatarSeleccionado === null ? "1px solid rgba(184,92,56,0.5)" : "1px solid rgba(196,181,160,0.15)",
                    color: avatarSeleccionado === null ? "#b85c38" : "#8a9bb0",
                    padding: "8px 20px",
                    fontSize: 11,
                    letterSpacing: "0.15em",
                    textTransform: "uppercase",
                    cursor: "pointer",
                    borderRadius: 2,
                    transition: "all 0.2s ease",
                    fontFamily: BODY,
                  }}
                >
                  Sin foto (usar inicial del nombre)
                </button>
              </div>

              {avatarSeleccionado === "sin_elegir" && (
                <p style={{ color: "#c9a84c", background: "rgba(201,168,76,0.08)", border: "1px solid rgba(201,168,76,0.2)", padding: "10px 14px", fontSize: 12, textAlign: "center", borderRadius: 2, letterSpacing: "0.02em" }}>
                  ⚠ Selecciona una foto o la opción sin foto para poder confirmar el registro.
                </p>
              )}

              {errorRegistro && <p className="error-oni">{errorRegistro}</p>}

              <div style={{ display: "flex", gap: 10 }}>
                <button type="button" onClick={() => setPaso(2)} disabled={cargando} className="btn-oni-danger" style={{ flex: 1, fontFamily: DISPLAY }}>
                  Atrás
                </button>
                <button
                  type="button"
                  onClick={handleFinalizar}
                  disabled={cargando || avatarSeleccionado === "sin_elegir"}
                  className="btn-oni-primary"
                  style={{ flex: 1, fontFamily: DISPLAY }}
                >
                  {cargando ? "Registrando…" : "Confirmar registro"}
                </button>
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
