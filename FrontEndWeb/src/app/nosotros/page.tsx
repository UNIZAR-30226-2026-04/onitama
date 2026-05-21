import Header from "@/components/Header";

/* ── Font shorthand ── */
const DISPLAY = "var(--font-rajdhani), var(--font-geist-sans), sans-serif";
const BODY = "var(--font-geist-sans), sans-serif";

/* ── Design tokens ── */
const C = {
  dark: "#0a1520",
  mid: "#0f1a2b",
  navy: "#131e2d",
  cream: "#f0ebe1",
  stone: "#c4b5a0",
  ink: "#1a1108",
  copper: "#b85c38",
  gold: "#c9a84c",
  steel: "#4a7fa5",
  cyber: "#00c8ff",
  muted: "#8a9bb0",
  dim: "#3a4d62",
};

/* ─────────────────────────────────────────
   Shared micro-components
───────────────────────────────────────── */
function AnimGrid({ id, spacing = 72, color = C.stone, opacity = 0.055 }: { id: string; spacing?: number; color?: string; opacity?: number }) {
  return (
    <div className="oni-grid-drift" style={{ position: "absolute", inset: "-10%", width: "120%", height: "120%", pointerEvents: "none" }}>
      <svg style={{ width: "100%", height: "100%", opacity }} preserveAspectRatio="xMidYMid slice" aria-hidden>
        <defs>
          <pattern id={id} width={spacing} height={spacing} patternUnits="userSpaceOnUse">
            <path d={`M ${spacing} 0 L 0 0 0 ${spacing}`} fill="none" stroke={color} strokeWidth="0.6" />
          </pattern>
        </defs>
        <rect width="200%" height="200%" fill={`url(#${id})`} />
      </svg>
    </div>
  );
}

function SectionLabel({ text, dark = false }: { text: string; dark?: boolean }) {
  const lineClr = dark ? "rgba(196,181,160,0.25)" : "rgba(26,17,8,0.2)";
  const txtClr = dark ? C.muted : "#5c4f42";
  return (
    <div style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 14, marginBottom: 40 }}>
      <div style={{ width: 44, height: 1, background: lineClr }} />
      <span style={{ fontFamily: BODY, color: txtClr, fontSize: 10, letterSpacing: "0.35em", textTransform: "uppercase" }}>{text}</span>
      <div style={{ width: 44, height: 1, background: lineClr }} />
    </div>
  );
}

/* PAGE */
export default function Nosotros() {
  const equipo = [
    {
      nombre: "Maha Boukil Hadifi",
      rol: "Equipo Frontend App / Diseñadora App",
      tareas: [
        "Participación en sesiones de prácticas. Propuesta técnica y económica.",
        "Participación en reuniones.",
        "Mapa de navegación, desarrollo de pantallas.",
        "Desarrollo del Frontend Móvil."
      ]
    },
    {
      nombre: "Ciro Fustero Zumeta",
      rol: "Líder de Backend",
      tareas: [
        "Participación en sesiones de prácticas. Propuesta técnica y económica.",
        "Participación en reuniones.",
        "Diseño BD.",
        "Desarrollo de los JDBC y clases asociadas.",
        "Gestión de Servidor."
      ]
    },
    {
      nombre: "Héctor Esteban Ortiz",
      rol: "Líder Equipo Frontend App / Diseñador App",
      tareas: [
        "Participación en sesiones de prácticas. Propuesta técnica y económica.",
        "Participación en reuniones.",
        "Mapa de navegación, desarrollo de pantallas.",
        "Diagrama de componentes.",
        "Desarrollo del Frontend Móvil."
      ]
    },
    {
      nombre: "Enrique José Guarás Lacasta",
      rol: "Equipo Frontend Web / Controlador de calidad web",
      tareas: [
        "Participación en sesiones de prácticas. Propuesta técnica y económica.",
        "Participación en reuniones.",
        "Diseño BD.",
        "Desarrollo de los JDBC y clases asociadas.",
        "Desarrollo del Frontend Web.",
        "Desarrollo bot juego local."
      ]
    },
    {
      nombre: "David Puértolas Merenciano",
      rol: "Director de Proyecto / Portavoz del equipo / Frontend Web / Diseñador App",
      tareas: [
        "Participación en sesiones de prácticas. Propuesta técnica y económica.",
        "Participación en reuniones.",
        "Mapa de navegación, desarrollo de pantallas.",
        "Diagrama de despliegue.",
        "Desarrollo del Frontend Web.",
        "Desarrollo bot juego local."
      ]
    },
    {
      nombre: "Ioan Sebastián Cismas",
      rol: "Frontend App",
      tareas: [
        "Participación en sesiones de prácticas. Propuesta técnica y económica.",
        "Participación en reuniones.",
        "Diagrama de clases.",
        "Desarrollo de los JDBC y clases asociadas.",
        "Desarrollo del Frontend Móvil."
      ]
    },
    {
      nombre: "Ibón Castarlenas Cortés",
      rol: "Líder de equipo Frontend Web",
      tareas: [
        "Participación en sesiones de prácticas. Propuesta técnica y económica.",
        "Participación en reuniones.",
        "Diagrama de clases.",
        "Desarrollo de los JDBC y clases asociadas.",
        "Desarrollo del Frontend Web."
      ]
    },
    {
      nombre: "Andrea Escartín López",
      rol: "Equipo de Backend / Diseñadora Web",
      tareas: [
        "Participación en sesiones de prácticas. Propuesta técnica y económica.",
        "Participación en reuniones.",
        "Mapa de navegación, desarrollo de pantallas.",
        "Gestión de Servidor."
      ]
    }
  ];

  return (
    <div style={{ backgroundColor: C.dark, minHeight: "100vh", fontFamily: BODY, position: "relative", overflowX: "hidden" }}>
      <Header />

      {/* Hero Header */}
      <section style={{ position: "relative", padding: "120px 24px 80px", textAlign: "center", overflow: "hidden" }}>
        {/* Abstract Background Elements */}
        <div style={{ position: "absolute", top: "50%", left: "50%", transform: "translate(-50%, -50%)", width: "120%", height: "100%", background: `radial-gradient(ellipse at center, rgba(184,92,56,0.15) 0%, transparent 60%)`, pointerEvents: "none" }} />
        <AnimGrid id="nosotros-grid" color={C.copper} opacity={0.06} spacing={60} />

        <div style={{ position: "relative", zIndex: 10, maxWidth: 900, margin: "0 auto" }}>
          <SectionLabel text="Sobre Nosotros" dark />
          <h1 style={{ fontFamily: DISPLAY, fontSize: "clamp(48px, 6vw, 72px)", fontWeight: 700, color: C.cream, lineHeight: 1.1, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 24 }}>
            Forjados en la <span style={{ color: C.copper }}>Universidad</span>
          </h1>

          <div style={{ background: "rgba(10, 21, 32, 0.5)", backdropFilter: "blur(8px)", padding: "32px", borderRadius: 16, border: `1px solid rgba(196, 181, 160, 0.1)`, marginTop: 32 }}>
            <p style={{ color: C.muted, fontSize: 18, lineHeight: 1.8, marginBottom: 20 }}>
              Somos un equipo de desarrollo multidisciplinar formado en la <strong>Universidad de Zaragoza</strong>. Este proyecto nace de nuestra ambición por llevar un concepto de juego de mesa tradicional al entorno digital moderno, aplicando arquitecturas robustas, comunicación en tiempo real mediante WebSockets y una estética de alta fidelidad.
            </p>
            <p style={{ color: C.muted, fontSize: 18, lineHeight: 1.8 }}>
              Nos define la dedicación por el código limpio, la innovación en la experiencia de usuario y la capacidad de resolver problemas técnicos complejos en equipo. Onitama es la prueba de nuestra habilidad para coordinar un ciclo de desarrollo completo. Actualmente seguimos en activo y estamos <strong>abiertos a colaborar en nuevos desafíos y proyectos tecnológicos</strong>.
            </p>
          </div>
        </div>
      </section>

      {/* Team Grid */}
      <section style={{ position: "relative", padding: "40px 24px 100px", zIndex: 10 }}>
        <div style={{ maxWidth: 1200, margin: "0 auto" }}>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fill, minmax(340px, 1fr))", gap: 32 }}>
            {equipo.map((miembro, idx) => (
              <div
                key={idx}
                style={{
                  background: "rgba(19, 30, 45, 0.6)",
                  backdropFilter: "blur(16px)",
                  WebkitBackdropFilter: "blur(16px)",
                  border: `1px solid rgba(201, 168, 76, 0.1)`,
                  borderRadius: 16,
                  padding: "32px 24px",
                  position: "relative",
                  overflow: "hidden",
                  transition: "transform 0.3s ease, box-shadow 0.3s ease",
                  cursor: "default",
                  display: "flex",
                  flexDirection: "column"
                }}
                className="oni-card-hover"
              >
                {/* Glow accent */}
                <div style={{ position: "absolute", top: 0, left: 0, width: "100%", height: 3, background: `linear-gradient(90deg, transparent, ${C.copper}, transparent)`, opacity: 0.8 }} />

                <h3 style={{ fontFamily: DISPLAY, fontSize: 24, fontWeight: 700, color: C.cream, letterSpacing: "0.05em", marginBottom: 8, textTransform: "uppercase" }}>
                  {miembro.nombre}
                </h3>
                <p style={{ color: C.gold, fontSize: 12, fontWeight: 600, letterSpacing: "0.15em", textTransform: "uppercase", marginBottom: 20, display: "inline-block", background: "rgba(201, 168, 76, 0.1)", padding: "4px 12px", borderRadius: 100, alignSelf: "flex-start" }}>
                  {miembro.rol}
                </p>

                <div style={{ flex: 1 }}>
                  <h4 style={{ color: C.dim, fontSize: 11, textTransform: "uppercase", letterSpacing: "0.1em", marginBottom: 12, fontWeight: "bold" }}>Misiones principales</h4>
                  <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "flex", flexDirection: "column", gap: 12 }}>
                    {miembro.tareas.map((tarea, tIdx) => (
                      <li key={tIdx} style={{ color: C.muted, fontSize: 14, lineHeight: 1.5, display: "flex", gap: 12, alignItems: "flex-start" }}>
                        <span style={{ color: C.cyber, fontSize: 12, marginTop: 2 }}>▹</span>
                        <span>{tarea}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Estilos CSS en línea para la animación hover de las tarjetas */}
      <style dangerouslySetInnerHTML={{
        __html: `
        .oni-card-hover:hover {
          transform: translateY(-8px);
          box-shadow: 0 20px 40px rgba(0,0,0,0.4), 0 0 20px rgba(184,92,56,0.1);
          border-color: rgba(201, 168, 76, 0.3) !important;
        }
      `}} />
    </div>
  );
}
