"use client";

/**
 * Pantalla de Inicio de Sesión — estética Japandi-Tech coherente con la landing.
 */
import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Header from "@/components/Header";
import { iniciarSesion } from "@/api/auth";
import { guardarSesion } from "@/lib/sesion";
import { validarContrasena } from "@/lib/validacion";
import * as WS from "@/api/ws";

const DISPLAY = "var(--font-rajdhani), var(--font-geist-sans), sans-serif";
const BODY    = "var(--font-geist-sans), sans-serif";

function AnimGrid() {
  return (
    <div className="oni-grid-drift" style={{ position: "absolute", inset: "-10%", width: "120%", height: "120%", pointerEvents: "none" }}>
      <svg style={{ width: "100%", height: "100%", opacity: 0.04 }} preserveAspectRatio="xMidYMid slice" aria-hidden>
        <defs>
          <pattern id="login-grid" width={72} height={72} patternUnits="userSpaceOnUse">
            <path d="M 72 0 L 0 0 0 72" fill="none" stroke="#00c8ff" strokeWidth="0.6" />
          </pattern>
        </defs>
        <rect width="200%" height="200%" fill="url(#login-grid)" />
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

export default function IniciarSesionPage() {
  const router = useRouter();
  const [nombre, setNombre] = useState("");
  const [contrasena, setContrasena] = useState("");
  const [errorNombre, setErrorNombre] = useState("");
  const [errorContrasena, setErrorContrasena] = useState("");
  const [errorGeneral, setErrorGeneral] = useState("");
  const [cargando, setCargando] = useState(false);
  const [mostrarContrasena, setMostrarContrasena] = useState(false);

  const [mostrarModalRecuperar, setMostrarModalRecuperar] = useState(false);
  const [correoRecuperar, setCorreoRecuperar] = useState("");
  const [estadoRecuperar, setEstadoRecuperar] = useState<
    "idle" | "enviando" | "enviado" | "error_noexiste" | "error_email" | "error_timeout"
  >("idle");

  const handleRecuperarContrasena = async (e: React.FormEvent) => {
    e.preventDefault();
    const correo = correoRecuperar.trim();
    if (!correo) return;
    setEstadoRecuperar("enviando");
    try {
      await WS.conectar();
      await new Promise<void>((resolve, reject) => {
        const limpiar = () => { unsubOk(); unsubNoExiste(); unsubEmailErr(); };
        const unsubOk       = WS.suscribir("CONTRASENA_ENVIADA",   () => { limpiar(); resolve(); });
        const unsubNoExiste = WS.suscribir("CORREO_NO_ENCONTRADO", () => { limpiar(); reject(new Error("no_existe")); });
        const unsubEmailErr = WS.suscribir("ERROR_EMAIL",          () => { limpiar(); reject(new Error("email")); });
        const enviado = WS.enviar({ tipo: "RECUPERAR_CONTRASENA", correo });
        if (!enviado) { limpiar(); reject(new Error("sin_conexion")); }
        setTimeout(() => { limpiar(); reject(new Error("timeout")); }, 20_000);
      });
      setEstadoRecuperar("enviado");
    } catch (err) {
      const msg = err instanceof Error ? err.message : "";
      if (msg === "email")        setEstadoRecuperar("error_email");
      else if (msg === "timeout") setEstadoRecuperar("error_timeout");
      else                        setEstadoRecuperar("error_noexiste");
    }
  };

  const cerrarModalRecuperar = () => {
    setMostrarModalRecuperar(false);
    setCorreoRecuperar("");
    setEstadoRecuperar("idle");
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorNombre(""); setErrorContrasena(""); setErrorGeneral("");
    const nombreVal = nombre.trim();
    if (!nombreVal) { setErrorNombre("Introduce tu nombre de usuario."); return; }
    if (!contrasena) { setErrorContrasena("Introduce tu contraseña."); return; }
    if (!validarContrasena(contrasena)) {
      setErrorContrasena("La contraseña debe tener al menos 8 caracteres, una letra y un número.");
      return;
    }
    setCargando(true);
    try {
      const datos = await iniciarSesion(nombreVal, contrasena);
      guardarSesion(datos);
      router.push("/partidas");
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Error al iniciar sesión.";
      if (msg.toLowerCase().includes("usuario"))          setErrorNombre(msg);
      else if (msg.toLowerCase().includes("contraseña") || msg.toLowerCase().includes("password")) setErrorContrasena(msg);
      else setErrorGeneral(msg);
    } finally {
      setCargando(false);
    }
  };

  return (
    <div style={{ backgroundColor: "#0a1520", minHeight: "100vh", fontFamily: BODY, position: "relative", overflow: "hidden" }}>
      {/* Fondo animado */}
      <div style={{ position: "fixed", inset: 0, overflow: "hidden", pointerEvents: "none" }} aria-hidden>
        <AnimGrid />
        <div style={{ position: "absolute", inset: 0, background: "radial-gradient(ellipse at 30% 50%, rgba(184,92,56,0.08) 0%, transparent 60%)" }} />
        <div style={{ position: "absolute", inset: 0, background: "radial-gradient(ellipse at 70% 20%, rgba(0,200,255,0.05) 0%, transparent 50%)" }} />
      </div>

      <Header />

      {/* ── Modal: recuperar contraseña ──────────────────────────────── */}
      {mostrarModalRecuperar && (
        <div style={{ position: "fixed", inset: 0, zIndex: 50, display: "flex", alignItems: "center", justifyContent: "center", background: "rgba(5,13,21,0.85)", backdropFilter: "blur(8px)", padding: "0 16px" }}>
          <div className="glass-card oni-anim-1" style={{ width: "100%", maxWidth: 420, padding: "40px 36px", position: "relative" }}>
            {/* Accent line top */}
            <div className="oni-copper-line" style={{ position: "absolute", top: 0, left: 0, right: 0, borderRadius: "4px 4px 0 0" }} />

            <h2 style={{ fontFamily: DISPLAY, fontSize: 20, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.08em", textTransform: "uppercase", textAlign: "center", marginBottom: 24 }}>
              Recuperar contraseña
            </h2>

            {estadoRecuperar === "enviado" ? (
              <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                <p style={{ color: "#4ade80", background: "rgba(74,222,128,0.1)", border: "1px solid rgba(74,222,128,0.2)", padding: "12px 16px", fontSize: 14, textAlign: "center", borderRadius: 2 }}>
                  ✅ ¡Listo! Revisa tu correo — te hemos enviado tu nueva contraseña.
                </p>
                <button type="button" onClick={cerrarModalRecuperar} className="btn-oni-primary" style={{ width: "100%" }}>
                  Cerrar
                </button>
              </div>
            ) : (
              <form onSubmit={handleRecuperarContrasena} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
                <p style={{ color: "#8a9bb0", fontSize: 13, textAlign: "center", lineHeight: 1.6 }}>
                  Introduce tu correo y te enviaremos una nueva contraseña.
                </p>
                <div>
                  <label className="label-oni">Correo electrónico</label>
                  <input
                    type="email"
                    required
                    placeholder="tu@correo.com"
                    value={correoRecuperar}
                    onChange={(e) => { setCorreoRecuperar(e.target.value); setEstadoRecuperar("idle" as "idle"); }}
                    className="input-oni"
                  />
                </div>

                {estadoRecuperar === "error_noexiste" && <p className="error-oni">No encontramos ninguna cuenta con ese correo.</p>}
                {estadoRecuperar === "error_email"    && <p className="error-oni">Cuenta encontrada pero no se pudo enviar el email. Inténtalo de nuevo.</p>}
                {estadoRecuperar === "error_timeout"  && <p className="error-oni">El servidor tardó demasiado. Comprueba tu conexión.</p>}

                <div style={{ display: "flex", gap: 10 }}>
                  <button type="button" onClick={cerrarModalRecuperar} className="btn-oni-ghost" style={{ flex: 1 }}>
                    Cancelar
                  </button>
                  <button type="submit" disabled={estadoRecuperar === "enviando"} className="btn-oni-primary" style={{ flex: 1 }}>
                    {estadoRecuperar === "enviando" ? "Enviando…" : "Enviar"}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}

      {/* ── Formulario ─────────────────────────────────────────────────── */}
      <main style={{ flex: 1, display: "flex", alignItems: "center", justifyContent: "center", padding: "60px 16px", minHeight: "calc(100vh - 64px)" }}>
        <div className="glass-card oni-anim-1" style={{ width: "100%", maxWidth: 440, padding: "52px 44px", position: "relative" }}>
          {/* Accent line top */}
          <div className="oni-copper-line" style={{ position: "absolute", top: 0, left: 0, right: 0, borderRadius: "4px 4px 0 0" }} />

          {/* Corner brackets decorativos */}
          <svg style={{ position: "absolute", top: 16, left: 16, opacity: 0.3 }} width="28" height="28" viewBox="0 0 28 28" aria-hidden>
            <path d="M0 28 L0 0 L28 0" fill="none" stroke="#b85c38" strokeWidth="1.2" />
          </svg>
          <svg style={{ position: "absolute", bottom: 16, right: 16, opacity: 0.3 }} width="28" height="28" viewBox="0 0 28 28" aria-hidden>
            <path d="M28 0 L28 28 L0 28" fill="none" stroke="#b85c38" strokeWidth="1.2" />
          </svg>

          {/* Pre-label */}
          <p style={{ fontFamily: BODY, color: "#00c8ff", fontSize: 10, letterSpacing: "0.4em", textTransform: "uppercase", textAlign: "center", marginBottom: 12, opacity: 0.8 }}>
            ◈ &nbsp;Acceso al Dojo&nbsp; ◈
          </p>

          <h1 style={{ fontFamily: DISPLAY, fontSize: 32, fontWeight: 700, color: "#f0ebe1", letterSpacing: "0.08em", textTransform: "uppercase", textAlign: "center", marginBottom: 36 }}>
            Iniciar Sesión
          </h1>

          <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: 20 }}>
            {/* Nombre de usuario */}
            <div>
              <label htmlFor="nombre" className="label-oni">Nombre de usuario</label>
              <input
                id="nombre"
                type="text"
                value={nombre}
                onChange={(e) => { setNombre(e.target.value); setErrorNombre(""); setErrorGeneral(""); }}
                placeholder="Tu nombre de usuario"
                autoComplete="username"
                className="input-oni"
              />
              {errorNombre && <p className="error-oni" style={{ marginTop: 6 }}>{errorNombre}</p>}
            </div>

            {/* Contraseña */}
            <div>
              <label htmlFor="contrasena" className="label-oni">Contraseña</label>
              <div style={{ position: "relative" }}>
                <input
                  id="contrasena"
                  type={mostrarContrasena ? "text" : "password"}
                  value={contrasena}
                  onChange={(e) => { setContrasena(e.target.value); setErrorContrasena(""); setErrorGeneral(""); }}
                  placeholder="Al menos 8 caracteres con letras y números"
                  autoComplete="current-password"
                  className="input-oni"
                  style={{ paddingRight: 44 }}
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

            {/* Error general */}
            {errorGeneral && <p className="error-oni">{errorGeneral}</p>}

            {/* Links secundarios */}
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <button
                type="button"
                onClick={() => setMostrarModalRecuperar(true)}
                style={{ background: "none", border: "none", color: "#8a9bb0", fontSize: 12, cursor: "pointer", letterSpacing: "0.02em", textDecoration: "none", padding: 0 }}
              >
                ¿Olvidaste tu contraseña?
              </button>
              <Link href="/registro" style={{ color: "#00c8ff", fontSize: 12, letterSpacing: "0.02em", textDecoration: "none", opacity: 0.85 }}>
                ¿Sin cuenta? Regístrate
              </Link>
            </div>

            {/* Separador */}
            <div style={{ height: 1, background: "rgba(196,181,160,0.1)" }} />

            {/* Submit */}
            <button type="submit" disabled={cargando} className="btn-oni-primary" style={{ width: "100%", fontFamily: DISPLAY }}>
              {cargando ? "Iniciando sesión…" : "Entrar al Dojo"}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
}
