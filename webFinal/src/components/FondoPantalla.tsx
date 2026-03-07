/**
 * Componente de fondo reutilizable para pantallas de auth (login, registro).
 * Muestra la imagen de paisaje oriental con overlay azul, igual que la pantalla de inicio.
 */
export default function FondoPantalla() {
  return (
    <div
      className="absolute inset-0 bg-cover bg-center bg-no-repeat -z-10"
      style={{
        backgroundImage: `linear-gradient(to bottom, rgba(26, 45, 74, 0.7) 0%, rgba(26, 45, 74, 0.5) 40%, rgba(26, 45, 74, 0.6) 100%),
          url('https://images.unsplash.com/photo-1528360983277-13d401cdc186?w=1920')`,
      }}
    />
  );
}
