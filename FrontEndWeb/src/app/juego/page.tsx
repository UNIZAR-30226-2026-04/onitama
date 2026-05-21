import Header from "@/components/Header";
import Image from "next/image";

/* ── Font shorthand ── */
const DISPLAY = "var(--font-rajdhani), var(--font-geist-sans), sans-serif";
const BODY    = "var(--font-geist-sans), sans-serif";

/* ── Design tokens ── */
const C = {
  dark:   "#0a1520",
  mid:    "#0f1a2b",
  navy:   "#131e2d",
  cream:  "#f0ebe1",
  stone:  "#c4b5a0",
  ink:    "#1a1108",
  copper: "#b85c38",
  gold:   "#c9a84c",
  steel:  "#4a7fa5",
  cyber:  "#00c8ff",
  muted:  "#8a9bb0",
  dim:    "#3a4d62",
};

/* ─────────────────────────────────────────
   Shared micro-components
───────────────────────────────────────── */
function AnimGrid({ id, spacing=72, color=C.stone, opacity=0.055 }: { id:string; spacing?:number; color?:string; opacity?:number }) {
  return (
    <div className="oni-grid-drift" style={{ position:"absolute", inset:"-10%", width:"120%", height:"120%", pointerEvents:"none" }}>
      <svg style={{ width:"100%", height:"100%", opacity }} preserveAspectRatio="xMidYMid slice" aria-hidden>
        <defs>
          <pattern id={id} width={spacing} height={spacing} patternUnits="userSpaceOnUse">
            <path d={`M ${spacing} 0 L 0 0 0 ${spacing}`} fill="none" stroke={color} strokeWidth="0.6"/>
          </pattern>
        </defs>
        <rect width="200%" height="200%" fill={`url(#${id})`}/>
      </svg>
    </div>
  );
}

function SectionLabel({ text, dark = false }: { text: string; dark?: boolean }) {
  const lineClr = dark ? "rgba(196,181,160,0.25)" : "rgba(26,17,8,0.2)";
  const txtClr  = dark ? C.muted : "#5c4f42";
  return (
    <div style={{ display:"flex", alignItems:"center", justifyContent:"center", gap:14, marginBottom:40 }}>
      <div style={{ width:44, height:1, background:lineClr }} />
      <span style={{ fontFamily:BODY, color:txtClr, fontSize:10, letterSpacing:"0.35em", textTransform:"uppercase" }}>{text}</span>
      <div style={{ width:44, height:1, background:lineClr }} />
    </div>
  );
}

/* ═══════════════════════════════════════
   PAGE
═══════════════════════════════════════ */
export default function ElJuego() {
  return (
    <div style={{ backgroundColor: C.dark, minHeight:"100vh", fontFamily: BODY, position: "relative", overflowX: "hidden" }}>
      <Header />

      {/* Hero Header */}
      <section style={{ position: "relative", padding: "120px 24px 100px", textAlign: "center", overflow: "hidden" }}>
        {/* Abstract Background Elements */}
        <div style={{ position: "absolute", top: "50%", left: "50%", transform: "translate(-50%, -50%)", width: "120%", height: "100%", background: `radial-gradient(ellipse at center, rgba(0, 200, 255, 0.08) 0%, transparent 70%)`, pointerEvents: "none" }} />
        <AnimGrid id="juego-grid" color={C.cyber} opacity={0.06} spacing={40} />
        
        <div style={{ position: "relative", zIndex: 10, maxWidth: 900, margin: "0 auto" }}>
          <SectionLabel text="Reglas y Novedades" dark />
          <h1 style={{ fontFamily: DISPLAY, fontSize: "clamp(48px, 6vw, 72px)", fontWeight: 700, color: C.cream, lineHeight: 1.1, textTransform: "uppercase", letterSpacing: "0.08em", marginBottom: 24 }}>
            Domina el arte de <span style={{ color: C.copper }}>Onitama</span>
          </h1>
          <p style={{ color: C.muted, fontSize: 18, lineHeight: 1.8, margin: "0 auto", maxWidth: 750 }}>
            Adéntrate en un duelo de artes marciales táctico donde la elegancia y la estrategia convergen. En nuestra versión digital remasterizada, hemos preservado la pureza milenaria del aclamado juego de mesa original, elevándolo con mecánicas destructivas y legendarias exclusivas de este proyecto.
          </p>
        </div>
      </section>

      {/* El Origen (Vanilla) */}
      <section style={{ position: "relative", padding: "80px 24px", background: "rgba(15, 26, 43, 0.4)", borderTop: `1px solid rgba(196, 181, 160, 0.05)`, borderBottom: `1px solid rgba(196, 181, 160, 0.05)` }}>
        <div style={{ maxWidth: 1000, margin: "0 auto" }}>
          <div style={{ textAlign: "center", marginBottom: 60 }}>
            <h2 style={{ fontFamily: DISPLAY, fontSize: 36, fontWeight: 700, color: C.stone, textTransform: "uppercase", letterSpacing: "0.1em" }}>La Esencia Original</h2>
            <p style={{ color: C.muted, fontSize: 16, marginTop: 16, maxWidth: 600, margin: "16px auto 0" }}>El juego de mesa clásico creado por Shimpei Sato, llevado a su máxima expresión.</p>
          </div>

          <div style={{ display: "flex", flexWrap: "wrap", gap: 60, alignItems: "center" }}>
            <div style={{ flex: "1 1 400px" }}>
              <h3 style={{ fontFamily: DISPLAY, fontSize: 28, color: C.gold, marginBottom: 20, textTransform: "uppercase" }}>El Tablero Milenario</h3>
              <p style={{ color: C.muted, fontSize: 16, lineHeight: 1.7, marginBottom: 24 }}>
                En las nebulosas montañas del antiguo Japón, el Santuario de Onitama es un lugar de iluminación. Juegas en una cuadrícula de 5x5, comandando a un Maestro y cuatro monjes discípulos. Al contrario que en el ajedrez, las piezas no tienen movimientos fijos ni valores distintos; la verdadera maestría reside en tu mente y en las cartas de postura.
              </p>
              <h4 style={{ fontFamily: DISPLAY, fontSize: 20, color: C.cream, marginBottom: 16, textTransform: "uppercase" }}>Las Dos Vías de la Victoria:</h4>
              <ul style={{ listStyle: "none", padding: 0, margin: 0, display: "flex", flexDirection: "column", gap: 16 }}>
                <li style={{ background: "rgba(0,0,0,0.2)", padding: 16, borderRadius: 8, borderLeft: `4px solid ${C.copper}` }}>
                  <span style={{ color: C.cream, fontSize: 16, fontWeight: "bold", display: "block", marginBottom: 4 }}>El Camino de la Piedra</span>
                  <span style={{ color: C.muted, fontSize: 14 }}>Demuestra tu superioridad marcial capturando al Maestro de tu oponente. Un golpe directo y definitivo.</span>
                </li>
                <li style={{ background: "rgba(0,0,0,0.2)", padding: 16, borderRadius: 8, borderLeft: `4px solid ${C.cyber}` }}>
                  <span style={{ color: C.cream, fontSize: 16, fontWeight: "bold", display: "block", marginBottom: 4 }}>El Camino del Arroyo</span>
                  <span style={{ color: C.muted, fontSize: 14 }}>Mueve a tu propio Maestro hasta el Arco del Templo enemigo, demostrando un control táctico absoluto del campo de batalla.</span>
                </li>
              </ul>
            </div>
            
            <div style={{ flex: "1 1 300px" }}>
              <div style={{ padding: 32, background: "rgba(19, 30, 45, 0.7)", backdropFilter: "blur(8px)", borderRadius: 16, border: `1px solid rgba(201, 168, 76, 0.15)`, position: "relative" }}>
                <div style={{ position: "absolute", top: -16, left: 32, background: C.dark, padding: "0 12px", color: C.gold, fontFamily: DISPLAY, fontWeight: "bold", letterSpacing: "0.1em" }}>MOVIMIENTO FLUIDO</div>
                <p style={{ color: C.muted, fontSize: 15, lineHeight: 1.7, marginBottom: 16 }}>
                  Lo que hace único a Onitama es su sistema de cartas rotativas. Hay solo 5 cartas de movimiento en juego: tú tienes dos, el oponente dos, y una quinta descansa en el centro.
                </p>
                <p style={{ color: C.cream, fontSize: 15, lineHeight: 1.7, fontStyle: "italic", borderLeft: `2px solid ${C.stone}`, paddingLeft: 16 }}>
                  "Cuando usas una postura, no la pierdes; se la entregas a tu enemigo para su próximo turno."
                </p>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* Novedades Premium */}
      <section style={{ position: "relative", padding: "100px 24px", zIndex: 10 }}>
        <div style={{ position: "absolute", inset: 0, background: "linear-gradient(to bottom, rgba(10,21,32,1) 0%, rgba(20,10,10,0.5) 50%, rgba(10,21,32,1) 100%)", zIndex: -1 }} />
        
        <div style={{ maxWidth: 1100, margin: "0 auto" }}>
          <SectionLabel text="La Expansión de la Universidad" dark />
          
          <div style={{ textAlign: "center", marginBottom: 60 }}>
            <h2 style={{ fontFamily: DISPLAY, fontSize: 36, fontWeight: 700, color: C.cream, textTransform: "uppercase", letterSpacing: "0.1em" }}>Nuestras Creaciones</h2>
            <p style={{ color: C.muted, fontSize: 16, marginTop: 16, maxWidth: 700, margin: "16px auto 0", lineHeight: 1.6 }}>
              No queríamos conformarnos con programar el juego base. Hemos inyectado el caos, la personalización y la competitividad moderna directamente en el corazón de Onitama. Estas son las adiciones exclusivas que transforman el juego de mesa en un videojuego táctico brutal.
            </p>
          </div>
          
          <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))", gap: 40 }}>
            
            {/* Poderes */}
            <div className="oni-feature-card" style={{ background: "rgba(10, 21, 32, 0.6)", backdropFilter: "blur(12px)", border: `1px solid rgba(201, 168, 76, 0.2)`, borderRadius: 16, padding: 40, display: "flex", flexDirection: "column" }}>
              <div style={{ width: 56, height: 56, background: "rgba(201, 168, 76, 0.1)", borderRadius: 16, display: "flex", alignItems: "center", justifyContent: "center", marginBottom: 24, color: C.gold, fontSize: 28, boxShadow: "0 0 20px rgba(201, 168, 76, 0.2)" }}>
                ⚡
              </div>
              <h3 style={{ fontFamily: DISPLAY, fontSize: 26, color: C.cream, marginBottom: 16, textTransform: "uppercase", letterSpacing: "0.05em" }}>Cartas de Acción Místicas</h3>
              <p style={{ color: C.muted, fontSize: 15, lineHeight: 1.7, flex: 1 }}>
                Totalmente inventadas por nuestro equipo. Ahora puedes sacrificar tu movimiento físico para invocar habilidades devastadoras. 
              </p>
              <ul style={{ marginTop: 20, listStyle: "none", padding: 0, display: "flex", flexDirection: "column", gap: 10 }}>
                <li style={{ color: C.stone, fontSize: 14 }}><strong>Cegar:</strong> Oculta temporalmente los movimientos del oponente.</li>
                <li style={{ color: C.stone, fontSize: 14 }}><strong>Robar:</strong> Quédate con la carta extra que iba a recibir el enemigo.</li>
                <li style={{ color: C.stone, fontSize: 14 }}><strong>Santo Grial:</strong> Revive a uno de tus discípulos caídos en batalla.</li>
                <li style={{ color: C.muted, fontSize: 13, marginTop: 8, fontStyle: "italic" }}>* Estas son solo algunas de las habilidades de ejemplo. ¡Descubre muchas más jugando!</li>
              </ul>
            </div>

            {/* Trampas */}
            <div className="oni-feature-card" style={{ background: "rgba(10, 21, 32, 0.6)", backdropFilter: "blur(12px)", border: `1px solid rgba(184, 92, 56, 0.2)`, borderRadius: 16, padding: 40, display: "flex", flexDirection: "column" }}>
              <div style={{ width: 56, height: 56, background: "rgba(184, 92, 56, 0.1)", borderRadius: 16, display: "flex", alignItems: "center", justifyContent: "center", marginBottom: 24, color: C.copper, fontSize: 28, boxShadow: "0 0 20px rgba(184, 92, 56, 0.2)" }}>
                ☠
              </div>
              <h3 style={{ fontFamily: DISPLAY, fontSize: 26, color: C.cream, marginBottom: 16, textTransform: "uppercase", letterSpacing: "0.05em" }}>Sistemas de Trampas</h3>
              <p style={{ color: C.muted, fontSize: 15, lineHeight: 1.7, flex: 1 }}>
                ¿El tablero te parecía demasiado seguro? En esta versión, cada jugador despliega de forma oculta una trampa letal en su mitad del tablero antes del primer movimiento. 
              </p>
              <p style={{ color: C.stone, fontSize: 14, marginTop: 16, padding: 12, background: "rgba(0,0,0,0.3)", borderRadius: 8 }}>
                <strong>Táctica psicológica:</strong> El oponente no ve tu trampa hasta que la pisa. Cualquier pieza que caiga en ella es eliminada al instante. ¡Usa tus propios discípulos como cebo!
              </p>
            </div>

            {/* Economía */}
            <div className="oni-feature-card" style={{ background: "rgba(10, 21, 32, 0.6)", backdropFilter: "blur(12px)", border: `1px solid rgba(0, 200, 255, 0.2)`, borderRadius: 16, padding: 40, display: "flex", flexDirection: "column" }}>
              <div style={{ width: 56, height: 56, background: "rgba(0, 200, 255, 0.1)", borderRadius: 16, display: "flex", alignItems: "center", justifyContent: "center", marginBottom: 24, color: C.cyber, fontSize: 28, boxShadow: "0 0 20px rgba(0, 200, 255, 0.2)" }}>
                💎
              </div>
              <h3 style={{ fontFamily: DISPLAY, fontSize: 26, color: C.cream, marginBottom: 16, textTransform: "uppercase", letterSpacing: "0.05em" }}>Metajuego y Economía</h3>
              <p style={{ color: C.muted, fontSize: 15, lineHeight: 1.7, flex: 1 }}>
                Le hemos dado una capa de progresión para mantener la adicción a largo plazo. Al jugar partidas públicas compites en el ranking global.
              </p>
              <ul style={{ marginTop: 20, listStyle: "none", padding: 0, display: "flex", flexDirection: "column", gap: 10 }}>
                <li style={{ color: C.stone, fontSize: 14 }}><strong>Katanas:</strong> Tus puntos MMR. Gana para subir, pierde y caerás en desgracia.</li>
                <li style={{ color: C.stone, fontSize: 14 }}><strong>Cores:</strong> La moneda premium del juego. Úsala en nuestra Tienda integrada para comprar Skins estéticas de alta fidelidad.</li>
              </ul>
            </div>
            
          </div>
        </div>
      </section>

      {/* Call to Action Final */}
      <section style={{ padding: "100px 24px", textAlign: "center", background: `linear-gradient(180deg, transparent, rgba(184, 92, 56, 0.05))` }}>
        <h2 style={{ fontFamily: DISPLAY, fontSize: 40, color: C.cream, marginBottom: 24, textTransform: "uppercase" }}>
          ¿Estás listo para el duelo?
        </h2>
        <p style={{ color: C.muted, fontSize: 18, marginBottom: 40, maxWidth: 600, margin: "0 auto 40px" }}>
          Crea tu cuenta, configura tus trampas y demuestra tu valor en la arena de Onitama.
        </p>
        <a href="/iniciar-sesion" style={{ display: "inline-block", background: C.copper, color: C.cream, padding: "16px 48px", fontSize: 16, fontWeight: 700, letterSpacing: "0.2em", textTransform: "uppercase", textDecoration: "none", borderRadius: 4, transition: "transform 0.3s, background 0.3s", boxShadow: "0 10px 30px rgba(184,92,56,0.3)" }} className="hover:-translate-y-1 hover:bg-[#a04e2e]">
          Forja tu leyenda
        </a>
      </section>

      <style dangerouslySetInnerHTML={{__html: `
        .oni-feature-card {
          transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275), border-color 0.4s ease;
        }
        .oni-feature-card:hover {
          transform: translateY(-10px);
          border-color: ${C.gold} !important;
        }
      `}} />
    </div>
  );
}
