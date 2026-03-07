/**
 * Componente Header compartido para todas las pantallas de la web.
 * Incluye logo, navegación (Sobre el juego, Sobre nosotros) y botón Iniciar sesión.
 */
import Image from "next/image";
import Link from "next/link";

export default function Header() {
  return (
    <header className="bg-[#1a2d4a] px-6 py-4 flex items-center justify-between sticky top-0 z-50">
      {/* Logo - enlace a página de inicio */}
      <Link href="/" className="flex items-center">
        <Image
          src="/nombre.png"
          alt="Onitama"
          width={140}
          height={40}
          priority
          className="h-10 w-auto object-contain"
        />
      </Link>

      {/* Enlaces de navegación - sin funcionalidad por ahora */}
      <nav className="flex items-center gap-6 md:gap-12">
        <button
          type="button"
          className="text-white uppercase text-sm font-medium tracking-wide hover:opacity-80 transition-opacity"
        >
          Sobre el juego
        </button>
        <button
          type="button"
          className="text-white uppercase text-sm font-medium tracking-wide hover:opacity-80 transition-opacity"
        >
          Sobre nosotros
        </button>
      </nav>

      {/* Botón Iniciar sesión - enlaza a la pantalla de login */}
      <Link
        href="/iniciar-sesion"
        className="bg-[#e8e8e8] text-[#2d2d2d] px-6 py-2.5 rounded-full uppercase text-sm font-medium tracking-wide hover:bg-[#d8d8d8] transition-colors"
      >
        Iniciar sesión
      </Link>
    </header>
  );
}
