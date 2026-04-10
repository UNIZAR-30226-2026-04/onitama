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
import {
  crearEstadoInicial,
  crearEstadoDesdeServidor,
  crearTableroInicial,
  calcularMovimientosValidos,
  ejecutarMovimiento,
  type EstadoJuego,
  type EquipoID,
  DIM,
} from "@/lib/juego";
import { TODAS_LAS_CARTAS, getImagenCarta, type CartaMovDef } from "@/lib/cartas";
import { obtenerJugadorActivo, guardarSesion } from "@/lib/sesion";
import { obtenerPerfil } from "@/api/auth";
import { calcularMejorMovimientoIA, type Dificultad } from "@/lib/ia";
import { getBoardStyle, getColorMovimiento, getEquipoNombre, getPiezaSrc, normalizarSkinId } from "@/lib/skins";
import { AvatarCircle } from "@/lib/avatar";
import { usarServidor } from "@/api/ws";
import {
  conectarPartida,
  desconectarPartida,
  enviarEstoyListo,
  enviarMovimiento,
  enviarAbandonar,
  enviarSolicitarPausa,
  enviarAceptarPausa,
  enviarRechazarPausa,
  type RespuestaMover,
  type RespuestaTerminarPartida,
  type RespuestaPartidaEncontrada,
} from "@/api/partida";

// ─── Constantes ───────────────────────────────────────────────────────────────

const TIEMPO_TURNO = 120;

const NOMBRE_DIFICULTAD: Record<Dificultad, string> = {
  facil: "Fácil",
  medio: "Medio",
  dificil: "Difícil",
};

function getMockOponente(dificultad: Dificultad) {
  return {
    nombre: `Iron Bot (${NOMBRE_DIFICULTAD[dificultad]})`,
    puntos: dificultad === "facil" ? 800 : dificultad === "medio" ? 1200 : 1600,
    avatar_id: null,
  };
}

// ─── Mini cuadrícula de la carta ─────────────────────────────────────────────

/**
 * equipo:     determina la orientación (signo) de los movimientos.
 *             2 = perspectiva del jugador local (hacia arriba)
 *             1 = perspectiva del rival (invertido)
 * colorDots:  color CSS de los puntos activos (movimientos posibles).
 *             Se adapta según skin + equipo.
 */
function MiniGrid({
  carta, equipo, colorDots = "#3b82f6", size = 5,
}: {
  carta: CartaMovDef; equipo: EquipoID; colorDots?: string; size?: number;
}) {
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
      className="grid shrink-0"
      style={{
        gridTemplateColumns: `repeat(${DIM}, 1fr)`,
        gap: "1px",
        width: size * DIM + (DIM - 1),
        height: size * DIM + (DIM - 1),
      }}
      aria-hidden
    >
      {Array.from({ length: DIM }, (_, f) =>
        Array.from({ length: DIM }, (_, c) => {
          const esC = f === CENTRO && c === CENTRO;
          const esA = activas.has(`${f},${c}`);
          return (
            <div
              key={`${f}-${c}`}
              className={`rounded-[1px] ${esC ? "bg-[#9a8a72]" : "bg-[#c8bba8]"}`}
              style={{
                width: size,
                height: size,
                ...(esA
                  ? {
                      background: colorDots,
                      boxShadow:
                        colorDots === "#f8fafc"
                          ? "inset 0 0 0 1px rgba(15,23,42,0.45)"
                          : undefined,
                    }
                  : {}),
              }}
            />
          );
        })
      )}
    </div>
  );
}

// ─── Tarjeta de movimiento ────────────────────────────────────────────────────

function CartaBtn({
  carta, equipo, colorDots = "bg-blue-500",
  seleccionada = false, onClick, desactivada = false,
}: {
  carta: CartaMovDef; equipo: EquipoID; colorDots?: string;
  seleccionada?: boolean; onClick?: () => void; desactivada?: boolean;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={desactivada}
      title={carta.nombre}
      className={`flex items-stretch gap-3 rounded-xl px-2 py-2 border-2 transition-all duration-150 text-xs w-full overflow-hidden ${seleccionada
        ? "border-blue-400 bg-blue-900/60 text-blue-100 scale-[1.03]"
        : desactivada
          ? "border-[#c8b89a]/50 bg-[#f5ede0] text-[#5c4a35]/60 cursor-default"
          : "border-[#c8b89a] bg-[#f5ede0] text-[#2d1a0a] hover:bg-[#ede0cc] hover:border-[#a8906a] cursor-pointer"
        }`}
    >
      <div className="relative w-20 h-20 shrink-0 rounded-lg overflow-hidden bg-[#2d1a0a]/15 border border-[#c8b89a]/30">
        <Image
          src={getImagenCarta(carta.nombre)}
          alt={carta.nombre}
          fill
          className="object-contain p-1"
          sizes="80px"
        />
      </div>
      <div className="flex flex-col gap-1 min-w-0 flex-1 justify-center items-center">
        <span className="font-bold uppercase tracking-wide leading-none text-xs truncate text-center">
          {carta.nombre}
        </span>
        <MiniGrid carta={carta} equipo={equipo} colorDots={colorDots} size={6} />
      </div>
    </button>
  );
}

// ─── Carta pequeña (mazo) ────────────────────────────────────────────────────

/**
 * La carta muestra siempre su nombre del animal dentro.
 * Si es la SIGUIENTE, aparece además una etiqueta "Siguiente" debajo de la carta.
 */
function CartaCola({
  carta, equipo, colorDots, esLaSiguiente,
}: {
  carta: CartaMovDef; equipo: EquipoID; colorDots: string; esLaSiguiente: boolean;
}) {
  return (
    <div className="flex flex-col items-center gap-1">
      <div
        className={`flex items-center gap-2 rounded-xl px-3 py-2 border transition-all ${esLaSiguiente
          ? "border-yellow-600 bg-yellow-200 text-yellow-900 shadow-md scale-[1.02]"
          : "border-[#c8b89a]/60 bg-[#f5ede0] text-[#2d1a0a]"
          }`}
        title={carta.nombre}
      >
        <div className="relative w-10 h-10 shrink-0 rounded-lg overflow-hidden bg-white/50">
          <Image
            src={getImagenCarta(carta.nombre)}
            alt={carta.nombre}
            fill
            className="object-contain p-0.5"
            sizes="40px"
          />
        </div>
        <div className="flex flex-col gap-0.5">
          <span className="text-[10px] font-bold uppercase tracking-wide truncate">
            {carta.nombre}
          </span>
          <MiniGrid carta={carta} equipo={equipo} colorDots={colorDots} size={5} />
        </div>
      </div>
      {esLaSiguiente && (
        <span className="text-[9px] font-bold uppercase tracking-widest text-yellow-600/90">
          Siguiente
        </span>
      )}
    </div>
  );
}

// ─── Celda del tablero ────────────────────────────────────────────────────────

function Celda({
  ficha, esTrono, equipoTrono, esSeleccionada, esMovimientoValido, esUltimoMov, onClick, skinActiva, baseClase, bordeClase,
}: {
  ficha: { equipo: EquipoID; esRey: boolean } | null;
  esTrono: boolean; equipoTrono: EquipoID | null; esSeleccionada: boolean;
  esMovimientoValido: boolean; esUltimoMov: boolean; onClick: () => void;
  skinActiva: string;
  baseClase: string;
  bordeClase: string;
}) {
  let bg = baseClase;
  if (esSeleccionada) bg = "bg-yellow-300";
  else if (esMovimientoValido) bg = "bg-[#93c5fd] cursor-pointer hover:bg-blue-300";
  else if (esUltimoMov) bg = "bg-yellow-100";

  return (
    <button
      type="button"
      onClick={onClick}
      className={`aspect-square flex items-center justify-center relative border transition-colors duration-100 ${bordeClase} ${bg} overflow-hidden`}
    >
      {/* Fondo del Templo */}
      {esTrono && equipoTrono && (
        <div
          className={`absolute inset-0 ${
            normalizarSkinId(skinActiva) === "Skin1" ? "opacity-75" : "opacity-40"
          }`}
        >
          {(() => {
            const skin = normalizarSkinId(skinActiva);
            const templeClass =
              skin === "Skin1"
                ? "object-contain p-0 contrast-200 saturate-200 brightness-75"
                : skin === "Skin2"
                  ? "object-contain p-[2px]"
                  : "object-contain p-[1px]";
            return (
          <Image
            src={getPiezaSrc("templo", equipoTrono, normalizarSkinId(skinActiva))}
            alt="Templo"
            fill
            sizes="(max-width: 768px) 100vw, 100px"
            className={templeClass}
          />
            );
          })()}
        </div>
      )}

      {/* Ficha (Peón o Rey) */}
      {ficha && (
        <div className="relative w-[85%] h-[85%] flex items-center justify-center z-10">
          <Image
            src={
              ficha.esRey
                ? getPiezaSrc("rey", ficha.equipo, normalizarSkinId(skinActiva))
                : getPiezaSrc("peon", ficha.equipo, normalizarSkinId(skinActiva))
            }
            alt={ficha.esRey ? "Rey" : "Peón"}
            fill
            sizes="(max-width: 768px) 100vw, 100px"
            className="object-contain drop-shadow-md"
          />

        </div>
      )}
    </button>
  );
}

interface MovimientoUI {
  id: string;
  jugador: string;
  equipo: EquipoID;
  carta: CartaMovDef;
  origen: { fila: number; col: number };
  destino: { fila: number; col: number };
}

function formatoCasilla(fila: number, col: number): string {
  return `(${fila + 1},${col + 1})`;
}

function UltimoMovimientoGhost({
  titulo,
  mov,
}: {
  titulo: string;
  mov: MovimientoUI | null;
}) {
  return (
    <div className="rounded-lg border border-white/10 bg-white/5 p-2">
      <p className="text-white/30 text-[9px] uppercase tracking-widest text-center mb-1">{titulo}</p>
      {!mov ? (
        <p className="text-white/20 text-[8px] text-center italic">Sin movimiento</p>
      ) : (
        <div className="rounded-lg border border-white/10 bg-black/20 p-2 opacity-60">
          <div className="flex items-center gap-2">
            <div className="relative w-16 h-16 shrink-0 rounded-lg overflow-hidden bg-white/10">
              <Image
                src={getImagenCarta(mov.carta.nombre)}
                alt={mov.carta.nombre}
                fill
                className="object-contain p-1"
                sizes="64px"
              />
            </div>
            <div className="min-w-0 flex-1">
              <p className="text-[10px] text-white/80 font-semibold uppercase truncate">{mov.carta.nombre}</p>
              <p className="text-[8px] text-white/50">
                {formatoCasilla(mov.origen.fila, mov.origen.col)} to {formatoCasilla(mov.destino.fila, mov.destino.col)}
              </p>
            </div>
          </div>
          <div className="mt-2 flex justify-center">
            <MiniGrid
              carta={mov.carta}
              equipo={mov.equipo}
              colorDots={getColorMovimiento(skinActiva, mov.equipo)}
              size={7}
            />
          </div>
        </div>
      )}
    </div>
  );
}

// ─── Lógica principal ─────────────────────────────────────────────────────────

function PartidaInterna({
  partidaId,
  dificultad,
  modoEntrenamiento,
}: {
  partidaId: string;
  dificultad: Dificultad;
  modoEntrenamiento: boolean;
}) {
  const router = useRouter();

  // ── Detectar modo servidor (se comprueba una vez al montar) ─────────────────
  // Se considera modo servidor si hay URL configurada Y datosPartida en sessionStorage
  // (buscarpartida.ts guarda datosPartida al recibir PARTIDA_ENCONTRADA del servidor;
  //  el modo entrenamiento/mock nunca lo guarda).
  const [esModoServidor] = useState<boolean>(() => {
    if (modoEntrenamiento) {
      sessionStorage.removeItem("datosPartida");
      return false;
    }
    return usarServidor && !!sessionStorage.getItem("datosPartida");
  });
  const enServidor = useRef(esModoServidor);
  /** Equipo del jugador local: 1 = arriba (rojo), 2 = abajo (azul). Por defecto 2 en mock. */
  const equipoJugadorRef = useRef<1 | 2>(1);
  const [miEquipoActual, setMiEquipoActual] = useState<1 | 2>(1);
  const jugadorActual = obtenerJugadorActivo();
  const nombreJugador = jugadorActual.nombre;
  const skinActiva = normalizarSkinId(jugadorActual.skin_activa);
  const nombreEquipoJugador = getEquipoNombre(skinActiva, miEquipoActual);
  const infoOponente = useRef<{ nombre: string; puntos: number; avatar_id: string | null }>(getMockOponente(dificultad));
  const [infoOponenteUI, setInfoOponenteUI] = useState<{ nombre: string; puntos: number; avatar_id: string | null }>(
    getMockOponente(dificultad)
  );

  const [mounted, setMounted] = useState(false);
  const [tipoPartida, setTipoPartida] = useState<"PUBLICA" | "PRIVADA" | "ENTRENAMIENTO">("ENTRENAMIENTO");
  const [estado, setEstado] = useState<EstadoJuego>(() => ({
    tablero: crearTableroInicial(),
    turnoActual: 1,
    // Use first 7 cards as stable initial state for SSR
    cartasJugador: [TODAS_LAS_CARTAS[0], TODAS_LAS_CARTAS[1]],
    cartasOponente: [TODAS_LAS_CARTAS[2], TODAS_LAS_CARTAS[3]],
    cartasSiguientes: [TODAS_LAS_CARTAS[4], TODAS_LAS_CARTAS[5], TODAS_LAS_CARTAS[6]],
    fichaSeleccionada: null,
    cartaSeleccionada: null,
    movimientosValidos: [],
    ganador: null,
    ultimoMovimiento: null,
  }));

  const [aguardandoInicio, setAguardandoInicio] = useState<boolean>(true);
  const [historialMovimientos, setHistorialMovimientos] = useState<MovimientoUI[]>([]);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setMounted(true);
    setHistorialMovimientos([]);
    const raw = sessionStorage.getItem("datosPartida");
    if (raw) {
      try {
        const datos = JSON.parse(raw) as Omit<RespuestaPartidaEncontrada, "tipo"> & {
          tipo?: "PARTIDA_ENCONTRADA" | "PARTIDA_PRIVADA_ENCONTRADA";
          tablero_eq1?: string;
          tablero_eq2?: string;
          turno?: number;
        };
        const estadoInicial = crearEstadoDesdeServidor(datos as RespuestaPartidaEncontrada & { tablero_eq1?: string; tablero_eq2?: string; turno?: number });
        setEstado(estadoInicial);
        equipoJugadorRef.current = datos.equipo;
        setMiEquipoActual(datos.equipo);
        infoOponente.current = {
          nombre: datos.oponente,
          puntos: datos.oponentePt,
          avatar_id: datos.oponente_avatar_id ?? null,
        };
        setInfoOponenteUI({
          nombre: datos.oponente,
          puntos: datos.oponentePt,
          avatar_id: datos.oponente_avatar_id ?? null,
        });
        // Si hay tablero guardado, el turno viene del servidor; si no, equipo 2 espera siempre
        const esReanudada = !!(datos.tablero_eq1 && datos.tablero_eq2);
        if (esReanudada) {
          // aguardandoInicio = true solo si NO es mi turno
          setAguardandoInicio(estadoInicial.turnoActual !== datos.equipo);
        } else {
          setAguardandoInicio(datos.equipo === 2);
        }
        setTipoPartida(
          datos.tipo === "PARTIDA_PRIVADA_ENCONTRADA" ? "PRIVADA" : "PUBLICA"
        );
      } catch {
        setEstado(crearEstadoInicial());
        setAguardandoInicio(false);
        setTipoPartida("ENTRENAMIENTO");
      }
    } else {
      // Mock mode: randomization happens only on client
      setEstado(crearEstadoInicial());
      setAguardandoInicio(false);
      setTipoPartida("ENTRENAMIENTO");
    }
  }, []);

  /**
   * Resultado declarado por el servidor (TERMINAR_PARTIDA).
   * En mock, el ganador se lee de estado.ganador.
   */
  const [tiempoRestante, setTiempoRestante] = useState(TIEMPO_TURNO);
  const [resultadoFinal, setResultadoFinal] = useState<{
    ganador: string; razon: string;
  } | null>(null);
  const [razonLocalFin, setRazonLocalFin] = useState<string | null>(null);

  /** Controla la visibilidad del modal de confirmación de abandono */
  const [mostrarModalAbandono, setMostrarModalAbandono] = useState(false);
  /** Modal de solicitud de pausa (solo partidas privadas) */
  const [mostrarModalPausa, setMostrarModalPausa] = useState(false);
  const [mensajePausaPendiente, setMensajePausaPendiente] = useState<string | null>(null);
  /** Pausa solicitada por mí — esperando respuesta del rival */
  const [pausaEnCurso, setPausaEnCurso] = useState(false);
  /** Solicitud de pausa recibida del rival */
  const [solicitudPausaEntrante, setSolicitudPausaEntrante] = useState<{
    remitente: string;
    idNotificacion: number;
  } | null>(null);

  const iaOcupada = useRef(false);

  const registrarMovimiento = useCallback((mov: Omit<MovimientoUI, "id">) => {
    setHistorialMovimientos((prev) => {
      const id = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
      const siguiente = [...prev, { id, ...mov }];
      return siguiente.slice(-12);
    });
  }, []);

  // ─── Conexión WS y mensajes del servidor ─────────────────────────────────
  useEffect(() => {
    if (!enServidor.current) return; // Modo mock: no hay WS

    // En desarrollo con StrictMode, este componente puede montarse dos veces.
    // Persistimos una marca por partida para evitar enviar ESTOY_LISTO duplicado.
    const claveListo = `estoy_listo_${partidaId}`;
    if (sessionStorage.getItem(claveListo) !== "1") {
      enviarEstoyListo();
      sessionStorage.setItem(claveListo, "1");
    }

    const desconectar = conectarPartida((msg) => {
      switch (msg.tipo) {

        case "TU_TURNO":
          // El servidor nos autoriza a mover primero (por si Ciro lo implementa en el futuro)
          setAguardandoInicio(false);
          break;

        case "MOVER": {
          // El oponente ha movido; reflejamos el movimiento en el tablero local
          const m = msg as RespuestaMover;
          setAguardandoInicio(false); // Nuestro turno comienza
          setEstado((prev) => {
            // Buscar la carta en las del oponente para mantener la rotación FIFO correcta.
            // Si no se encuentra (incompatibilidad de nombres), usar el catálogo local.
            const carta =
              prev.cartasOponente.find((c) => c.nombre === m.carta) ??
              TODAS_LAS_CARTAS.find((c) => c.nombre === m.carta);
            if (!carta) return prev;
            const { nuevoEstado } = ejecutarMovimiento(
              prev, m.fila_origen, m.col_origen, m.fila_destino, m.col_destino, carta,
              equipoJugadorRef.current
            );
            return nuevoEstado;
          });
          const cartaMsg = TODAS_LAS_CARTAS.find((c) => c.nombre === m.carta);
          if (cartaMsg) {
            registrarMovimiento({
              jugador: infoOponente.current.nombre,
              equipo: equipoJugadorRef.current === 2 ? 1 : 2,
              carta: cartaMsg,
              origen: { fila: m.fila_origen, col: m.col_origen },
              destino: { fila: m.fila_destino, col: m.col_destino },
            });
          }
          break;
        }

        case "VICTORIA":
          // El servidor nos declara ganadores de esta partida
          setResultadoFinal({
            ganador: jugadorActual.nombre,
            razon: String((msg as { motivo?: string }).motivo ?? "FIN_PARTIDA"),
          });
          break;

        case "DERROTA":
          // El servidor declara al oponente como ganador
          setResultadoFinal({
            ganador: infoOponente.current.nombre,
            razon: String((msg as { motivo?: string }).motivo ?? "FIN_PARTIDA"),
          });
          break;

        case "TERMINAR_PARTIDA": {
          const t = msg as RespuestaTerminarPartida;
          setResultadoFinal({ ganador: t.ganador, razon: t.razon });
          break;
        }

        case "SOLICITUD_PAUSA": {
          // El rival quiere pausar — mostrar modal para aceptar/rechazar
          setSolicitudPausaEntrante({
            remitente: msg.remitente as string,
            idNotificacion: msg.idNotificacion as number,
          });
          break;
        }

        case "PARTIDA_PAUSADA":
          // La pausa fue aceptada (por mi o por el rival) — volver al menú
          desconectarPartida();
          router.push("/partidas");
          break;

        case "PAUSA_RECHAZADA":
          // El rival rechazó mi solicitud de pausa
          setPausaEnCurso(false);
          setMensajePausaPendiente("Tu rival rechazó la solicitud de pausa.");
          window.setTimeout(() => setMensajePausaPendiente(null), 3500);
          break;
      }
    });

    return desconectar;
  }, [jugadorActual.nombre, registrarMovimiento, router]); // Solo al montar en la practica

  // ─── Timer visual ─────────────────────────────────────────────────────────
  useEffect(() => {
    // No correr el timer si la partida terminó
    if (estado.ganador || resultadoFinal) return;

    // eslint-disable-next-line react-hooks/set-state-in-effect
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
            // En entrenamiento/mock: si se agota el tiempo del jugador, pierde la partida.
            if (estado.turnoActual === equipoJugadorRef.current) {
              setResultadoFinal({
                ganador: infoOponente.current.nombre,
                razon: "TIEMPO_AGOTADO",
              });
              return 0;
            }
            // Si el tiempo se agotara en turno del bot, se rota turno para evitar bloqueo.
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

  // ─── IA del oponente (solo en modo mock / entrenamiento) ──────────────────
  const ejecutarIa = useCallback((est: EstadoJuego) => {
    if (iaOcupada.current) return;
    iaOcupada.current = true;
    const equipoIA: EquipoID = equipoJugadorRef.current === 2 ? 1 : 2;
    setTimeout(() => {
      const jugada = calcularMejorMovimientoIA(
        est,
        equipoIA,
        equipoJugadorRef.current,
        dificultad
      );
      if (!jugada) { iaOcupada.current = false; return; }
      const { nuevoEstado, victoriaPortrono, esReyCapturado } = ejecutarMovimiento(
        est, jugada.origenFila, jugada.origenCol,
        jugada.destinoFila, jugada.destinoCol,
        jugada.carta, equipoJugadorRef.current
      );
      setEstado(nuevoEstado);
      if (nuevoEstado.ganador === equipoIA) {
        setRazonLocalFin(victoriaPortrono ? "TRONO" : esReyCapturado ? "REY_CAPTURADO" : "FIN_PARTIDA");
      }
      registrarMovimiento({
        jugador: infoOponente.current.nombre,
        equipo: equipoIA,
        carta: jugada.carta,
        origen: { fila: jugada.origenFila, col: jugada.origenCol },
        destino: { fila: jugada.destinoFila, col: jugada.destinoCol },
      });
      iaOcupada.current = false;
    }, 600);
  }, [dificultad, registrarMovimiento]);

  // La IA solo corre en modo entrenamiento/mock y cuando le toca al equipo contrario
  useEffect(() => {
    const equipoIA = equipoJugadorRef.current === 2 ? 1 : 2;
    if (!enServidor.current && estado.turnoActual === equipoIA && !estado.ganador) {
      ejecutarIa(estado);
    }
  }, [estado, ejecutarIa]);

  // ─── Interacciones del jugador ─────────────────────────────────────────────

  const handleCelda = (fila: number, col: number) => {
    const miEquipo = equipoJugadorRef.current;
    if (aguardandoInicio || estado.turnoActual !== miEquipo || estado.ganador || resultadoFinal) return;
    const celda = estado.tablero[fila][col];

    // Destino válido → ejecutar y enviar al servidor
    if (
      estado.movimientosValidos.some((m) => m.fila === fila && m.col === col) &&
      estado.fichaSeleccionada &&
      estado.cartaSeleccionada
    ) {
      const { nuevoEstado, victoriaPortrono, esReyCapturado } = ejecutarMovimiento(
        estado,
        estado.fichaSeleccionada.fila,
        estado.fichaSeleccionada.col,
        fila, col,
        estado.cartaSeleccionada,
        equipoJugadorRef.current
      );
      setEstado(nuevoEstado);
      if (nuevoEstado.ganador === miEquipo) {
        setRazonLocalFin(victoriaPortrono ? "TRONO" : esReyCapturado ? "REY_CAPTURADO" : "FIN_PARTIDA");
      }
      registrarMovimiento({
        jugador: jugadorActual.nombre,
        equipo: miEquipo,
        carta: estado.cartaSeleccionada,
        origen: { fila: estado.fichaSeleccionada.fila, col: estado.fichaSeleccionada.col },
        destino: { fila, col },
      });

      // Enviar movimiento al servidor (ignorado si no hay conexión activa)
      enviarMovimiento({
        equipo: miEquipo,
        col_origen: estado.fichaSeleccionada.col,
        fila_origen: estado.fichaSeleccionada.fila,
        col_destino: col,
        fila_destino: fila,
        carta: estado.cartaSeleccionada.nombre,
      });
      return;
    }

    // Ficha propia → seleccionar
    if (celda.ficha?.equipo === miEquipo) {
      const movimientosValidos = estado.cartaSeleccionada
        ? calcularMovimientosValidos(estado.tablero, fila, col, estado.cartaSeleccionada, miEquipo)
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
    const miEquipo = equipoJugadorRef.current;
    if (aguardandoInicio || estado.turnoActual !== miEquipo || estado.ganador || resultadoFinal) return;
    if (estado.cartaSeleccionada?.nombre === carta.nombre) {
      setEstado((prev) => ({ ...prev, cartaSeleccionada: null, movimientosValidos: [] }));
      return;
    }
    const movimientosValidos = estado.fichaSeleccionada
      ? calcularMovimientosValidos(
        estado.tablero, estado.fichaSeleccionada.fila, estado.fichaSeleccionada.col, carta, miEquipo
      )
      : [];
    setEstado((prev) => ({ ...prev, cartaSeleccionada: carta, movimientosValidos }));
  };

  /** Espera a que el servidor persista abandono/victoria en la BD antes de leer el perfil */
  const DELAY_PERFIL_MS = 500;

  /**
   * Tras un breve delay, pide el perfil actualizado y vuelve a /partidas.
   * El delay evita leer la BD antes de que el backend termine de actualizar puntos/cores.
   */
  const volverAPartidas = () => {
    if (tipoPartida !== "PUBLICA") {
      router.push("/partidas");
      return;
    }
    window.setTimeout(() => {
      obtenerPerfil(nombreJugador)
        .then((datos) => guardarSesion(datos))
        .catch(() => {
          /* si falla, /partidas reintenta en useEffect */
        })
        .finally(() => router.push("/partidas"));
    }, DELAY_PERFIL_MS);
  };

  /** El jugador confirma que quiere abandonar: notifica al servidor y vuelve */
  const handleConfirmarAbandonar = () => {
    setMostrarModalAbandono(false);
    if (enServidor.current) {
      enviarAbandonar(equipoJugadorRef.current);
      desconectarPartida();
    }
    volverAPartidas();
  };

  /** Envía la solicitud de pausa al rival y espera su respuesta. */
  const handleConfirmarPausa = () => {
    setMostrarModalPausa(false);
    if (!enServidor.current) return;
    const ok = enviarSolicitarPausa(
      jugadorActual.nombre,
      infoOponente.current.nombre,
      partidaId
    );
    if (ok) {
      setPausaEnCurso(true);
      setMensajePausaPendiente("Solicitud enviada. Esperando respuesta del rival…");
    } else {
      setMensajePausaPendiente("No se pudo enviar la solicitud. Comprueba la conexión.");
      window.setTimeout(() => setMensajePausaPendiente(null), 3500);
    }
  };

  /** El jugador acepta la solicitud de pausa del rival. */
  const handleAceptarPausaEntrante = () => {
    if (!solicitudPausaEntrante) return;
    enviarAceptarPausa(solicitudPausaEntrante.idNotificacion, jugadorActual.nombre);
    setSolicitudPausaEntrante(null);
    // El servidor responderá con PARTIDA_PAUSADA → listener redirige a /partidas
  };

  /** El jugador rechaza la solicitud de pausa del rival. */
  const handleRechazarPausaEntrante = () => {
    if (!solicitudPausaEntrante) return;
    enviarRechazarPausa(solicitudPausaEntrante.idNotificacion, jugadorActual.nombre);
    setSolicitudPausaEntrante(null);
  };

  // ─── Derivados para el render ──────────────────────────────────────────────

  const min = String(Math.floor(tiempoRestante / 60)).padStart(2, "0");
  const seg = String(tiempoRestante % 60).padStart(2, "0");
  const esTurnoJugador = !aguardandoInicio && estado.turnoActual === miEquipoActual && !estado.ganador && !resultadoFinal;

  // Ganador: puede llegar por resultadoFinal (VICTORIA/DERROTA del servidor) o por
  // detección local (ejecutarMovimiento). Ambas fuentes se muestran; resultadoFinal
  // tiene prioridad para el mensaje de texto de la razón.
  const hayFinPartida = resultadoFinal !== null || estado.ganador !== null;

  const esVictoria = resultadoFinal
    ? resultadoFinal.ganador === jugadorActual.nombre
    : estado.ganador === miEquipoActual;

  const razonFin = resultadoFinal?.razon ?? razonLocalFin ?? "FIN_PARTIDA";
  const razonFinLabel =
    razonFin === "ABANDONO"
      ? "RIVAL ABANDONO"
      : razonFin === "TIEMPO_AGOTADO"
        ? "TE QUEDASTE SIN TIEMPO"
      : razonFin === "TRONO"
        ? "LLEGASTE AL TEMPLO RIVAL"
        : razonFin === "REY_CAPTURADO"
          ? "CAPTURASTE EL REY RIVAL"
          : "FIN DE PARTIDA";
  const nombreOponente = infoOponenteUI.nombre;
  const descripcionFin = esVictoria
    ? razonFin === "ABANDONO"
      ? "Tu rival ha abandonado la partida."
      : razonFin === "TIEMPO_AGOTADO"
        ? "Ganaste porque tu rival superó el tiempo límite."
      : razonFin === "TRONO"
        ? "Has ganado por victoria de templo."
        : razonFin === "REY_CAPTURADO"
          ? "Has ganado capturando al rey rival."
          : "¡Excelente partida! Has dominado el tablero."
    : razonFin === "ABANDONO"
      ? "Has abandonado la partida."
      : razonFin === "TIEMPO_AGOTADO"
        ? "Te has tomado demasiado tiempo para pensar."
      : `@${nombreOponente} ha ganado esta vez.`;
  const equipoOponente: EquipoID = miEquipoActual === 1 ? 2 : 1;
  const ultimoMovJugador =
    [...historialMovimientos].reverse().find((m) => m.equipo === miEquipoActual) ?? null;
  const ultimoMovOponente =
    [...historialMovimientos].reverse().find((m) => m.equipo === equipoOponente) ?? null;

  let ayuda = "";
  if (aguardandoInicio) ayuda = "Esperando al servidor para comenzar…";
  else if (esTurnoJugador) {
    if (!estado.fichaSeleccionada && !estado.cartaSeleccionada) ayuda = `Selecciona una de tus piezas (${nombreEquipoJugador.toLowerCase()})`;
    else if (estado.fichaSeleccionada && !estado.cartaSeleccionada) ayuda = "Ahora elige una carta del panel derecho";
    else if (estado.movimientosValidos.length === 0) ayuda = "Sin movimientos válidos. Prueba otra carta o pieza.";
    else ayuda = "Haz clic en una casilla blanca para mover";
  } else if (!hayFinPartida) {
    ayuda = esModoServidor
      ? `Turno de @${nombreOponente}…`
      : `Turno de @${nombreOponente}…`;
  }

  const boardStyle = getBoardStyle(skinActiva);

  return (
    <div className="h-screen flex flex-col overflow-hidden" style={{ background: "#111d2c" }}>

      {/* ═══ HEADER ════════════════════════════════════════════════════════ */}
      <header className="bg-[#1a2d4a] px-5 py-2 flex items-center justify-between shrink-0 shadow-lg">
        <div className="flex items-center">
          <Image src="/nombre.png" alt="Onitama" width={110} height={32} priority className="h-8 w-auto object-contain" style={{ height: '2rem', width: 'auto' }} />
        </div>
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-1">
            <Image src="/katanas.png" alt="Puntos" width={18} height={18} className="h-4 w-auto" />
            <span className="text-white font-semibold text-xs">{jugadorActual.puntos.toLocaleString()}</span>
          </div>
          <div className="flex items-center gap-1">
            <Image src="/core.png" alt="Cores" width={18} height={18} className="h-4 w-auto" />
            <span className="text-white font-semibold text-xs">{jugadorActual.cores.toLocaleString()}</span>
          </div>
          <div className="w-8 h-8 rounded-full bg-[#2a4a6a] border-2 border-white/30 overflow-hidden">
            <AvatarCircle nombre={jugadorActual.nombre} avatarId={jugadorActual.avatar_id} sizeClass="w-full h-full" textClass="text-xs" />
          </div>
        </div>
      </header>

      {/* ═══ MODAL: FIN DE PARTIDA ══════════════════════════════════════════ */}
      {hayFinPartida && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-10 flex flex-col items-center gap-5 shadow-2xl max-w-xs w-full mx-4">
            <div className="relative w-20 h-20 shrink-0">
              <Image
                src={esVictoria ? "/emoteVictoria.png" : "/emoteDerrota.png"}
                alt={esVictoria ? "Victoria" : "Derrota"}
                fill
                className="object-contain"
              />
            </div>
            <h2 className="text-2xl font-bold text-white uppercase tracking-widest text-center">
              {esVictoria ? "¡Victoria!" : "Derrota"}
            </h2>
            <p className="text-white/50 text-xs uppercase tracking-widest">{razonFinLabel}</p>
            <p className="text-white/60 text-sm text-center">
              {descripcionFin}
            </p>
            <button
              type="button"
              onClick={volverAPartidas}
              className="w-full py-3 rounded-xl font-bold uppercase tracking-widest text-sm bg-[#e8e8e8] text-[#1a2d4a] hover:bg-white transition-all"
            >
              Volver a partidas
            </button>
            {!esModoServidor && (
              <button
                type="button"
                onClick={() => {
                  setEstado(crearEstadoInicial());
                  setResultadoFinal(null);
                  setRazonLocalFin(null);
                  setTiempoRestante(TIEMPO_TURNO);
                  setHistorialMovimientos([]);
                  setMiEquipoActual(1);
                  const mock = getMockOponente(dificultad);
                  infoOponente.current = mock;
                  setInfoOponenteUI(mock);
                }}
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

      {/* ═══ MODAL: SOLICITAR PAUSA (PRIVADA) ═══════════════════════════════ */}
      {mostrarModalPausa && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-8 flex flex-col items-center gap-5 shadow-2xl max-w-sm w-full mx-4">
            <span className="text-4xl">⏸️</span>
            <h2 className="text-xl font-bold text-white uppercase tracking-widest text-center">
              ¿Solicitar pausa?
            </h2>
            <p className="text-white/60 text-sm text-center">
              Se enviará una solicitud de pausa a tu rival. Si acepta, la partida quedará pausada para reanudarla más adelante.
            </p>
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={() => setMostrarModalPausa(false)}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm border border-white/20 text-white/70 hover:bg-white/10 transition-colors"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmarPausa}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm bg-amber-700 text-white hover:bg-amber-600 transition-colors"
              >
                Solicitar pausa
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ═══ MODAL: SOLICITUD DE PAUSA ENTRANTE ═════════════════════════════ */}
      {solicitudPausaEntrante && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-sm">
          <div className="bg-[#1a2d4a] border border-white/20 rounded-2xl p-8 flex flex-col items-center gap-5 shadow-2xl max-w-sm w-full mx-4">
            <span className="text-4xl">⏸️</span>
            <h2 className="text-xl font-bold text-white uppercase tracking-widest text-center">
              Solicitud de pausa
            </h2>
            <p className="text-white/60 text-sm text-center">
              <span className="text-white font-semibold">@{solicitudPausaEntrante.remitente}</span> quiere pausar la partida para reanudarla más adelante. ¿Aceptas?
            </p>
            <div className="flex gap-3 w-full">
              <button
                type="button"
                onClick={handleRechazarPausaEntrante}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm border border-white/20 text-white/70 hover:bg-white/10 transition-colors"
              >
                Rechazar
              </button>
              <button
                type="button"
                onClick={handleAceptarPausaEntrante}
                className="flex-1 py-3 rounded-xl font-bold uppercase tracking-widest text-sm bg-amber-700 text-white hover:bg-amber-600 transition-colors"
              >
                Aceptar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ═══ ÁREA DE JUEGO ══════════════════════════════════════════════════ */}
      <div className="flex flex-1 min-h-0 overflow-hidden">

        {/* ─── PANEL IZQUIERDO: oponente ──────────────────────────────────── */}
        {/* El oponente siempre es el equipo contrario al jugador local */}
        {/* Equipo 1 = Azul, Equipo 2 = Rojo */}
        <aside className="w-64 shrink-0 flex flex-col gap-3 px-3 pt-3 pb-2 bg-[#162235] border-r border-white/10 overflow-hidden min-h-0">
          <div className="flex flex-col items-center gap-1">
            <div className={`w-11 h-11 rounded-full border-2 overflow-hidden shrink-0 ${miEquipoActual === 2
              ? "bg-blue-900/60 border-blue-400/40"   // oponente es equipo 1 (azul)
              : "bg-red-900/60 border-red-400/40"     // oponente es equipo 2 (rojo)
              }`}>
              <AvatarCircle
                nombre={nombreOponente}
                avatarId={infoOponenteUI.avatar_id}
                sizeClass="w-full h-full"
                textClass="text-base"
                bgClass="bg-transparent"
              />
            </div>
            <span className="text-white/80 text-[11px] font-semibold">@{nombreOponente}</span>
            <span className="text-white/30 text-[9px]">{infoOponenteUI.puntos} pts</span>
          </div>

          <div className="flex flex-col gap-1.5 shrink-0">
            <p className="text-white/40 text-[9px] uppercase tracking-widest text-center">Cartas del oponente</p>
            {estado.cartasOponente.map((c, i) => (
              // equipo={1}: movimientos invertidos (perspectiva del rival)
              // colorDots: color del rival (opuesto al jugador local)
              <CartaBtn
                key={`${c.nombre}-oponente-${i}`}
                carta={c}
                equipo={1}
                colorDots={getColorMovimiento(
                  skinActiva,
                  miEquipoActual === 1 ? 2 : 1
                )}
                desactivada
              />
            ))}
          </div>

          {/* DEBUG: bloque oculto temporalmente. Descomentar si hace falta revisar movimientos del rival.
          <UltimoMovimientoGhost titulo="Ultimo movimiento rival" mov={ultimoMovOponente} />
          */}

          <div className="flex-1 min-h-2" />

          <div className="flex flex-col items-center gap-1 shrink-0">
            <p className={`text-center text-[10px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded-md ${!aguardandoInicio && estado.turnoActual !== miEquipoActual && !hayFinPartida
              ? miEquipoActual === 2
                ? "text-blue-300 bg-blue-900/30 animate-pulse"
                : "text-red-300 bg-red-900/30 animate-pulse"
              : "text-white/30"
              }`}>
              {aguardandoInicio
                ? "Preparando…"
                : !aguardandoInicio && estado.turnoActual !== miEquipoActual && !hayFinPartida
                  ? `Turno de @${nombreOponente}`
                  : "Esperando…"}
            </p>
            <p className={`font-mono text-2xl font-bold tabular-nums ${tiempoRestante <= 15 && tiempoRestante > 0 ? "text-red-400 animate-pulse" : "text-white/70"
              }`}>
              {min}:{seg}
            </p>
          </div>
        </aside>

        {/* ─── CENTRO: tablero + cola de cartas ───────────────────────────── */}
        <main className="flex-1 bg-[#dbeafe] flex flex-col items-center justify-center gap-2 px-3 min-h-0 min-w-0 overflow-hidden">
          <div className="relative shrink-0" style={{ width: "min(min(52vh, 440px), calc(100vw - 520px))", aspectRatio: "1" }}>
          <div
            className="grid border-2 border-[#1a2d4a]/30 shadow-2xl bg-white w-full h-full"
            style={{
              gridTemplateColumns: `repeat(${DIM}, 1fr)`,
            }}
          >
            {/*
              Cada jugador ve siempre sus piezas abajo y las del rival arriba.
              Para equipo 1: rotación 180° completa (filas Y columnas invertidas).
              Esto es necesario porque calcularMovimientosValidos usa signo=-1 que
              espeja ambos ejes; sin invertir columnas los movimientos válidos aparecerían
              en el lado contrario al que muestra el MiniGrid de la carta.
            */}
            {Array.from({ length: DIM }, (_, filaVisual) => {
              const fila = miEquipoActual === 1 ? (DIM - 1 - filaVisual) : filaVisual;
              return Array.from({ length: DIM }, (_, colVisual) => {
                const col = miEquipoActual === 1 ? (DIM - 1 - colVisual) : colVisual;
                const celda = estado.tablero[fila][col];
                const esSel = estado.fichaSeleccionada?.fila === fila && estado.fichaSeleccionada?.col === col;
                const esValido = estado.movimientosValidos.some((m) => m.fila === fila && m.col === col);
                const esUlt =
                  !!estado.ultimoMovimiento &&
                  ((estado.ultimoMovimiento.origen.fila === fila && estado.ultimoMovimiento.origen.col === col) ||
                    (estado.ultimoMovimiento.destino.fila === fila && estado.ultimoMovimiento.destino.col === col));
                const esBlanca = (fila + col) % 2 === 0;
                const baseClase =
                  boardStyle === "ajedrez"
                    ? (esBlanca ? "bg-stone-900 hover:bg-stone-800" : "bg-stone-100 hover:bg-stone-200")
                    : boardStyle === "clasico-futbol"
                      ? "bg-emerald-700/90 hover:bg-emerald-600/90"
                      : "bg-gray-100 hover:bg-gray-200";
                const bordeClase = boardStyle === "clasico-futbol" ? "border-white/35" : "border-gray-300";
                return (
                  <Celda
                    key={`${fila}-${col}`}
                    ficha={celda.ficha}
                    esTrono={celda.esTrono}
                    equipoTrono={celda.esTrono ? (fila === 0 ? 1 : 2) : null}
                    esSeleccionada={esSel}
                    esMovimientoValido={esValido}
                    esUltimoMov={esUlt && !esSel && !esValido}
                    onClick={() => handleCelda(fila, col)}
                    skinActiva={skinActiva}
                    baseClase={baseClase}
                    bordeClase={bordeClase}
                  />
                );
              });
            })}
          </div>
          {boardStyle === "clasico-futbol" && (
            <svg
              className="pointer-events-none absolute inset-0"
              viewBox="0 0 100 100"
              preserveAspectRatio="none"
              aria-hidden
            >
              <line x1="0" y1="50" x2="100" y2="50" stroke="rgba(255,255,255,0.62)" strokeWidth="0.8" />
              <circle cx="50" cy="50" r="9" fill="none" stroke="rgba(255,255,255,0.62)" strokeWidth="0.8" />
              <circle cx="50" cy="50" r="0.9" fill="rgba(255,255,255,0.8)" />
            </svg>
          )}
          </div>

          <div className="flex items-center justify-center gap-2 shrink-0">
            <p className="text-[#1a2d4a]/70 font-bold text-[9px] uppercase tracking-widest shrink-0">Mazo:</p>
            {estado.cartasSiguientes.map((carta, i) => {
              // Color según el equipo que recibirá esa carta al salir de la cola.
              // i=0 la recibe el equipo que mueve ahora; i=1 el contrario; i=2 el actual...
              const equipoRecibeCarta = (i % 2 === 0)
                ? estado.turnoActual
                : (estado.turnoActual === 1 ? 2 : 1);
              const colorDotsSkin = getColorMovimiento(skinActiva, equipoRecibeCarta as 1 | 2);
              return (
                <CartaCola
                  key={`${carta.nombre}-${i}`}
                  carta={carta}
                  equipo={2}
                  colorDots={colorDotsSkin}
                  esLaSiguiente={i === 0}
                />
              );
            })}
          </div>
        </main>

        {/* ─── PANEL DERECHO: acciones del jugador ────────────────────────── */}
        <aside className="w-64 shrink-0 flex flex-col gap-3 px-3 pt-3 pb-2 bg-[#162235] border-l border-white/10 overflow-hidden min-h-0">
          {/* Botón ABANDONAR (reemplaza PAUSAR) */}
          <button
            type="button"
            onClick={() => setMostrarModalAbandono(true)}
            className="w-full py-1.5 rounded-lg border border-red-800/60 text-red-400/70 text-xs font-bold uppercase tracking-widest hover:bg-red-900/20 hover:text-red-300 transition-colors shrink-0"
          >
            🚩 Abandonar
          </button>

          {tipoPartida === "PRIVADA" && (
            <button
              type="button"
              onClick={() => setMostrarModalPausa(true)}
              disabled={hayFinPartida || pausaEnCurso}
              className="w-full py-1.5 rounded-lg border border-amber-700/60 text-amber-300/80 text-xs font-bold uppercase tracking-widest hover:bg-amber-900/20 hover:text-amber-200 transition-colors shrink-0 disabled:opacity-40 disabled:cursor-not-allowed"
            >
              {pausaEnCurso ? "⏳ Esperando…" : "⏸️ Pausar"}
            </button>
          )}

          {tipoPartida === "PRIVADA" && mensajePausaPendiente && (
            <p className="text-[10px] text-amber-200/80 bg-amber-900/20 border border-amber-700/30 rounded-md px-2 py-1">
              {mensajePausaPendiente}
            </p>
          )}

          <div className="flex flex-col gap-1.5 shrink-0">
            <p className="text-white/40 text-[9px] uppercase tracking-widest text-center">
              {esTurnoJugador ? "Elige una carta" : "Tus cartas"}
            </p>
            {estado.cartasJugador.map((carta, i) => (
              <CartaBtn
                key={`${carta.nombre}-jugador-${i}`}
                carta={carta}
                equipo={2}
                colorDots={getColorMovimiento(skinActiva, miEquipoActual)}
                seleccionada={estado.cartaSeleccionada?.nombre === carta.nombre}
                onClick={() => handleCarta(carta)}
                desactivada={!esTurnoJugador}
              />
            ))}
          </div>

          {/* DEBUG: bloque oculto temporalmente. Descomentar si hace falta revisar movimientos del jugador.
          <UltimoMovimientoGhost titulo="Tu ultimo movimiento" mov={ultimoMovJugador} />
          */}

          <div className="flex-1 min-h-2" />

          <div className="flex flex-col items-center gap-1 shrink-0">
            <p className={`text-center text-[10px] font-bold uppercase tracking-widest px-1.5 py-0.5 rounded-md ${esTurnoJugador
              ? miEquipoActual === 1 ? "text-blue-300 bg-blue-900/30" : "text-red-300 bg-red-900/30"
              : aguardandoInicio ? "text-yellow-300/70 bg-yellow-900/20"
                : "text-white/30"
              }`}>
              {aguardandoInicio ? "Esperando inicio…" : esTurnoJugador ? "¡Es tu turno!" : "Esperando…"}
            </p>
            <div className={`w-9 h-9 rounded-full border-2 overflow-hidden ${miEquipoActual === 1
              ? "bg-blue-900/60 border-blue-400/40"
              : "bg-red-900/60 border-red-400/40"
              }`}>
              <AvatarCircle
                nombre={jugadorActual.nombre}
                avatarId={jugadorActual.avatar_id}
                sizeClass="w-full h-full"
                textClass="text-xs"
                bgClass="bg-transparent"
              />
            </div>
            <span className="text-white/60 text-[10px]">@{jugadorActual.nombre}</span>
            <span className="text-white/30 text-[9px]">{jugadorActual.puntos} pts</span>
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
  const dificultadParam = searchParams.get("dificultad") ?? "medio";
  const modoEntrenamiento = searchParams.get("modo") === "entrenamiento";
  const dificultad: Dificultad = (["facil", "medio", "dificil"] as const).includes(dificultadParam as Dificultad)
    ? (dificultadParam as Dificultad)
    : "medio";
  return (
    <PartidaInterna
      partidaId={partidaId}
      dificultad={dificultad}
      modoEntrenamiento={modoEntrenamiento}
    />
  );
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
