"use client";

/**
 * Pantalla principal del jugador autenticado (prototipo 4. - Partidas.jpg).
 * Muestra el menú lateral y los tres tipos de partida disponibles.
 *
 * Datos del jugador: se obtienen de la sesión (sessionStorage), que se guarda
 * al iniciar sesión con los valores que envía el servidor (puntos, cores, etc.).
 *
 * Navegación:
 *  - "Partida Pública" → /buscar (busca oponente en línea)
 *  - "Partida Entrenamiento" → modal de dificultad → /partida?modo=entrenamiento&dificultad=X
 *  - "Partida Privada" → TODO
 */
import { useState } from "react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { obtenerJugadorActivo } from "@/lib/sesion";

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

// Niveles de dificultad para el entrenamiento
const NIVELES_DIFICULTAD = [
  {
    id: "facil",
    nombre: "Principiante",
    descripcion: "IA básica. Ideal para aprender los movimientos.",
    color: "bg-[#e8e8e8] text-[#1a2d4a]",
    textColor: "text-[#1a2d4a]",
    descColor: "text-[#1a2d4a]/70",
    hover: "hover:bg-white hover:scale-[1.02]",
  },
  {
    id: "medio",
    nombre: "Guerrero",
    descripcion: "IA equilibrada. Un desafío moderado.",
    color: "bg-[#1a2d4a] text-white border-2 border-white/20",
    textColor: "text-white",
    descColor: "text-white/60",
    hover: "hover:bg-[#203a60] hover:border-white/40 hover:scale-[1.02]",
  },
  {
    id: "dificil",
    nombre: "Maestro",
    descripcion: "IA experta con visión estratégica profunda.",
    color: "bg-red-900/30 text-red-100 border-2 border-red-500/40",
    textColor: "text-red-100",
    descColor: "text-red-200/70",
    hover: "hover:bg-red-900/50 hover:border-red-400 hover:scale-[1.02] shadow-[0_0_15px_rgba(239,68,68,0.2)]",
  },
];

export default function PartidasPage() {
  const router = useRouter();
  const jugador = obtenerJugadorActivo();
  const [mostrarModalDificultad, setMostrarModalDificultad] = useState(false);

  /** Navega a la pantalla correspondiente según el tipo de partida */
  const handleIniciarPartida = (id: string) => {
    if (id === "publica") {
      router.push("/buscar");
    } else if (id === "entrenamiento") {
      setMostrarModalDificultad(true);
    }
    // TODO: id === "privada" → enviar SOLICITUD_PARTIDA al servidor
  };

  /** Inicia partida de entrenamiento con la dificultad seleccionada */
  const handleSeleccionarDificultad = (dificultad: string) => {
    setMostrarModalDificultad(false);
    router.push(`/partida?modo=entrenamiento&dificultad=${dificultad}`);
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
              {jugador.nombre.charAt(0).toUpperCase()}
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
              {jugador.puntos.toLocaleString()}
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
              {jugador.cores.toLocaleString()}
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

      {/* ═══ MODAL: SELECCIÓN DE DIFICULTAD ══════════════════════════════════ */}
      {mostrarModalDificultad && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
          <div className="w-full max-w-sm bg-[#1a2d4a]/95 backdrop-blur-md border border-white/10 rounded-2xl shadow-2xl p-8 flex flex-col items-center gap-6">

            {/* Cabecera del modal */}
            <div className="flex flex-col items-center gap-3 w-full border-b border-white/10 pb-6">
              <div className="w-16 h-16 rounded-full bg-stone-300 flex items-center justify-center shadow-[0_0_15px_rgba(255,255,255,0.2)]">
                <Image src="/pEntrenamiento.png" alt="Iron Bot" width={64} height={64} className="rounded-full object-cover" />
              </div>
              <h2 className="text-2xl font-bold text-white uppercase tracking-widest text-center mt-2">
                Entrenamiento
              </h2>
              <p className="text-white/60 text-sm text-center">
                Elige la dificultad del bot Iron
              </p>
            </div>

            {/* Botones de dificultad */}
            <div className="flex flex-col gap-4 w-full pt-2">
              {NIVELES_DIFICULTAD.map((nivel) => (
                <button
                  key={nivel.id}
                  type="button"
                  onClick={() => handleSeleccionarDificultad(nivel.id)}
                  className={`w-full flex flex-col items-center justify-center py-4 rounded-xl transition-all duration-200 cursor-pointer active:scale-100 ${nivel.color} ${nivel.hover}`}
                >
                  <span className={`font-bold text-lg uppercase tracking-widest ${nivel.textColor}`}>
                    {nivel.nombre}
                  </span>
                  <span className={`text-xs mt-1 ${nivel.descColor}`}>
                    {nivel.descripcion}
                  </span>
                </button>
              ))}
            </div>

            {/* Botón cancelar */}
            <button
              type="button"
              onClick={() => setMostrarModalDificultad(false)}
              className="text-white/50 text-sm hover:text-white transition-colors mt-2"
            >
              ← Cancelar y volver
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
