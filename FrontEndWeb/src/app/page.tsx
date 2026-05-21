import Header from "@/components/Header";
import Link from "next/link";

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

function SectionLabel({ text, dark = false }: { text: string; dark?: boolean }) {
  const lineClr = dark ? "rgba(196,181,160,0.25)" : "rgba(26,17,8,0.2)";
  const txtClr  = dark ? C.muted : "#5c4f42";
  return (
    <div className="oni-section-label" style={{ display:"flex", alignItems:"center", justifyContent:"center", gap:14, marginBottom:40 }}>
      <div style={{ width:44, height:1, background:lineClr }} />
      <span style={{ fontFamily:BODY, color:txtClr, fontSize:10, letterSpacing:"0.35em", textTransform:"uppercase" }}>{text}</span>
      <div style={{ width:44, height:1, background:lineClr }} />
    </div>
  );
}

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

function HexIcon({ color, children }: { color: string; children: React.ReactNode }) {
  return (
    <div style={{ position:"relative", width:56, height:56, margin:"0 auto 20px", display:"flex", alignItems:"center", justifyContent:"center" }}>
      <svg style={{ position:"absolute", inset:0 }} width="56" height="56" viewBox="0 0 56 56" aria-hidden>
        <polygon points="28,2 52,15 52,41 28,54 4,41 4,15" fill="none" stroke={color} strokeWidth="1.4" opacity="0.8"/>
      </svg>
      {children}
    </div>
  );
}

/* ═══════════════════════════════════════
   PAGE
═══════════════════════════════════════ */
export default function Home() {
  return (
    <div style={{ backgroundColor: C.dark, minHeight:"100vh", fontFamily: BODY }}>
      <Header />

      {/* ══════════════════════════════════════════════════════
          I. HERO
      ══════════════════════════════════════════════════════ */}
      <section
        className="oni-scanlines oni-landing-hero"
        style={{ position:"relative", minHeight:"calc(100vh - 68px)", display:"flex", flexDirection:"column", alignItems:"center", justifyContent:"center", overflow:"hidden" }}
      >
        {/* Background photo */}
        <div style={{ position:"absolute", inset:0, backgroundImage:"url('/fondoMainPage.png')", backgroundSize:"cover", backgroundPosition:"center" }} aria-hidden/>

        {/* Heavy dark veil */}
        <div style={{ position:"absolute", inset:0, background:`linear-gradient(180deg, rgba(10,21,32,0.92) 0%, rgba(10,21,32,0.65) 45%, rgba(10,21,32,0.95) 100%)` }} aria-hidden/>

        {/* Animated grid */}
        <div style={{ position:"absolute", inset:0, overflow:"hidden" }} aria-hidden>
          <AnimGrid id="hero-grid" spacing={72} color={C.cyber} opacity={0.045}/>
        </div>

        {/* Diagonal slash accent */}
        <div aria-hidden style={{
          position:"absolute", top:0, right:0, width:"45%", height:"100%",
          background:`linear-gradient(135deg, transparent 60%, rgba(184,92,56,0.06) 100%)`,
          pointerEvents:"none",
        }}/>

        {/* Corner brackets */}
        <svg style={{ position:"absolute", top:32, left:32, opacity:0.35 }} width="52" height="52" viewBox="0 0 52 52" aria-hidden>
          <path className="oni-beam-path" d="M0 52 L0 0 L52 0" fill="none" stroke={C.copper} strokeWidth="1.5"/>
        </svg>
        <svg style={{ position:"absolute", bottom:32, right:32, opacity:0.35 }} width="52" height="52" viewBox="0 0 52 52" aria-hidden>
          <path className="oni-beam-path" d="M52 0 L52 52 L0 52" fill="none" stroke={C.copper} strokeWidth="1.5"/>
        </svg>
        <svg style={{ position:"absolute", top:32, right:32, opacity:0.2 }} width="52" height="52" viewBox="0 0 52 52" aria-hidden>
          <path className="oni-beam-path" d="M0 0 L52 0 L52 52" fill="none" stroke={C.cyber} strokeWidth="1"/>
        </svg>
        <svg style={{ position:"absolute", bottom:32, left:32, opacity:0.2 }} width="52" height="52" viewBox="0 0 52 52" aria-hidden>
          <path className="oni-beam-path" d="M52 52 L0 52 L0 0" fill="none" stroke={C.cyber} strokeWidth="1"/>
        </svg>

        {/* ── Content ── */}
        <div className="oni-hero-content" style={{ position:"relative", zIndex:10, textAlign:"center", padding:"0 24px", maxWidth:900 }}>

          {/* Pre-label */}
          <p className="oni-anim-1 oni-hero-label" style={{ fontFamily:BODY, color:C.cyber, fontSize:10, letterSpacing:"0.45em", textTransform:"uppercase", marginBottom:28, opacity:0.85 }}>
            ◈ &nbsp;Duelo de Maestros&nbsp; ◈
          </p>

          {/* TITLE — Rajdhani 700 + glitch */}
          <h1
            className="oni-glitch oni-anim-2 oni-hero-title"
            style={{
              fontFamily: DISPLAY,
              fontSize: "clamp(5rem, 18vw, 12rem)",
              fontWeight: 700,
              letterSpacing: "0.12em",
              lineHeight: 0.9,
              textTransform: "uppercase",
              color: C.cream,
              marginBottom: 28,
            }}
          >
            Onitama
          </h1>

          {/* Sub-title */}
          <p className="oni-anim-3 oni-hero-subtitle" style={{ fontFamily:DISPLAY, color:C.copper, fontSize:"clamp(1rem, 2.5vw, 1.375rem)", fontWeight:600, letterSpacing:"0.18em", textTransform:"uppercase", marginBottom:20 }}>
            No hay azar. Solo disciplina, lectura y el instante preciso.
          </p>

          <p className="oni-anim-3 oni-hero-copy" style={{ fontFamily:BODY, color:C.muted, fontSize:"clamp(0.875rem, 1.5vw, 1.0625rem)", lineHeight:1.85, maxWidth:480, margin:"0 auto 52px", fontWeight:300 }}>
            Dos maestros. Un tablero. Cartas que dictan cada paso. Una sola apertura separa la victoria de la derrota.
          </p>

          {/* CTAs */}
          <div className="oni-anim-4 oni-landing-actions" style={{ display:"flex", gap:14, justifyContent:"center", flexWrap:"wrap" }}>
            <Link
              href="/iniciar-sesion"
              className="oni-glow oni-landing-button"
              style={{
                fontFamily: DISPLAY,
                padding: "16px 52px",
                background: C.copper,
                color: C.cream,
                textDecoration: "none",
                fontSize: 13,
                fontWeight: 700,
                letterSpacing: "0.28em",
                textTransform: "uppercase",
                position: "relative",
              }}
            >
              Entrar al Dojo
            </Link>
            <Link
              href="/registro"
              className="oni-landing-button"
              style={{
                fontFamily: DISPLAY,
                padding: "16px 52px",
                background: "transparent",
                color: C.cyber,
                textDecoration: "none",
                fontSize: 13,
                fontWeight: 600,
                letterSpacing: "0.28em",
                textTransform: "uppercase",
                border: `1px solid rgba(0,200,255,0.35)`,
              }}
            >
              Registrarse
            </Link>
          </div>

          {/* Stat strip */}
          <div className="oni-anim-5 oni-stat-strip" style={{ display:"flex", justifyContent:"center", gap:0, marginTop:56, borderTop:`1px solid rgba(196,181,160,0.12)`, borderLeft:`1px solid rgba(196,181,160,0.12)` }}>
            {[
              { v:"7×7", l:"Tablero" },
              { v:"5",   l:"Cartas activas" },
              { v:"2+",  l:"Jugadores" },
              { v:"∞",   l:"Estrategias" },
            ].map(({ v, l }) => (
              <div className="oni-stat-item" key={l} style={{ flex:1, padding:"20px 12px", borderRight:`1px solid rgba(196,181,160,0.12)`, borderBottom:`1px solid rgba(196,181,160,0.12)`, textAlign:"center" }}>
                <div style={{ fontFamily:DISPLAY, fontSize:"clamp(1.5rem,4vw,2.25rem)", fontWeight:700, color:C.copper, letterSpacing:"-0.02em", lineHeight:1 }}>{v}</div>
                <div style={{ fontFamily:BODY, color:C.muted, fontSize:9, letterSpacing:"0.2em", textTransform:"uppercase", marginTop:6, opacity:0.7 }}>{l}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Scroll indicator */}
        <div className="oni-scroll-indicator" style={{ position:"absolute", bottom:32, left:"50%", transform:"translateX(-50%)" }} aria-hidden>
          <div style={{ width:1, height:52, background:`linear-gradient(to bottom, transparent, ${C.copper})` }}/>
        </div>
      </section>

      {/* ══════════════════════════════════════════════════════
          II. BATTLE — los dos lados
      ══════════════════════════════════════════════════════ */}
      <section className="oni-landing-section" style={{ background:C.navy, padding:"96px 24px", position:"relative", overflow:"hidden" }}>
        <div style={{ position:"absolute", inset:0, overflow:"hidden" }} aria-hidden>
          <AnimGrid id="battle-grid" spacing={60} color={C.stone} opacity={0.04}/>
        </div>

        {/* Horizontal laser line */}
        <div aria-hidden style={{ position:"absolute", top:"50%", left:0, right:0, height:1, background:`linear-gradient(to right, transparent, ${C.copper} 30%, ${C.copper} 70%, transparent)`, opacity:0.15, pointerEvents:"none" }}/>

        <div style={{ maxWidth:1140, margin:"0 auto", position:"relative", zIndex:1 }}>
          <div style={{ textAlign:"center", marginBottom:64 }}>
            <SectionLabel text="El duelo" dark/>
            <h2 style={{ fontFamily:DISPLAY, color:C.cream, fontSize:"clamp(2rem,5vw,3.5rem)", fontWeight:700, letterSpacing:"0.04em", textTransform:"uppercase", lineHeight:1.1 }}>
              Un tablero. Dos maestros. Cero piedad.
            </h2>
          </div>

          <div className="oni-battle-grid" style={{ display:"grid", gridTemplateColumns:"1fr auto 1fr", gap:24, alignItems:"center" }}>

            {/* Player 1 */}
            <div className="oni-left-1" style={{ background:"rgba(74,127,165,0.07)", border:`1px solid rgba(74,127,165,0.25)`, padding:"40px 32px" }}>
              <div style={{ fontFamily:BODY, color:C.steel, fontSize:9, letterSpacing:"0.3em", textTransform:"uppercase", marginBottom:14, opacity:0.8 }}>Jugador 1</div>
              <div style={{ fontFamily:DISPLAY, color:C.cream, fontSize:"clamp(1.5rem,4vw,2.5rem)", fontWeight:700, letterSpacing:"0.04em", marginBottom:20, textTransform:"uppercase" }}>
                El Atacante
              </div>
              <p style={{ fontFamily:BODY, color:C.muted, fontSize:"0.9rem", lineHeight:1.85, fontWeight:300 }}>
                Mueve primero. Presiona al rival. Domina el centro del tablero y fuerza los errores. La agresión sin lectura es derrota segura.
              </p>
              <div className="oni-chip-row" style={{ marginTop:28, paddingTop:20, borderTop:`1px solid rgba(74,127,165,0.2)`, display:"flex", gap:10 }}>
                {["Presión","Apertura","Control"].map(t => (
                  <span key={t} style={{ fontFamily:BODY, color:C.steel, fontSize:9, letterSpacing:"0.2em", textTransform:"uppercase", background:"rgba(74,127,165,0.1)", padding:"5px 10px", border:`1px solid rgba(74,127,165,0.2)` }}>{t}</span>
                ))}
              </div>
            </div>

            {/* VS */}
            <div className="oni-vs-pulse oni-battle-vs" style={{ display:"flex", flexDirection:"column", alignItems:"center", gap:8 }}>
              <div aria-hidden style={{ width:1, height:64, background:`linear-gradient(to bottom, transparent, ${C.copper})` }}/>
              <div style={{ fontFamily:DISPLAY, color:C.copper, fontSize:"clamp(1.75rem,5vw,3rem)", fontWeight:700, letterSpacing:"0.1em", textShadow:`0 0 24px rgba(184,92,56,0.6)` }}>
                VS
              </div>
              <svg width="32" height="32" viewBox="0 0 32 32" aria-hidden>
                <polygon className="oni-flicker" points="16,2 30,28 2,28" fill="none" stroke={C.copper} strokeWidth="1.5" opacity="0.8"/>
                <polygon points="16,8 26,26 6,26" fill={C.copper} opacity="0.15"/>
              </svg>
              <div aria-hidden style={{ width:1, height:64, background:`linear-gradient(to top, transparent, ${C.copper})` }}/>
            </div>

            {/* Player 2 */}
            <div className="oni-right-1" style={{ background:"rgba(184,92,56,0.07)", border:`1px solid rgba(184,92,56,0.25)`, padding:"40px 32px" }}>
              <div style={{ fontFamily:BODY, color:C.copper, fontSize:9, letterSpacing:"0.3em", textTransform:"uppercase", marginBottom:14, opacity:0.8 }}>Jugador 2</div>
              <div style={{ fontFamily:DISPLAY, color:C.cream, fontSize:"clamp(1.5rem,4vw,2.5rem)", fontWeight:700, letterSpacing:"0.04em", marginBottom:20, textTransform:"uppercase" }}>
                El Maestro
              </div>
              <p style={{ fontFamily:BODY, color:C.muted, fontSize:"0.9rem", lineHeight:1.85, fontWeight:300 }}>
                Reacciona. Lee las cartas del rival. Espera la apertura y golpea con precisión quirúrgica. La paciencia es el arma más letal.
              </p>
              <div className="oni-chip-row" style={{ marginTop:28, paddingTop:20, borderTop:`1px solid rgba(184,92,56,0.2)`, display:"flex", gap:10 }}>
                {["Paciencia","Lectura","Contragolpe"].map(t => (
                  <span key={t} style={{ fontFamily:BODY, color:C.copper, fontSize:9, letterSpacing:"0.2em", textTransform:"uppercase", background:"rgba(184,92,56,0.08)", padding:"5px 10px", border:`1px solid rgba(184,92,56,0.2)` }}>{t}</span>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ══════════════════════════════════════════════════════
          III. MECÁNICAS
      ══════════════════════════════════════════════════════ */}
      <section className="oni-landing-section" style={{ background:C.dark, padding:"112px 24px", position:"relative", overflow:"hidden" }}>
        <div style={{ position:"absolute", inset:0, overflow:"hidden" }} aria-hidden>
          <AnimGrid id="mec-grid" spacing={80} color={C.cyber} opacity={0.03}/>
        </div>

        {/* Top slash decoration */}
        <div aria-hidden style={{ position:"absolute", top:0, left:0, right:0, height:3, background:`linear-gradient(to right, transparent, ${C.copper} 40%, ${C.gold} 60%, transparent)`, opacity:0.5 }}/>

        <div style={{ maxWidth:1140, margin:"0 auto", position:"relative", zIndex:1 }}>
          <div style={{ textAlign:"center", marginBottom:72 }}>
            <SectionLabel text="Mecánicas" dark/>
            <h2 style={{ fontFamily:DISPLAY, color:C.cream, fontSize:"clamp(2rem,5vw,3.5rem)", fontWeight:700, letterSpacing:"0.04em", textTransform:"uppercase", lineHeight:1.1, marginBottom:16 }}>
              El ritual del combate
            </h2>
            <p style={{ fontFamily:BODY, color:C.muted, fontSize:"0.9375rem", fontWeight:300, lineHeight:1.8, maxWidth:440, margin:"0 auto" }}>
              Tres caminos hacia la victoria. Una sola apertura define el duelo.
            </p>
          </div>

          <div className="oni-card-grid" style={{ display:"grid", gridTemplateColumns:"repeat(auto-fit, minmax(300px, 1fr))", gap:2, background:`rgba(0,200,255,0.05)` }}>
            {[
              {
                cls: "oni-scale-1",
                num: "01",
                accent: C.steel,
                title: "Cartas de Movimiento",
                body: "Cada turno seleccionas una carta de tu mano. Ese patrón dicta adónde puede moverse cualquiera de tus piezas. Al usarla la intercambias por la del centro: el tablero respira con cada jugada.",
                tags: ["Patrón","Rotación","Anticipación"],
              },
              {
                cls: "oni-scale-2",
                num: "02",
                accent: C.copper,
                title: "Cartas de Acción",
                body: "Poderes especiales que alteran el equilibrio del duelo. Revivir una pieza caída. Robar la carta del adversario. Sacrificarte para ganar terreno. Cada carta de acción es una decisión irreversible.",
                tags: ["Poder","Riesgo","Timing"],
              },
              {
                cls: "oni-scale-3",
                num: "03",
                accent: C.gold,
                title: "Victoria & Honor",
                body: "Captura al maestro rival o lleva el tuyo a su templo. Las trampas acechan bajo el tablero. El tiempo corre. Un solo instante de indecisión lo cambia todo.",
                tags: ["Maestro","Templo","Trampa"],
              },
            ].map(({ cls, num, accent, title, body, tags }) => (
              <div
                key={num}
                className={`${cls} oni-card-hover oni-info-card`}
                style={{ background:C.mid, padding:"56px 44px", position:"relative", overflow:"hidden", cursor:"default" }}
              >
                {/* Ghost number */}
                <div aria-hidden style={{ fontFamily:DISPLAY, fontSize:"7rem", fontWeight:700, color:accent, opacity:0.07, position:"absolute", top:-16, right:16, lineHeight:1, userSelect:"none" }}>{num}</div>

                {/* Accent top bar */}
                <div style={{ width:"100%", height:2, background:accent, marginBottom:36, opacity:0.7 }}/>

                <div style={{ fontFamily:DISPLAY, color:accent, fontSize:11, letterSpacing:"0.3em", textTransform:"uppercase", marginBottom:14, fontWeight:600 }}>{num}</div>

                <h3 style={{ fontFamily:DISPLAY, color:C.cream, fontSize:"1.375rem", fontWeight:700, letterSpacing:"0.03em", textTransform:"uppercase", marginBottom:18, lineHeight:1.2 }}>
                  {title}
                </h3>
                <p style={{ fontFamily:BODY, color:C.muted, fontSize:"0.9375rem", lineHeight:1.85, fontWeight:300, marginBottom:28 }}>
                  {body}
                </p>
                <div style={{ display:"flex", gap:8, flexWrap:"wrap" }}>
                  {tags.map(t => (
                    <span key={t} style={{ fontFamily:BODY, color:accent, fontSize:9, letterSpacing:"0.2em", textTransform:"uppercase", border:`1px solid ${accent}`, padding:"4px 10px", opacity:0.75 }}>{t}</span>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ══════════════════════════════════════════════════════
          IV. FEATURES — más allá del tablero
      ══════════════════════════════════════════════════════ */}
      <section className="oni-landing-section" style={{ background:C.navy, padding:"112px 24px", position:"relative", overflow:"hidden" }}>
        <div style={{ position:"absolute", inset:0, overflow:"hidden" }} aria-hidden>
          <AnimGrid id="feat-grid" spacing={64} color={C.stone} opacity={0.04}/>
        </div>

        <div style={{ maxWidth:1140, margin:"0 auto", position:"relative", zIndex:1 }}>
          <div style={{ textAlign:"center", marginBottom:72 }}>
            <SectionLabel text="Arsenal" dark/>
            <h2 style={{ fontFamily:DISPLAY, color:C.cream, fontSize:"clamp(2rem,5vw,3.5rem)", fontWeight:700, letterSpacing:"0.04em", textTransform:"uppercase", lineHeight:1.1 }}>
              Más allá del tablero
            </h2>
          </div>

          <div className="oni-card-grid" style={{ display:"grid", gridTemplateColumns:"repeat(auto-fit, minmax(240px, 1fr))", gap:2, background:"rgba(196,181,160,0.06)" }}>
            {[
              {
                cls:"oni-scale-1",
                color: C.steel,
                icon: (
                  <svg width="22" height="22" viewBox="0 0 22 22">
                    <circle cx="11" cy="11" r="9" fill="none" stroke={C.steel} strokeWidth="1.4"/>
                    <line x1="11" y1="3" x2="11" y2="19" stroke={C.steel} strokeWidth="1"/>
                    <line x1="3"  y1="11" x2="19" y2="11" stroke={C.steel} strokeWidth="1"/>
                    <circle cx="11" cy="11" r="2.5" fill={C.steel} opacity="0.6"/>
                  </svg>
                ),
                title:"Modo Online",
                body:"Desafía a jugadores reales en tiempo real. Partidas públicas o privadas a elección.",
              },
              {
                cls:"oni-scale-2",
                color: C.copper,
                icon: (
                  <svg width="22" height="22" viewBox="0 0 22 22">
                    <polygon points="11,1 21,21 1,21" fill="none" stroke={C.copper} strokeWidth="1.4"/>
                    <polygon points="11,7 18,19 4,19"  fill={C.copper} opacity="0.2"/>
                    <line x1="11" y1="7" x2="11" y2="15" stroke={C.copper} strokeWidth="1.2"/>
                    <circle cx="11" cy="17.5" r="1" fill={C.copper}/>
                  </svg>
                ),
                title:"IA Adaptativa",
                body:"Minimax con poda alfa-beta e iterative deepening. Ajusta la dificultad al nivel del maestro.",
              },
              {
                cls:"oni-scale-3",
                color: C.gold,
                icon: (
                  <svg width="22" height="22" viewBox="0 0 22 22">
                    <rect x="2" y="2" width="18" height="18" fill="none" stroke={C.gold} strokeWidth="1.4"/>
                    <rect x="6" y="6" width="10" height="10" fill="none" stroke={C.gold} strokeWidth="0.8"/>
                    <rect x="9" y="9" width="4"  height="4"  fill={C.gold} opacity="0.6"/>
                  </svg>
                ),
                title:"Skins & Avatares",
                body:"Personaliza tus piezas. Gana cores en duelo y desbloquea skins en la tienda.",
              },
              {
                cls:"oni-scale-4",
                color: C.cyber,
                icon: (
                  <svg width="22" height="22" viewBox="0 0 22 22">
                    <polygon points="11,1 20,6.5 20,15.5 11,21 2,15.5 2,6.5" fill="none" stroke={C.cyber} strokeWidth="1.4"/>
                    <circle cx="11" cy="11" r="3" fill="none" stroke={C.cyber} strokeWidth="1"/>
                    <circle cx="11" cy="11" r="1" fill={C.cyber} opacity="0.8"/>
                  </svg>
                ),
                title:"Sistema Social",
                body:"Amigos, invitaciones y notificaciones en tiempo real. Tu dojo, tu comunidad.",
              },
            ].map(({ cls, color, icon, title, body }) => (
              <div
                key={title}
                className={`${cls} oni-card-hover oni-feature-card`}
                style={{ background:C.mid, padding:"52px 36px", textAlign:"center", cursor:"default" }}
              >
                <HexIcon color={color}>{icon}</HexIcon>
                <h3 style={{ fontFamily:DISPLAY, color:C.cream, fontSize:"1.0625rem", fontWeight:700, letterSpacing:"0.1em", textTransform:"uppercase", marginBottom:14 }}>
                  {title}
                </h3>
                <p style={{ fontFamily:BODY, color:C.muted, fontSize:"0.875rem", lineHeight:1.85, fontWeight:300 }}>
                  {body}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ══════════════════════════════════════════════════════
          V. CTA FINAL
      ══════════════════════════════════════════════════════ */}
      <section className="oni-final-cta" style={{ background:C.dark, padding:"180px 24px", textAlign:"center", position:"relative", overflow:"hidden" }}>
        {/* Concentric geo shapes */}
        <svg className="oni-final-geo" style={{ position:"absolute", left:"50%", top:"50%", transform:"translate(-50%,-50%)", opacity:0.055, width:700, height:700 }} viewBox="0 0 700 700" aria-hidden>
          {[40, 110, 180, 255, 320].map(o => (
            <polygon key={o} points={`350,${o} ${700-o},350 350,${700-o} ${o},350`} fill="none" stroke={C.stone} strokeWidth="0.7"/>
          ))}
        </svg>
        {/* Top/bottom accent lines */}
        <div aria-hidden style={{ position:"absolute", top:0, left:0, right:0, height:2, background:`linear-gradient(to right, transparent, ${C.copper} 30%, ${C.gold} 60%, transparent)`, opacity:0.5 }}/>
        <div aria-hidden style={{ position:"absolute", bottom:0, left:0, right:0, height:2, background:`linear-gradient(to right, transparent, ${C.steel} 40%, ${C.cyber} 60%, transparent)`, opacity:0.35 }}/>

        <div style={{ position:"relative", zIndex:1 }}>
          <SectionLabel text="El siguiente paso" dark/>

          <h2
            style={{
              fontFamily: DISPLAY,
              color: C.cream,
              fontSize: "clamp(2.5rem, 8vw, 6rem)",
              fontWeight: 700,
              letterSpacing: "0.08em",
              textTransform: "uppercase",
              lineHeight: 1,
              marginBottom: 24,
            }}
          >
            El tablero
            <br />
            <span style={{ color: C.copper }}>te espera</span>
          </h2>

          <p style={{ fontFamily:BODY, color:C.muted, fontSize:"1rem", fontWeight:300, lineHeight:1.85, maxWidth:400, margin:"0 auto 64px" }}>
            Cada duelo es un nuevo camino. Cada derrota, una lección de maestría. El primer paso es el más difícil.
          </p>

          <div className="oni-landing-actions" style={{ display:"flex", gap:16, justifyContent:"center", flexWrap:"wrap" }}>
            <Link
              href="/iniciar-sesion"
              className="oni-glow oni-landing-button"
              style={{
                fontFamily: DISPLAY,
                display:"inline-block",
                padding:"18px 68px",
                background: C.copper,
                color: C.cream,
                textDecoration:"none",
                fontSize:14,
                fontWeight:700,
                letterSpacing:"0.3em",
                textTransform:"uppercase",
              }}
            >
              Comenzar duelo
            </Link>
            <Link
              href="/registro"
              className="oni-glow-blue oni-landing-button"
              style={{
                fontFamily: DISPLAY,
                display:"inline-block",
                padding:"18px 68px",
                background: "transparent",
                color: C.cyber,
                textDecoration:"none",
                fontSize:14,
                fontWeight:600,
                letterSpacing:"0.3em",
                textTransform:"uppercase",
                border:`1px solid rgba(0,200,255,0.3)`,
              }}
            >
              Registrarse
            </Link>
          </div>

          {/* Bottom word strip */}
          <div className="oni-word-strip" style={{ marginTop:80, display:"flex", justifyContent:"center", alignItems:"center", gap:32 }}>
            {["Maestría","◈","Honor","◈","Estrategia"].map((w, i) => (
              <span key={i} style={{ fontFamily: w==="◈" ? BODY : DISPLAY, color: w==="◈" ? C.copper : C.stone, fontSize: w==="◈" ? 10 : 11, letterSpacing:"0.3em", textTransform:"uppercase", opacity: w==="◈" ? 0.5 : 0.3, fontWeight: w==="◈" ? 400 : 600 }}>
                {w}
              </span>
            ))}
          </div>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className="oni-landing-footer" style={{ background:"#050d15", padding:"28px 40px", borderTop:`1px solid rgba(0,200,255,0.07)`, display:"flex", justifyContent:"space-between", alignItems:"center", flexWrap:"wrap", gap:12 }}>
        <span style={{ fontFamily:DISPLAY, color:C.muted, fontSize:12, letterSpacing:"0.28em", textTransform:"uppercase", fontWeight:600 }}>Onitama</span>
        <span style={{ fontFamily:BODY, color:C.dim, fontSize:11, letterSpacing:"0.12em" }}>El duelo de los maestros</span>
      </footer>
    </div>
  );
}
