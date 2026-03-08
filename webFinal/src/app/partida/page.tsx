"use client";

/**
 * Pantalla de Partida – Onitama 7×7 (versión básica sin cartas de acción).
 * Accesible desde /buscar (Partida Pública) y desde /partidas (Entrenamiento).
 *
 * ─── Diseño sin scroll ────────────────────────────────────────────────────────
 * El layout ocupa exactamente h-screen y todo el contenido cabe en una sola vista.
 * El tablero se escala mediante CSS min() para adaptarse a cualquier pantalla.
 *
 * ─── 7 cartas (2+2+3) ────────────────────────────────────────────────────────
 * Cada jugador tiene 2 cartas en mano. La cola tiene 3 cartas.
 * Al jugar una carta: el jugador recibe la primera de la cola (índice 0)
 * y la carta usada pasa al final de la cola (índice 2).
 *
 * ─── Flujo de interacción (turno del jugador) ─────────────────────────────────
 *  1. Clic en una ficha propia  → highlight amarillo (fichaSeleccionada)
 *  2. Clic en una carta         → highlight azul en destinos válidos
 *  3. Clic en casilla azul      → ejecutar movimiento, cambiar turno
 *
 * ─── IA local (equipo 1, oponente) ────────────────────────────────────────────
 * Tras 900 ms ejecuta un movimiento aleatorio válido.
 * TODO: reemplazar por recepción de mensaje WS tipo MOVER (ver api/partida.ts).
 */

import { useState, useEffect, useCallback, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import {
  crearEstadoInicial,
  calcularMovimientosValidos,
  ejecutarMovimiento,
  type EstadoJuego,
  type EquipoID,
  DIM,
} from "@/lib/juego";
import { type CartaMovDef } from "@/lib/cartas";
import { mockJugador } from "@/lib/mockJugador";

// ─── Constantes ───────────────────────────────────────────────────────────────

const TIEMPO_TURNO = 120;
const MOCK_OPONENTE = { nombre: "granluchador" };

// ─── Mini cuadrícula de la carta ─────────────────────────────────────────────

function MiniGrid({ carta, equipo }: { carta: CartaMovDef; equipo: EquipoID }) {
  const CENTRO = 3;
  const signo = equipo === 2 ? 1 : -1;
  const activas = new Set<string>();
  for (const { dc, df } of carta.movimientos) {
    const gf = CENTRO - df * signo;
    const gc = CENTRO + dc * signo;
    if (gf >= 0 && gf < DIM && gc >= 0 && gc < DIM) activas.add(`${gf},${gc}`);
  }
  return (
    <div
      className="grid gap-[1px] shrink-0"
      style={{ gridTemplateColumns: `repeat(${DIM}, 1fr)` }}
      aria-hidden
    >
      {Array.from({ length: DIM }, (_, f) =>
        Array.from({ length: DIM }, (_, c) => {
          const esC = f === CENTRO && c === CENTRO;
          const esA = activas.has(`${f},${c}`);
          return (
            <div
              key={`${f}-${c}`}
              className={`w-[5px] h-[5px] rounded-[1px] ${
                esC ? "bg-gray-300" : esA ? "bg-blue-400" : "bg-gray-700"
              }`}
            />
          );
        })
      )}
    </div>
  );
}

// ─── Tarjeta de movimiento ────────────────────────────────────────────────────

function CartaBtn({
  carta,
  equipo,
  seleccionada = false,
  onClick,
  desactivada = false,
}: {
  carta: CartaMovDef;
  equipo: EquipoID;
  seleccionada?: boolean;
  onClick?: () => void;
  desactivada?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={desactivada}
      title={carta.nombre}
      className={`flex items-center gap-2 rounded-lg px-2 py-1.5 border-2 transition-all duration-150 text-xs w-full ${
        seleccionada
          ? "border-blue-400 bg-blue-900/60 text-blue-100 scale-[1.03]"
          : desactivada
          ? "border-white/10 bg-white/5 text-white/40 cursor-default"
          : "border-white/20 bg-white/10 text-white hover:bg-white/20 hover:border-white/40 cursor-pointer"
      }`}
    >
      <span className="text-lg shrink-0">{carta.emoji}</span>
      <div className="flex flex-col gap-0.5 min-w-0">
        <span className="font-bold uppercase tracking-wide leading-none text-[10px] truncate">
          {carta.nombre}
        </span>
        <MiniGrid carta={carta} equipo={equipo} />
      </div>
    </button>
  );
}

// ─── Carta pequeña (para la cola) ────────────────────────────────────────────

function CartaCola({
  carta,
  equipo,
  esLaSiguiente,
}: {
  carta: CartaMovDef;
  equipo: EquipoID;
  esLaSiguiente: boolean;
}) {
  return (
    <div
      className={`flex flex-col items-center gap-1 rounded-lg px-2 py-1.5 border transition-all ${
        esLaSiguiente
          ? "border-yellow-400/60 bg-yellow-900/20 text-yellow-200"
          : "border-white/10 bg-white/5 text-white/40"
      }`}
      title={carta.nombre}
    >
      <span className="text-base">{carta.emoji}</span>
      <MiniGrid carta={carta} equipo={equipo} />
      <span className="text-[9px] font-bold uppercase tracking-wide truncate w-full text-center">
        {esLaSiguiente ? "Siguiente" : carta.nombre}
      </span>
    </div>
  );
}

// ─── Celda del tablero ────────────────────────────────────────────────────────

function Celda({
  ficha,
  esTrono,
  esSeleccionada,
  esMovimientoValido,
  esUltimoMov,
  onClick,
}: {
  ficha: { equipo: EquipoID; esRey: boolean } | null;
  esTrono: boolean;
  esSeleccionada: boolean;
  esMovimientoValido: boolean;
  esUltimoMov: boolean;
  onClick: () => void;
}) {
  let bg = "bg-[#2c3a4a] hover:bg-[#354657]";
  if (esSeleccionada) bg = "bg-yellow-500";
  else if (esMovimientoValido) bg = "bg-white cursor-pointer";
  else if (esUltimoMov) bg = "bg-yellow-900/50";

  return (
    <button
      type="button"
      onClick={onClick}
      className={`aspect-square flex items-center justify-center relative border border-[#1a2a3a] transition-colors duration-100 ${bg}`}
    >
      {/* Trono vacío */}
      {esTrono && !ficha && (
        <span className="text-white/20 text-[10px] select-none">🏯</span>
      )}
      {/* Pieza */}
      {ficha && (
        <div
          className={`w-[70%] h-[70%] rounded-full flex items-center justify-center shadow-md ${
            ficha.equipo === 1
              ? "bg-red-700 border-2 border-red-400"
              : "bg-blue-700 border-2 border-blue-400"
          }`}
        >
          {ficha.esRey && (
            <span className="text-white leading-none select-none" style={{ fontSize: "40%" }}>
              🏯
            </span>
          )}
        </div>
      )}
    </button>
  );
}

// ─── Lógica principal ─────────────────────────────────────────────────────────

function PartidaInterna({ partidaId }: { partidaId: string }) {
  const router = useRouter();
  const [estado, setEstado] = useState<EstadoJuego>(() => crearEstadoInicial());
  const [tiempoRestante, setTiempoRestante] = useState(TIEMPO_TURNO);
  const iaOcupada = useRef(false);

  // ─── Timer ──────────────────────────────────────────────────────────────
  useEffect(() => {
    if (estado.ganador) return;
    setTiempoRestante(TIEMPO_TURNO);
    const intervalo = setInterval(() => {
      setTiempoRestante((t) => {
        if (t <= 1) {
          clearInterval(intervalo);
          // Tiempo agotado → pasar turno sin mover
          setEstado((prev) => ({
            ...prev,
            turnoActual: prev.turnoActual === 2 ? 1 : 2,
            fichaSeleccionada: null,
            cartaSeleccionada: null,
            movimientosValidos: [],
          }));
          return TIEMPO_TURNO;
        }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(intervalo);
  }, [estado.turnoActual, estado.ganador]);

  // ─── IA del oponente (equipo 1) ──────────────────────────────────────────
  // TODO: eliminar y sustituir por recepción de WS mensaje MOVER (api/partida.ts)
  const ejecutarIa = useCallback((est: EstadoJuego) => {
    if (iaOcupada.current) return;
    iaOcupada.current = true;
    setTimeout(() => {
      const jugadas: {
        fila: number;
        col: number;
        carta: CartaMovDef;
        destinos: { fila: number; col: number }[];
      }[] = [];

      for (let f = 0; f < DIM; f++) {
        for (let c = 0; c < DIM; c++) {
          if (est.tablero[f][c].ficha?.equipo !== 1) continue;
          for (const carta of est.cartasOponente) {
            const destinos = calcularMovimientosValidos(est.tablero, f, c, carta, 1);
            if (destinos.length > 0) jugadas.push({ fila: f, col: c, carta, destinos });
          }
        }
      }
      if (jugadas.length === 0) { iaOcupada.current = false; return; }

      const j = jugadas[Math.floor(Math.random() * jugadas.length)];
      const d = j.destinos[Math.floor(Math.random() * j.destinos.length)];
      const { nuevoEstado } = ejecutarMovimiento(est, j.fila, j.col, d.fila, d.col, j.carta);
      setEstado(nuevoEstado);
      iaOcupada.current = false;
    }, 900);
  }, []);

  useEffect(() => {
    if (estado.turnoActual === 1 && !estado.ganador) ejecutarIa(estado);
  }, [estado, ejecutarIa]);

  // ─── Interacciones del jugador ───────────────────────────────────────────

  const handleCelda = (fila: number, col: number) => {
    if (estado.turnoActual !== 2 || estado.ganador) return;
    const celda = estado.tablero[fila][col];

    // Destino válido → ejecutar movimiento
    if (
      estado.movimientosValidos.some((m) => m.fila === fila && m.col === col) &&
      estado.fichaSeleccionada &&
      estado.cartaSeleccionada
    ) {
      const { nuevoEstado } = ejecutarMovimiento(
        estado,
        estado.fichaSeleccionada.fila,
        estado.fichaSeleccionada.col,
        fila,
        col,
        estado.cartaSeleccionada
      );
      setEstado(nuevoEstado);
      return;
    }

    // Ficha propia → seleccionar
    if (celda.ficha?.equipo === 2) {
      const movimientosValidos = estado.cartaSeleccionada
        ? calcularMovimientosValidos(estado.tablero, fila, col, estado.cartaSeleccionada, 2)
        : [];
      setEstado((prev) => ({ ...prev, fichaSeleccionada: { fila, col }, movimientosValidos }));
      return;
    }

    // Otro → deseleccionar
    setEstado((prev) => ({
      ...prev,
      fichaSeleccionada: null,
      cartaSeleccionada: null,
      movimientosValidos: [],
    }));
  };

  const handleCarta = (carta: CartaMovDef) => {
    if (estado.turnoActual !== 2 || estado.ganador) return;

    if (estado.cartaSeleccionada?.nombre === carta.nombre) {
      setEstado((prev) => ({ ...prev, cartaSeleccionada: null, movimientosValidos: [] }));
      return;
    }
    const movimientosValidos = estado.fichaSeleccionada
      ? calcularMovimientosValidos(
          estado.tablero,
          estado.fichaSeleccionada.fila,
          estado.fichaSeleccionada.col,
          carta,
          2
        )
      : [];
    setEstado((prev) => ({ ...prev, cartaSeleccionada: carta, movimientosValidos }));
  };

  const min = String(Math.floor(tiempoRestante / 60)).padStart(2, "0");
  const seg = String(tiempoRestante % 60).padStart(2, "0");
  const esTurnoJugador = estado.turnoActual === 2 && !estado.ganador;

  // ─── Mensaje de ayuda (parte inferior) ───────────────────────────────────
  let ayuda = "";
  if (esTurnoJugador) {
    if (!estado.fichaSeleccionada && !estado.cartaSeleccionada)
      ayuda = "Selecciona una de tus piezas (azules)";
    else if (estado.fichaSeleccionada && !estado.cartaSeleccionada)
      ayuda = "Ahora elige una carta del panel derecho";
    else if (estado.fichaSeleccionada && estado.cartaSeleccionada && estado.movimientosValidos.length === 0)
      ayuda = "Sin movimientos válidos con esta combinación. Prueba otra carta o pieza.";
    else if (estado.movimientosValidos.length > 0)
      ayuda = "Haz clic en una casilla blanca para mover";
  } else if (estado.turnoActual === 1 && !estado.ganador) {
    ayuda = `Turno de @${MOCK_OPONENTE.nombre}…`;
  }

  // ─────────────────────────────────────────────────────────────────────────
  return (
    <div className="h-screen flex flex-col overflow-hidden" style={{ background: "#111d2c" }}>

      {/* ═══ HEADER ════════════════════════════════════════════════════════ */}
      <header className="bg-[#1a2d4a] px-5 py-2 flex items-center justify-between shrink-0 shadow-lg">
        <Link href="/" className="flex items-center">
          <Image src="/nombre.png" alt="Onitama" width={110} height={32} priority className="h-8 w-auto object-contain" />
        </Link>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1">
            <Image src="/katanas.png" alt="Puntos" width={18} height={18} className="h-4 w-auto" />
            <span className="text-white font-semibold text-xs">{mockJugador.puntos.toLocaleString()}</span>
          </div>
          <div className="flex items-center gap-1">
            <Image src="/core.png" alt="Cores" width={18} height={18} className="h-4 w-auto" />
            <span className="text-white font-semibold text-xs">{mockJugador.cores.toLocaleString()}</span>
          </div>
          <div className="w-8 h-8 rounded-full bg-[#2a4a6a] border-2 border-white/30 flex items-center justify-center">
            <span className="text-white/50 text-xs">{mockJugador.nombre.charAt(0).toUpperCase()}</span>
          </div>
        </div>
      </header>

      {/* ═══ PANTALLA DE VICTORIA / DERROTA ════════════════════════════════ */}
      {estado.ganador && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-10 flex flex-col items-center gap-5 shadow-2xl max-w-xs w-full mx-4">
            <span className="text-5xl">{estado.ganador === 2 ? "🏆" : "💀"}</span>
            <h2 className="text-2xl font-bold text-white uppercase tracking-widest text-center">
              {estado.ganador === 2 ? "¡Victoria!" : "Derrota"}
            </h2>
            <p className="text-white/60 text-sm text-center">
              {estado.ganador === 2
                ? "¡Excelente partida! Has dominado el tablero."
                : `@${MOCK_OPONENTE.nombre} ha ganado esta vez.`}
            </p>
            <button
              type="button"
              onClick={() => router.push("/partidas")}
              className="w-full py-3 rounded-xl font-bold uppercase tracking-widest text-sm bg-[#e8e8e8] text-[#1a2d4a] hover:bg-white transition-all"
            >
              Volver a partidas
            </button>
            <button
              type="button"
              onClick={() => { setEstado(crearEstadoInicial()); setTiempoRestante(TIEMPO_TURNO); }}
              className="text-white/40 text-xs hover:text-white/70 transition-colors"
            >
              Jugar de nuevo (local)
            </button>
          </div>
        </div>
      )}

      {/* ═══ ÁREA DE JUEGO ══════════════════════════════════════════════════ */}
      <div className="flex flex-1 min-h-0 overflow-hidden">

        {/* ─── PANEL IZQUIERDO: oponente ──────────────────────────────────── */}
        <aside className="w-48 shrink-0 flex flex-col gap-3 px-2 pt-3 pb-2 bg-[#162235] border-r border-white/10 overflow-y-auto">
          {/* Avatar y nombre */}
          <div className="flex flex-col items-center gap-1">
            <div className="w-11 h-11 rounded-full bg-red-900/60 border-2 border-red-400/40 flex items-center justify-center shrink-0">
              <span className="text-white/60 text-base">{MOCK_OPONENTE.nombre.charAt(0).toUpperCase()}</span>
            </div>
            <span className="text-white/80 text-[11px] font-semibold">@{MOCK_OPONENTE.nombre}</span>
          </div>

          {/* Cartas del oponente */}
          <div className="flex flex-col gap-1.5">
            <p className="text-white/40 text-[9px] uppercase tracking-widest text-center">Cartas del oponente</p>
            {estado.cartasOponente.map((c) => (
              <CartaBtn key={c.nombre} carta={c} equipo={1} desactivada />
            ))}
          </div>

          <div className="flex-1" />

          {/* Indicador de turno */}
          <div className="flex flex-col items-center gap-1">
            <p className={`text-center text-[10px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded-md ${
              estado.turnoActual === 1 && !estado.ganador
                ? "text-red-300 bg-red-900/30 animate-pulse"
                : "text-white/30"
            }`}>
              {estado.turnoActual === 1 && !estado.ganador
                ? `Turno de @${MOCK_OPONENTE.nombre}`
                : "Esperando…"}
            </p>
            {/* Timer */}
            <p className={`font-mono text-2xl font-bold tabular-nums ${
              tiempoRestante <= 15 ? "text-red-400 animate-pulse" : "text-white/70"
            }`}>
              {min}:{seg}
            </p>
          </div>
        </aside>

        {/* ─── CENTRO: tablero + cola de cartas ───────────────────────────── */}
        <main className="flex-1 flex flex-col items-center justify-center gap-3 px-4 min-h-0 min-w-0">
          {/* Tablero 7×7 – tamaño calculado para encajar en pantalla sin scroll */}
          <div
            className="grid border-2 border-[#1a2a3a] shadow-2xl shrink-0"
            style={{
              gridTemplateColumns: `repeat(${DIM}, 1fr)`,
              width: "min(min(55vh, 460px), calc(100vw - 460px))",
              aspectRatio: "1",
            }}
          >
            {Array.from({ length: DIM }, (_, fila) =>
              Array.from({ length: DIM }, (_, col) => {
                const celda = estado.tablero[fila][col];
                const esSel =
                  estado.fichaSeleccionada?.fila === fila && estado.fichaSeleccionada?.col === col;
                const esValido = estado.movimientosValidos.some(
                  (m) => m.fila === fila && m.col === col
                );
                const esUlt =
                  !!estado.ultimoMovimiento &&
                  ((estado.ultimoMovimiento.origen.fila === fila && estado.ultimoMovimiento.origen.col === col) ||
                    (estado.ultimoMovimiento.destino.fila === fila && estado.ultimoMovimiento.destino.col === col));

                return (
                  <Celda
                    key={`${fila}-${col}`}
                    ficha={celda.ficha}
                    esTrono={celda.esTrono}
                    esSeleccionada={esSel}
                    esMovimientoValido={esValido}
                    esUltimoMov={esUlt && !esSel && !esValido}
                    onClick={() => handleCelda(fila, col)}
                  />
                );
              })
            )}
          </div>

          {/* Cola de cartas: 3 cartas en fila (siguiente + 2 en espera) */}
          <div className="flex items-center gap-2 shrink-0">
            <p className="text-white/40 text-[9px] uppercase tracking-widest mr-1">Cola:</p>
            {estado.cartasSiguientes.map((carta, i) => (
              <CartaCola
                key={carta.nombre}
                carta={carta}
                equipo={2}
                esLaSiguiente={i === 0}
              />
            ))}
          </div>
        </main>

        {/* ─── PANEL DERECHO: acciones del jugador ────────────────────────── */}
        <aside className="w-52 shrink-0 flex flex-col gap-3 px-2 pt-3 pb-2 bg-[#162235] border-l border-white/10 overflow-y-auto">
          {/* Botón PAUSAR */}
          <button
            type="button"
            onClick={() => router.push("/partidas")}
            className="w-full py-1.5 rounded-lg border border-white/20 text-white/60 text-xs font-bold uppercase tracking-widest hover:bg-white/10 transition-colors shrink-0"
          >
            ⏸ Pausar
          </button>

          {/* Cartas del jugador */}
          <div className="flex flex-col gap-1.5">
            <p className="text-white/40 text-[9px] uppercase tracking-widest text-center">
              {esTurnoJugador ? "Elige una carta" : "Tus cartas"}
            </p>
            {estado.cartasJugador.map((carta) => (
              <CartaBtn
                key={carta.nombre}
                carta={carta}
                equipo={2}
                seleccionada={estado.cartaSeleccionada?.nombre === carta.nombre}
                onClick={() => handleCarta(carta)}
                desactivada={!esTurnoJugador}
              />
            ))}
          </div>

          {/* Área de cartas de acción (reservada) */}
          <div className="flex-1 min-h-0" />
          <div className="rounded-lg border border-white/10 bg-white/5 p-2 flex flex-col items-center gap-1 shrink-0">
            <p className="text-white/30 text-[9px] uppercase tracking-widest text-center">Cartas de acción</p>
            <p className="text-white/20 text-[8px] text-center italic">(Próximamente)</p>
          </div>

          {/* Avatar del jugador */}
          <div className="flex flex-col items-center gap-1 shrink-0">
            <p className={`text-center text-[10px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded-md ${
              esTurnoJugador ? "text-blue-300 bg-blue-900/30" : "text-white/30"
            }`}>
              {esTurnoJugador ? "¡Es tu turno!" : "Esperando…"}
            </p>
            <div className="w-9 h-9 rounded-full bg-[#2a4a6a] border-2 border-blue-400/40 flex items-center justify-center">
              <span className="text-white/60 text-xs">{mockJugador.nombre.charAt(0).toUpperCase()}</span>
            </div>
            <span className="text-white/60 text-[10px]">@{mockJugador.nombre}</span>
          </div>
        </aside>
      </div>

      {/* ═══ BARRA DE AYUDA INFERIOR ════════════════════════════════════════ */}
      {ayuda && (
        <div className="bg-[#0e1820] py-1.5 px-4 text-center shrink-0">
          <p className="text-white/35 text-[10px]">{ayuda}</p>
        </div>
      )}
    </div>
  );
}

// ─── Wrapper con Suspense (requerido por useSearchParams) ────────────────────

function PartidaConParams() {
  const searchParams = useSearchParams();
  const partidaId = searchParams.get("id") ?? "local";
  return <PartidaInterna partidaId={partidaId} />;
}

export default function PartidaPage() {
  return (
    <Suspense
      fallback={
        <div className="h-screen flex items-center justify-center bg-[#111d2c]">
          <p className="text-white/50 text-sm animate-pulse">Cargando partida…</p>
        </div>
      }
    >
      <PartidaConParams />
    </Suspense>
  );
}
