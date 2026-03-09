"use client";

/**
 * Pantalla de Partida – Onitama 7×7 (versión básica sin cartas de acción).
 *
 * ─── Modos de funcionamiento ─────────────────────────────────────────────────
 *  Mock (sin servidor):
 *    - Las cartas se generan aleatoriamente en el cliente.
 *    - El oponente lo controla una IA local que mueve tras 900 ms.
 *    - El timer al llegar a 0 pasa el turno sin penalizar.
 *
 *  Con servidor (NEXT_PUBLIC_WS_URL configurado):
 *    - Las cartas vienen en sessionStorage (guardadas por buscarpartida.ts).
 *    - Al montar se envía ESTOY_LISTO; el servidor responderá con TU_TURNO.
 *    - Los movimientos del oponente llegan como mensajes MOVER.
 *    - El servidor gestiona el timer autoritativamente y envía TERMINAR_PARTIDA.
 *    - El timer visual sirve solo de referencia; al llegar a 0 espera al servidor.
 *
 * ─── Protocolo de mensajes (diagrama de secuencia del backend) ────────────────
 *  Cliente → Servidor: ESTOY_LISTO, MOVER, ABANDONAR
 *  Servidor → Cliente: TU_TURNO, MOVER, TERMINAR_PARTIDA
 *
 * ─── Flujo de interacción (turno del jugador) ─────────────────────────────────
 *  1. Clic en una ficha propia  → highlight amarillo
 *  2. Clic en una carta         → highlight azul en destinos válidos
 *  3. Clic en casilla azul      → ejecutar movimiento y enviar MOVER al servidor
 */

import { useState, useEffect, useCallback, useRef, Suspense } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Image from "next/image";
import Link from "next/link";
import {
  crearEstadoInicial,
  crearEstadoDesdeServidor,
  calcularMovimientosValidos,
  ejecutarMovimiento,
  type EstadoJuego,
  type EquipoID,
  DIM,
} from "@/lib/juego";
import { TODAS_LAS_CARTAS, type CartaMovDef } from "@/lib/cartas";
import { mockJugador } from "@/lib/mockJugador";
import {
  getWsActivo,
  conectarPartida,
  enviarEstoyListo,
  enviarMovimiento,
  enviarAbandonar,
  type RespuestaMover,
  type RespuestaTerminarPartida,
  type RespuestaPartidaEncontrada,
} from "@/api/partida";

// ─── Constantes ───────────────────────────────────────────────────────────────

const TIEMPO_TURNO = 120;
const MOCK_OPONENTE = { nombre: "granluchador", puntos: 1200 };

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
  carta, equipo, seleccionada = false, onClick, desactivada = false,
}: {
  carta: CartaMovDef; equipo: EquipoID; seleccionada?: boolean;
  onClick?: () => void; desactivada?: boolean;
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
        <span className="font-bold uppercase tracking-wide leading-none text-[10px] truncate">{carta.nombre}</span>
        <MiniGrid carta={carta} equipo={equipo} />
      </div>
    </button>
  );
}

// ─── Carta pequeña (cola) ────────────────────────────────────────────────────

function CartaCola({ carta, equipo, esLaSiguiente }: {
  carta: CartaMovDef; equipo: EquipoID; esLaSiguiente: boolean;
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
  ficha, esTrono, esSeleccionada, esMovimientoValido, esUltimoMov, onClick,
}: {
  ficha: { equipo: EquipoID; esRey: boolean } | null;
  esTrono: boolean; esSeleccionada: boolean;
  esMovimientoValido: boolean; esUltimoMov: boolean; onClick: () => void;
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
      {esTrono && !ficha && <span className="text-white/20 text-[10px] select-none">🏯</span>}
      {ficha && (
        <div className={`w-[70%] h-[70%] rounded-full flex items-center justify-center shadow-md ${
          ficha.equipo === 1 ? "bg-red-700 border-2 border-red-400" : "bg-blue-700 border-2 border-blue-400"
        }`}>
          {ficha.esRey && (
            <span className="text-white leading-none select-none" style={{ fontSize: "40%" }}>🏯</span>
          )}
        </div>
      )}
    </button>
  );
}

// ─── Lógica principal ─────────────────────────────────────────────────────────

function PartidaInterna({ partidaId }: { partidaId: string }) {
  const router = useRouter();

  // ── Detectar modo servidor (se comprueba una vez al montar) ─────────────────
  const enServidor = useRef(!!getWsActivo());
  /** Equipo del jugador local: 1 = arriba, 2 = abajo (por defecto 2 en mock) */
  const equipoJugadorRef = useRef<1 | 2>(2);

  // ── Estado inicial: desde servidor (sessionStorage) o mock ──────────────────
  const [estado, setEstado] = useState<EstadoJuego>(() => {
    if (typeof window !== "undefined") {
      const raw = sessionStorage.getItem("datosPartida");
      if (raw) {
        try {
          const datos = JSON.parse(raw) as RespuestaPartidaEncontrada;
          return crearEstadoDesdeServidor(datos);
        } catch {
          console.warn("[partida] Datos de sessionStorage inválidos. Usando mock.");
        }
      }
    }
    return crearEstadoInicial();
  });

  // ── Datos del oponente (del servidor o mock) ────────────────────────────────
  const infoOponente = useRef<{ nombre: string; puntos: number }>(MOCK_OPONENTE);
  useEffect(() => {
    const raw = sessionStorage.getItem("datosPartida");
    if (raw) {
      try {
        const datos = JSON.parse(raw) as RespuestaPartidaEncontrada;
        equipoJugadorRef.current = datos.equipo;
        infoOponente.current = { nombre: datos.oponente, puntos: datos.oponentePt };
      } catch { /* mantener mock */ }
    }
  }, []);

  // ── Estados adicionales ─────────────────────────────────────────────────────
  const [tiempoRestante, setTiempoRestante] = useState(TIEMPO_TURNO);

  /**
   * Mientras aguardandoInicio = true (solo en modo servidor), el jugador
   * no puede interactuar. Se desactiva al recibir TU_TURNO o el primer MOVER.
   */
  const [aguardandoInicio, setAguardandoInicio] = useState(() => enServidor.current);

  /**
   * Resultado declarado por el servidor (TERMINAR_PARTIDA).
   * En mock, el ganador se lee de estado.ganador.
   */
  const [resultadoFinal, setResultadoFinal] = useState<{
    ganador: string; razon: string;
  } | null>(null);

  /** Controla la visibilidad del modal de confirmación de abandono */
  const [mostrarModalAbandono, setMostrarModalAbandono] = useState(false);

  const iaOcupada = useRef(false);

  // ─── Conexión WS y mensajes del servidor ─────────────────────────────────
  useEffect(() => {
    if (!enServidor.current) return; // Modo mock: no hay WS

    // Avisar al servidor que la pantalla está lista (espera ambos ESTOY_LISTO)
    enviarEstoyListo();

    // Escuchar mensajes del servidor durante la partida
    const desconectar = conectarPartida((msg) => {
      switch (msg.tipo) {
        case "TU_TURNO":
          // El servidor nos autoriza a mover primero
          setAguardandoInicio(false);
          break;

        case "MOVER": {
          // El oponente ha movido; actualizamos el tablero
          const m = msg as RespuestaMover;
          const carta = TODAS_LAS_CARTAS.find((c) => c.nombre === m.carta);
          if (!carta) return;
          setAguardandoInicio(false); // Si el oponente movió primero, ahora nos toca
          setEstado((prev) => {
            const { nuevoEstado } = ejecutarMovimiento(
              prev, m.fila_origen, m.col_origen, m.fila_destino, m.col_destino, carta
            );
            return nuevoEstado;
          });
          break;
        }

        case "TERMINAR_PARTIDA": {
          // El servidor declara el resultado (tiempo, victoria, abandono)
          const t = msg as RespuestaTerminarPartida;
          setResultadoFinal({ ganador: t.ganador, razon: t.razon });
          break;
        }
      }
    });

    return desconectar;
  }, []); // Solo al montar

  // ─── Timer visual ─────────────────────────────────────────────────────────
  useEffect(() => {
    // No correr el timer si la partida terminó
    if (estado.ganador || resultadoFinal) return;

    setTiempoRestante(TIEMPO_TURNO);
    const intervalo = setInterval(() => {
      setTiempoRestante((t) => {
        if (t <= 1) {
          clearInterval(intervalo);
          if (enServidor.current) {
            // En modo servidor: el timer es solo visual. El servidor gestiona
            // la derrota por tiempo y enviará TERMINAR_PARTIDA cuando corresponda.
            return 0;
          } else {
            // En mock: pasar turno sin penalizar (comportamiento de desarrollo)
            setEstado((prev) => ({
              ...prev,
              turnoActual: prev.turnoActual === 2 ? 1 : 2,
              fichaSeleccionada: null,
              cartaSeleccionada: null,
              movimientosValidos: [],
            }));
            return TIEMPO_TURNO;
          }
        }
        return t - 1;
      });
    }, 1000);
    return () => clearInterval(intervalo);
  }, [estado.turnoActual, estado.ganador, resultadoFinal]);

  // ─── IA del oponente (solo en modo mock) ──────────────────────────────────
  const ejecutarIa = useCallback((est: EstadoJuego) => {
    if (iaOcupada.current) return;
    iaOcupada.current = true;
    setTimeout(() => {
      const jugadas: {
        fila: number; col: number; carta: CartaMovDef;
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
    if (!enServidor.current && estado.turnoActual === 1 && !estado.ganador) {
      ejecutarIa(estado);
    }
  }, [estado, ejecutarIa]);

  // ─── Interacciones del jugador ─────────────────────────────────────────────

  const handleCelda = (fila: number, col: number) => {
    if (aguardandoInicio || estado.turnoActual !== 2 || estado.ganador || resultadoFinal) return;
    const celda = estado.tablero[fila][col];

    // Destino válido → ejecutar y enviar al servidor
    if (
      estado.movimientosValidos.some((m) => m.fila === fila && m.col === col) &&
      estado.fichaSeleccionada &&
      estado.cartaSeleccionada
    ) {
      const { nuevoEstado } = ejecutarMovimiento(
        estado,
        estado.fichaSeleccionada.fila,
        estado.fichaSeleccionada.col,
        fila, col,
        estado.cartaSeleccionada
      );
      setEstado(nuevoEstado);

      // Enviar al servidor (solo si está conectado)
      enviarMovimiento({
        equipo: equipoJugadorRef.current,
        col_origen: estado.fichaSeleccionada.col,
        fila_origen: estado.fichaSeleccionada.fila,
        col_destino: col,
        fila_destino: fila,
        carta: estado.cartaSeleccionada.nombre,
      });
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
      ...prev, fichaSeleccionada: null, cartaSeleccionada: null, movimientosValidos: [],
    }));
  };

  const handleCarta = (carta: CartaMovDef) => {
    if (aguardandoInicio || estado.turnoActual !== 2 || estado.ganador || resultadoFinal) return;
    if (estado.cartaSeleccionada?.nombre === carta.nombre) {
      setEstado((prev) => ({ ...prev, cartaSeleccionada: null, movimientosValidos: [] }));
      return;
    }
    const movimientosValidos = estado.fichaSeleccionada
      ? calcularMovimientosValidos(
          estado.tablero, estado.fichaSeleccionada.fila, estado.fichaSeleccionada.col, carta, 2
        )
      : [];
    setEstado((prev) => ({ ...prev, cartaSeleccionada: carta, movimientosValidos }));
  };

  /** El jugador confirma que quiere abandonar: notifica al servidor y vuelve */
  const handleConfirmarAbandonar = () => {
    setMostrarModalAbandono(false);
    enviarAbandonar(); // No hace nada si no hay servidor conectado
    router.push("/partidas");
  };

  // ─── Derivados para el render ──────────────────────────────────────────────

  const min = String(Math.floor(tiempoRestante / 60)).padStart(2, "0");
  const seg = String(tiempoRestante % 60).padStart(2, "0");
  const esTurnoJugador = !aguardandoInicio && estado.turnoActual === 2 && !estado.ganador && !resultadoFinal;

  // Ganador: en mock viene de estado.ganador; en servidor de resultadoFinal
  const hayFinPartida = enServidor.current
    ? resultadoFinal !== null
    : estado.ganador !== null;

  const esVictoria = enServidor.current
    ? resultadoFinal?.ganador === mockJugador.nombre
    : estado.ganador === 2;

  const razonFin = resultadoFinal?.razon ?? (estado.ganador === 2 ? "Victoria" : "Derrota");

  const nombreOponente = infoOponente.current.nombre;

  let ayuda = "";
  if (aguardandoInicio) ayuda = "Esperando al servidor para comenzar…";
  else if (esTurnoJugador) {
    if (!estado.fichaSeleccionada && !estado.cartaSeleccionada) ayuda = "Selecciona una de tus piezas (azules)";
    else if (estado.fichaSeleccionada && !estado.cartaSeleccionada) ayuda = "Ahora elige una carta del panel derecho";
    else if (estado.movimientosValidos.length === 0) ayuda = "Sin movimientos válidos. Prueba otra carta o pieza.";
    else ayuda = "Haz clic en una casilla blanca para mover";
  } else if (!hayFinPartida) {
    ayuda = enServidor.current
      ? `Turno de @${nombreOponente}…`
      : `Turno de @${nombreOponente}…`;
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

      {/* ═══ MODAL: FIN DE PARTIDA ══════════════════════════════════════════ */}
      {hayFinPartida && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-10 flex flex-col items-center gap-5 shadow-2xl max-w-xs w-full mx-4">
            <span className="text-5xl">{esVictoria ? "🏆" : "💀"}</span>
            <h2 className="text-2xl font-bold text-white uppercase tracking-widest text-center">
              {esVictoria ? "¡Victoria!" : "Derrota"}
            </h2>
            <p className="text-white/50 text-xs uppercase tracking-widest">{razonFin.replace(/_/g, " ")}</p>
            <p className="text-white/60 text-sm text-center">
              {esVictoria
                ? "¡Excelente partida! Has dominado el tablero."
                : `@${nombreOponente} ha ganado esta vez.`}
            </p>
            <button
              type="button"
              onClick={() => router.push("/partidas")}
              className="w-full py-3 rounded-xl font-bold uppercase tracking-widest text-sm bg-[#e8e8e8] text-[#1a2d4a] hover:bg-white transition-all"
            >
              Volver a partidas
            </button>
            {!enServidor.current && (
              <button
                type="button"
                onClick={() => { setEstado(crearEstadoInicial()); setResultadoFinal(null); setTiempoRestante(TIEMPO_TURNO); }}
                className="text-white/40 text-xs hover:text-white/70 transition-colors"
              >
                Jugar de nuevo (local)
              </button>
            )}
          </div>
        </div>
      )}

      {/* ═══ MODAL: CONFIRMAR ABANDONO ══════════════════════════════════════ */}
      {mostrarModalAbandono && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-8 flex flex-col items-center gap-5 shadow-2xl max-w-sm w-full mx-4">
            <span className="text-4xl">⚠️</span>
            <h2 className="text-xl font-bold text-white uppercase tracking-widest text-center">
              ¿Abandonar partida?
            </h2>
            <p className="text-white/60 text-sm text-center">
              Si abandonas, perderás la partida y tu oponente será declarado ganador.
            </p>
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={() => setMostrarModalAbandono(false)}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm border border-white/20 text-white/70 hover:bg-white/10 transition-colors"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmarAbandonar}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm bg-red-700 text-white hover:bg-red-600 transition-colors"
              >
                Abandonar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ═══ ÁREA DE JUEGO ══════════════════════════════════════════════════ */}
      <div className="flex flex-1 min-h-0 overflow-hidden">

        {/* ─── PANEL IZQUIERDO: oponente ──────────────────────────────────── */}
        <aside className="w-48 shrink-0 flex flex-col gap-3 px-2 pt-3 pb-2 bg-[#162235] border-r border-white/10 overflow-y-auto">
          <div className="flex flex-col items-center gap-1">
            <div className="w-11 h-11 rounded-full bg-red-900/60 border-2 border-red-400/40 flex items-center justify-center shrink-0">
              <span className="text-white/60 text-base">{nombreOponente.charAt(0).toUpperCase()}</span>
            </div>
            <span className="text-white/80 text-[11px] font-semibold">@{nombreOponente}</span>
            <span className="text-white/30 text-[9px]">{infoOponente.current.puntos} pts</span>
          </div>

          <div className="flex flex-col gap-1.5">
            <p className="text-white/40 text-[9px] uppercase tracking-widest text-center">Cartas del oponente</p>
            {estado.cartasOponente.map((c) => (
              <CartaBtn key={c.nombre} carta={c} equipo={1} desactivada />
            ))}
          </div>

          <div className="flex-1" />

          <div className="flex flex-col items-center gap-1">
            <p className={`text-center text-[10px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded-md ${
              !aguardandoInicio && estado.turnoActual === 1 && !hayFinPartida
                ? "text-red-300 bg-red-900/30 animate-pulse"
                : "text-white/30"
            }`}>
              {aguardandoInicio
                ? "Preparando…"
                : !aguardandoInicio && estado.turnoActual === 1 && !hayFinPartida
                ? `Turno de @${nombreOponente}`
                : "Esperando…"}
            </p>
            <p className={`font-mono text-2xl font-bold tabular-nums ${
              tiempoRestante <= 15 && tiempoRestante > 0 ? "text-red-400 animate-pulse" : "text-white/70"
            }`}>
              {min}:{seg}
            </p>
          </div>
        </aside>

        {/* ─── CENTRO: tablero + cola de cartas ───────────────────────────── */}
        <main className="flex-1 flex flex-col items-center justify-center gap-3 px-4 min-h-0 min-w-0">
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
                const esSel = estado.fichaSeleccionada?.fila === fila && estado.fichaSeleccionada?.col === col;
                const esValido = estado.movimientosValidos.some((m) => m.fila === fila && m.col === col);
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

          <div className="flex items-center gap-2 shrink-0">
            <p className="text-white/40 text-[9px] uppercase tracking-widest mr-1">Cola:</p>
            {estado.cartasSiguientes.map((carta, i) => (
              <CartaCola key={carta.nombre} carta={carta} equipo={2} esLaSiguiente={i === 0} />
            ))}
          </div>
        </main>

        {/* ─── PANEL DERECHO: acciones del jugador ────────────────────────── */}
        <aside className="w-52 shrink-0 flex flex-col gap-3 px-2 pt-3 pb-2 bg-[#162235] border-l border-white/10 overflow-y-auto">
          {/* Botón ABANDONAR (reemplaza PAUSAR) */}
          <button
            type="button"
            onClick={() => setMostrarModalAbandono(true)}
            className="w-full py-1.5 rounded-lg border border-red-800/60 text-red-400/70 text-xs font-bold uppercase tracking-widest hover:bg-red-900/20 hover:text-red-300 transition-colors shrink-0"
          >
            🚩 Abandonar
          </button>

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

          <div className="flex-1 min-h-0" />
          <div className="rounded-lg border border-white/10 bg-white/5 p-2 flex flex-col items-center gap-1 shrink-0">
            <p className="text-white/30 text-[9px] uppercase tracking-widest text-center">Cartas de acción</p>
            <p className="text-white/20 text-[8px] text-center italic">(Próximamente)</p>
          </div>

          <div className="flex flex-col items-center gap-1 shrink-0">
            <p className={`text-center text-[10px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded-md ${
              esTurnoJugador ? "text-blue-300 bg-blue-900/30"
              : aguardandoInicio ? "text-yellow-300/70 bg-yellow-900/20"
              : "text-white/30"
            }`}>
              {aguardandoInicio ? "Esperando inicio…" : esTurnoJugador ? "¡Es tu turno!" : "Esperando…"}
            </p>
            <div className="w-9 h-9 rounded-full bg-[#2a4a6a] border-2 border-blue-400/40 flex items-center justify-center">
              <span className="text-white/60 text-xs">{mockJugador.nombre.charAt(0).toUpperCase()}</span>
            </div>
            <span className="text-white/60 text-[10px]">@{mockJugador.nombre}</span>
            <span className="text-white/30 text-[9px]">{mockJugador.puntos} pts</span>
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

// ─── Wrapper con Suspense ─────────────────────────────────────────────────────

function PartidaConParams() {
  const searchParams = useSearchParams();
  const partidaId = searchParams.get("id") ?? "local";
  return <PartidaInterna partidaId={partidaId} />;
}

export default function PartidaPage() {
  return (
    <Suspense fallback={
      <div className="h-screen flex items-center justify-center bg-[#111d2c]">
        <p className="text-white/50 text-sm animate-pulse">Cargando partida…</p>
      </div>
    }>
      <PartidaConParams />
    </Suspense>
  );
}
