/**
 * Header simplificado: solo muestra el logo Onitama sin botones de navegación.
 * Se usa en las pantallas de auth (inicio de sesión, registro) y búsqueda de partida,
 * donde no queremos distraer al usuario con la navegación principal.
 */
import Image from "next/image";
import Link from "next/link";

export default function HeaderLogo() {
  return (
    <header className="relative z-10 px-8 py-5 flex items-center justify-center">
      <Link href="/" className="flex items-center">
        <Image
          src="/nombre.png"
          alt="Onitama"
          width={160}
          height={44}
          priority
          className="h-10 w-auto object-contain"
        />
      </Link>
    </header>
  );
}
