/**
 * Tipos y lógica del juego Onitama (versión básica sin cartas de acción).
 * Tablero 7×7. 7 cartas de movimiento por partida (2 jugador + 2 oponente + 3 en cola).
 *
 * La cola de cartas funciona como FIFO:
 *  - Al jugar una carta, el jugador recibe la primera carta de la cola.
 *  - La carta usada se añade al final de la cola.
 *  - Así la cola siempre tiene 3 cartas.
 *
 * Condiciones de victoria:
 *  1. Capturar el rey enemigo.
 *  2. Llegar con el rey propio al trono enemigo.
 *
 * TODO (servidor): cuando el servidor implemente la lógica de movimiento,
 * esta lógica local pasará a ser solo visual (para resaltar casillas válidas).
 * El servidor validará y ejecutará los movimientos reales.
 */

import { CartaMovDef, seleccionarCartasAleatorias } from "./cartas";

// ─── Constantes del tablero ───────────────────────────────────────────────────

export const DIM = 7;
export const CENTRO = Math.floor(DIM / 2); // Columna del trono: 3

// ─── Tipos ────────────────────────────────────────────────────────────────────

export type EquipoID = 1 | 2;

export interface Ficha {
  equipo: EquipoID;
  esRey: boolean;
}

export interface Celda {
  ficha: Ficha | null;
  /** true si esta casilla es el trono de algún equipo */
  esTrono: boolean;
}

export interface EstadoJuego {
  tablero: Celda[][];
  turnoActual: EquipoID;
  /** 2 cartas del jugador local (equipo 2) */
  cartasJugador: CartaMovDef[];
  /** 2 cartas del oponente (equipo 1) */
  cartasOponente: CartaMovDef[];
  /**
   * Cola de 3 cartas en espera.
   * - cartasSiguientes[0] es la que recibirá quien juegue.
   * - La carta usada se añade al final (índice 2).
   */
  cartasSiguientes: CartaMovDef[];
  /** Ficha que el jugador ha pulsado (highlight amarillo) */
  fichaSeleccionada: { fila: number; col: number } | null;
  /** Carta que el jugador ha seleccionado (activa las casillas azules) */
  cartaSeleccionada: CartaMovDef | null;
  /** Casillas destino válidas según la ficha y carta seleccionadas */
  movimientosValidos: { fila: number; col: number }[];
  /** Equipo ganador; null mientras la partida siga en curso */
  ganador: EquipoID | null;
  /** Origen y destino del último movimiento (para feedback visual) */
  ultimoMovimiento: {
    origen: { fila: number; col: number };
    destino: { fila: number; col: number };
  } | null;
}

// ─── Creación del estado inicial ──────────────────────────────────────────────

/** Construye el tablero 7×7 con fichas en posición inicial */
export function crearTableroInicial(): Celda[][] {
  return Array.from({ length: DIM }, (_, fila) =>
    Array.from({ length: DIM }, (_, col) => {
      let ficha: Ficha | null = null;
      if (fila === 0 || fila === DIM - 1) {
        const equipo: EquipoID = fila === 0 ? 1 : 2;
        ficha = { equipo, esRey: col === CENTRO };
      }
      return {
        ficha,
        esTrono: (fila === 0 || fila === DIM - 1) && col === CENTRO,
      };
    })
  );
}

/**
 * Crea el estado inicial de una partida local (mock).
 * Se reparten 7 cartas aleatorias: 2 jugador + 2 oponente + 3 en cola.
 */
export function crearEstadoInicial(): EstadoJuego {
  const cartas = seleccionarCartasAleatorias(7);
  return {
    tablero: crearTableroInicial(),
    turnoActual: 2,
    cartasJugador: [cartas[0], cartas[1]],
    cartasOponente: [cartas[2], cartas[3]],
    cartasSiguientes: [cartas[4], cartas[5], cartas[6]], // cola FIFO de 3
    fichaSeleccionada: null,
    cartaSeleccionada: null,
    movimientosValidos: [],
    ganador: null,
    ultimoMovimiento: null,
  };
}

// ─── Cálculo de movimientos válidos ───────────────────────────────────────────

/**
 * Devuelve las casillas a las que puede moverse la ficha en (fila, col)
 * utilizando la carta dada.
 *
 * Convención de dirección:
 *  - Equipo 2 (abajo): df+ avanza hacia fila 0 → new_row = fila − df
 *  - Equipo 1 (arriba): invierte ambos ejes → new_row = fila + df, new_col = col − dc
 */
export function calcularMovimientosValidos(
  tablero: Celda[][],
  fila: number,
  col: number,
  carta: CartaMovDef,
  equipo: EquipoID
): { fila: number; col: number }[] {
  const signo = equipo === 2 ? 1 : -1;
  const validos: { fila: number; col: number }[] = [];

  for (const { dc, df } of carta.movimientos) {
    const nf = fila - df * signo;
    const nc = col + dc * signo;

    if (nf < 0 || nf >= DIM || nc < 0 || nc >= DIM) continue;
    if (tablero[nf][nc].ficha?.equipo === equipo) continue;

    validos.push({ fila: nf, col: nc });
  }

  return validos;
}

// ─── Ejecución de un movimiento ───────────────────────────────────────────────

export interface ResultadoMovimiento {
  nuevoEstado: EstadoJuego;
  capturado: boolean;
  esReyCapturado: boolean;
  victoriaPortrono: boolean;
}

/**
 * Ejecuta el movimiento indicado y rota la cola de cartas:
 *  1. El jugador activo pierde la carta usada.
 *  2. Recibe cartasSiguientes[0] (la primera de la cola).
 *  3. La carta usada pasa al final de la cola.
 *  4. La cola queda con 3 cartas de nuevo.
 *
 * TODO (servidor): cuando el servidor esté listo, esta función solo se usará
 * para actualizar el estado visual tras recibir la confirmación del servidor
 * via mensaje WebSocket tipo MOVER.
 */
export function ejecutarMovimiento(
  estado: EstadoJuego,
  origenFila: number,
  origenCol: number,
  destinoFila: number,
  destinoCol: number,
  carta: CartaMovDef
): ResultadoMovimiento {
  // Copia profunda del tablero
  const tablero: Celda[][] = estado.tablero.map((fila) =>
    fila.map((celda) => ({
      ...celda,
      ficha: celda.ficha ? { ...celda.ficha } : null,
    }))
  );

  const fichaMovida = tablero[origenFila][origenCol].ficha!;
  const fichaDestino = tablero[destinoFila][destinoCol].ficha;
  const capturado = fichaDestino !== null;
  const esReyCapturado = capturado && fichaDestino!.esRey;

  tablero[destinoFila][destinoCol].ficha = fichaMovida;
  tablero[origenFila][origenCol].ficha = null;

  // Victoria por trono: el rey llega al trono enemigo
  const victoriaPortrono =
    fichaMovida.esRey &&
    ((fichaMovida.equipo === 2 && destinoFila === 0 && destinoCol === CENTRO) ||
      (fichaMovida.equipo === 1 && destinoFila === DIM - 1 && destinoCol === CENTRO));

  // ─── Rotación de la cola ──────────────────────────────────────────────────
  // El jugador recibe la primera carta de la cola; la carta usada va al final.
  const cartaRecibida = estado.cartasSiguientes[0];
  const nuevasSiguientes: CartaMovDef[] = [
    estado.cartasSiguientes[1],
    estado.cartasSiguientes[2],
    carta, // La carta jugada pasa al final de la cola
  ];

  const equipoActual = estado.turnoActual;
  const nuevasCartasJugador =
    equipoActual === 2
      ? estado.cartasJugador.map((c) =>
          c.nombre === carta.nombre ? cartaRecibida : c
        )
      : [...estado.cartasJugador];

  const nuevasCartasOponente =
    equipoActual === 1
      ? estado.cartasOponente.map((c) =>
          c.nombre === carta.nombre ? cartaRecibida : c
        )
      : [...estado.cartasOponente];

  const ganador: EquipoID | null =
    esReyCapturado || victoriaPortrono ? equipoActual : null;

  const nuevoEstado: EstadoJuego = {
    tablero,
    turnoActual: equipoActual === 2 ? 1 : 2,
    cartasJugador: nuevasCartasJugador,
    cartasOponente: nuevasCartasOponente,
    cartasSiguientes: nuevasSiguientes,
    fichaSeleccionada: null,
    cartaSeleccionada: null,
    movimientosValidos: [],
    ganador,
    ultimoMovimiento: {
      origen: { fila: origenFila, col: origenCol },
      destino: { fila: destinoFila, col: destinoCol },
    },
  };

  return { nuevoEstado, capturado, esReyCapturado, victoriaPortrono };
}
