"use client";

/**
 * Pantalla principal del jugador autenticado (prototipo 4. - Partidas.jpg).
 * Muestra el menú lateral y los tres tipos de partida disponibles.
 *
 * Datos del jugador: por ahora usamos mockJugador (sin servidor).
 * TODO: Reemplazar mockJugador por los datos reales del jugador autenticado
 *       cuando el servidor esté listo.
 *
 * Navegación:
 *  - "Partida Pública" → /buscar (busca oponente en línea)
 *  - "Partida Entrenamiento" → /partida?modo=entrenamiento (sin servidor, TODO)
 *  - "Partida Privada" → TODO
 */
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { mockJugador } from "@/lib/mockJugador";

// Tipos de partida disponibles con imagen y descripción
const TIPOS_PARTIDA = [
  {
    id: "publica",
    nombre: "Partida Pública",
    descripcion: "Lucha contra un usuario en línea.",
    imagen: "/pPublica.png",
  },
  {
    id: "entrenamiento",
    nombre: "Partida Entrenamiento",
    descripcion: "Lucha contra el robot Iron.",
    imagen: "/pEntrenamiento.png",
  },
  {
    id: "privada",
    nombre: "Partida Privada",
    descripcion: "Lucha contra tus amigos.",
    imagen: "/pPrivada.png",
  },
];

// Elementos del menú lateral izquierdo
const MENU_LATERAL = [
  { id: "cartas", label: "Mis cartas" },
  { id: "tableros", label: "Mis tableros" },
  { id: "amigos", label: "Mis amigos" },
  { id: "tienda", label: "Tienda" },
];

export default function PartidasPage() {
  const router = useRouter();

  /** Navega a la pantalla correspondiente según el tipo de partida */
  const handleIniciarPartida = (id: string) => {
    if (id === "publica") {
      router.push("/buscar");
    } else if (id === "entrenamiento") {
      // Sin servidor: inicia directamente contra la IA local
      router.push("/partida?modo=entrenamiento");
    }
    // TODO: id === "privada" → enviar SOLICITUD_PARTIDA al servidor
  };

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header del jugador autenticado: logo + estadísticas */}
      <header className="bg-[#1a2d4a] px-6 py-3 flex items-center justify-between shrink-0">
        {/* Logo - enlace a la landing */}
        <Link href="/" className="flex items-center">
          <Image
            src="/nombre.png"
            alt="Onitama"
            width={130}
            height={36}
            priority
            className="h-9 w-auto object-contain"
          />
        </Link>

        {/* Estadísticas del jugador: avatar + puntos (katanas) + cores */}
        <div className="flex items-center gap-5">
          {/* Avatar placeholder - se reemplazará por la imagen real del jugador */}
          <div className="w-11 h-11 rounded-full bg-[#2a4a6a] border-2 border-white/30 flex items-center justify-center overflow-hidden">
            <span className="text-white/50 text-xs select-none">
              {mockJugador.nombre.charAt(0).toUpperCase()}
            </span>
          </div>

          {/* Puntos / katanas */}
          <div className="flex items-center gap-2">
            <Image
              src="/katanas.png"
              alt="Puntos"
              width={22}
              height={22}
              className="h-5 w-auto"
            />
            <span className="text-white font-semibold text-sm">
              {mockJugador.puntos.toLocaleString()}
            </span>
          </div>

          {/* Cores / monedas */}
          <div className="flex items-center gap-2">
            <Image
              src="/core.png"
              alt="Cores"
              width={22}
              height={22}
              className="h-5 w-auto"
            />
            <span className="text-white font-semibold text-sm">
              {mockJugador.cores.toLocaleString()}
            </span>
          </div>
        </div>
      </header>

      {/* Cuerpo: sidebar izquierdo + área principal */}
      <div className="flex flex-1 overflow-hidden">
        {/* Menú lateral izquierdo */}
        <aside className="w-60 bg-[#7b8fa8] flex flex-col shrink-0">
          {/* Título de sección */}
          <div className="px-6 pt-6 pb-3">
            <span className="text-white font-bold text-lg tracking-wide uppercase">
              ¡A jugar!
            </span>
          </div>

          {/* Elementos de navegación - sin funcionalidad por ahora */}
          <nav className="flex flex-col mt-2">
            {MENU_LATERAL.map((item) => (
              <button
                key={item.id}
                type="button"
                className="text-white/90 text-sm font-semibold tracking-widest uppercase px-6 py-4 text-left hover:bg-white/10 transition-colors border-b border-white/10 last:border-0"
              >
                {item.label}
              </button>
            ))}
          </nav>
        </aside>

        {/* Área principal: tarjetas de tipo de partida */}
        <main className="flex-1 bg-stone-100 flex items-center justify-center px-8 py-10">
          <div className="flex flex-wrap gap-10 items-center justify-center">
            {TIPOS_PARTIDA.map((tipo) => (
              <div key={tipo.id} className="flex flex-col items-center gap-4">
                {/* Imagen circular - navega a la pantalla de partida correspondiente */}
                <button
                  type="button"
                  className="group"
                  aria-label={tipo.nombre}
                  onClick={() => handleIniciarPartida(tipo.id)}
                >
                  {/*
                   * Usamos <img> en lugar de next/image para evitar problemas
                   * con los caracteres especiales en el nombre del archivo (ej. PÚBLICA).
                   * TODO: Renombrar archivos a nombres sin espacios/tildes para usar next/image.
                   */}
                  {/* eslint-disable-next-line @next/next/no-img-element */}
                  <img
                    src={tipo.imagen}
                    alt={tipo.nombre}
                    className="w-44 h-44 rounded-full object-cover shadow-lg group-hover:scale-105 group-hover:shadow-xl transition-all duration-200"
                  />
                </button>

                {/* Etiqueta con nombre y descripción */}
                <div className="bg-stone-300 rounded-lg px-4 py-3 text-center w-52 shadow-sm">
                  <p className="text-xs font-bold text-stone-700 uppercase tracking-wide">
                    {tipo.nombre}
                  </p>
                  <p className="text-xs text-stone-600 mt-1">{tipo.descripcion}</p>
                </div>
              </div>
            ))}
          </div>
        </main>
      </div>
    </div>
  );
}
