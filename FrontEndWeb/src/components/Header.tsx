/**
 * Header compartido para todas las pantallas.
 * Diseño: Japandi-Tech — consistente con la landing page.
 */
import Image from "next/image";
import Link from "next/link";

const DISPLAY = "var(--font-rajdhani), var(--font-geist-sans), sans-serif";

export default function Header() {
  return (
    <header
      className="oni-site-header"
      style={{
        background: "rgba(10, 21, 32, 0.96)",
        backdropFilter: "blur(12px)",
        WebkitBackdropFilter: "blur(12px)",
        borderBottom: "1px solid rgba(0, 200, 255, 0.08)",
        padding: "0 32px",
        height: 64,
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        position: "sticky",
        top: 0,
        zIndex: 50,
      }}
    >
      {/* Accent line top */}
      <div
        aria-hidden
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          right: 0,
          height: 2,
          background:
            "linear-gradient(to right, transparent, rgba(184,92,56,0.7) 30%, rgba(201,168,76,0.5) 60%, transparent)",
          pointerEvents: "none",
        }}
      />

      {/* Logo */}
      <Link href="/" style={{ display: "flex", alignItems: "center", textDecoration: "none" }}>
        <Image
          className="oni-header-logo"
          src="/nombre.png"
          alt="Onitama"
          width={130}
          height={36}
          priority
          style={{ height: 34, width: "auto", objectFit: "contain" }}
        />
      </Link>

      {/* Nav */}
      <nav className="oni-header-nav" style={{ display: "flex", alignItems: "center", gap: 36 }}>
        <Link
          href="/juego"
          className="oni-nav-link"
          style={{
            fontFamily: DISPLAY,
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: "0.22em",
            textTransform: "uppercase",
            textDecoration: "none",
          }}
        >
          El juego
        </Link>
        <Link
          href="/nosotros"
          className="oni-nav-link"
          style={{
            fontFamily: DISPLAY,
            fontSize: 12,
            fontWeight: 600,
            letterSpacing: "0.22em",
            textTransform: "uppercase",
            textDecoration: "none",
          }}
        >
          Nosotros
        </Link>
      </nav>

      {/* CTA */}
      <Link
        href="/iniciar-sesion"
        className="oni-header-cta"
        style={{
          fontFamily: DISPLAY,
          background: "#b85c38",
          color: "#f0ebe1",
          padding: "9px 28px",
          fontSize: 12,
          fontWeight: 700,
          letterSpacing: "0.22em",
          textTransform: "uppercase",
          textDecoration: "none",
        }}
      >
        Iniciar sesión
      </Link>
    </header>
  );
}
