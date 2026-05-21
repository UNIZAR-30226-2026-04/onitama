/**
 * Fondo reutilizable con la imagen principal del juego (fondoMainPage.png),
 * el mismo que se usa en la landing page.
 * Se usa en las pantallas de auth (inicio de sesión, registro) y búsqueda de partida.
 */
export default function FondoPantalla() {
  return (
    <div
      className="fixed inset-0 bg-cover bg-center bg-no-repeat -z-10"
      style={{
        backgroundImage: `
          linear-gradient(to bottom,
            rgba(26, 45, 74, 0.75) 0%,
            rgba(26, 45, 74, 0.55) 40%,
            rgba(26, 45, 74, 0.70) 100%
          ),
          url('/fondoMainPage.png')`,
      }}
    />
  );
}
