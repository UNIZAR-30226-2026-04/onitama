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

import { CartaMovDef, seleccionarCartasAleatorias, TODAS_LAS_CARTAS } from "./cartas";

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
  /**
   * 1 | 2 = trampa activa de ese equipo (aún no disparada).
   * -1    = trampa disparada (casilla injugable).
   * null  = sin trampa.
   */
  esTrampaEquipo?: number | null;
}

export type FasePartida = "COLOCAR_TRAMPA" | "ELEGIR_CARTA_ACCION" | "JUGANDO" | "TERMINADA";

export interface CartaAccionJuego {
  nombre: string;
  accion: string;
  /** Estado persistido por backend: USABLE, ACTIVA, NO_USABLE, etc. */
  estado?: string;
}

export interface EstadoJuego {
  fasePartida: FasePartida;
  opcionesCartasAccion: { nombre: string; accion: string }[];
  cartaAccionPropia: CartaAccionJuego | null;
  cartaAccionRival: CartaAccionJuego | null;
  /** Carta que abrió el modo de acción (puede ser la segunda en mano) */
  cartaAccionParaModo: CartaAccionJuego | null;
  /** Estado de interacción visual para jugar una carta de acción */
  modoAccion?: "REVIVIR" | "SALVAR_REY" | "SACRIFICIO_PROPIO" | "SACRIFICIO_RIVAL" | "ROBAR" | null;
  /** Datos temporales recogidos durante el modoAccion */
  accionParams?: {
    x?: number; y?: number; x_op?: number; y_op?: number; cartaRobar?: string;
  };
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
  /** Equipo ciego durante toda la partida por Brujería (null = sin efecto activo) */
  equipoCiego: EquipoID | null;
  /**
   * Dama del Mar / Finisterre: restricción solo para quien toque mover con el efecto.
   * Se limpia cuando el rival del jugador que jugó la carta mueve (o cadena acción→tú→rival).
   */
  restriccionSolo: RestriccionSolo | null;
  /**
   * Equipo que jugó Pensatorium (ESPEJO). El servidor deshace el espejo cuando mueve el rival;
   * el cliente debe invertir otra vez las cartas en ese momento para coincidir con el servidor.
   */
  espejoActivadoPor: EquipoID | null;
}

/** Estado de la restricción adelante/atrás (sincronizado con Partida.java) */
export interface RestriccionSolo {
  tipo: "SOLO_PARA_ADELANTE" | "SOLO_PARA_ATRAS";
  /** Quien jugó la carta Dama/Finisterre */
  caster: EquipoID;
  /** Quien debe obedecer la restricción en su siguiente movimiento */
  equipoAfectado: EquipoID;
}

export function transferirRestriccionSoloSiJuegaAccion(
  r: RestriccionSolo | null,
  equipoQueJuegaAccion: EquipoID
): RestriccionSolo | null {
  if (!r || r.equipoAfectado !== equipoQueJuegaAccion) return r;
  const otro: EquipoID = equipoQueJuegaAccion === 1 ? 2 : 1;
  return { ...r, equipoAfectado: otro };
}

export function resolverRestriccionSoloTrasMovimiento(
  r: RestriccionSolo | null,
  equipoQueMueve: EquipoID
): RestriccionSolo | null {
  if (!r || r.equipoAfectado !== equipoQueMueve) return r;
  const rivalDelCaster: EquipoID = r.caster === 1 ? 2 : 1;
  if (equipoQueMueve === rivalDelCaster) {
    return null;
  }
  if (equipoQueMueve === r.caster) {
    return { ...r, equipoAfectado: rivalDelCaster };
  }
  return r;
}

export function activarRestriccionSolo(
  caster: EquipoID,
  tipo: "SOLO_PARA_ADELANTE" | "SOLO_PARA_ATRAS"
): RestriccionSolo {
  return {
    tipo,
    caster,
    equipoAfectado: caster === 1 ? 2 : 1,
  };
}

/** Misma transformación que al aplicar ESPEJO (invertir dc en cada vector). */
export function invertirCartasEspejo(cartas: CartaMovDef[]): CartaMovDef[] {
  return cartas.map((ca) => ({
    ...ca,
    movimientos: (ca.movimientos || []).map((m) => ({ dc: -m.dc, df: m.df })),
  }));
}

/**
 * Si el servidor habría deshecho ESPEJO en este movimiento (mueve quien no jugó Pensatorium),
 * alinea las cartas del cliente con Partida.moverFicha.
 */
export function deshacerEspejoTrasMovimientoRival(
  estado: EstadoJuego,
  equipoQueMueve: EquipoID
): EstadoJuego {
  const c = estado.espejoActivadoPor;
  if (c === null || equipoQueMueve === c) return estado;
  return {
    ...estado,
    cartasJugador: invertirCartasEspejo(estado.cartasJugador),
    cartasOponente: invertirCartasEspejo(estado.cartasOponente),
    cartasSiguientes: invertirCartasEspejo(estado.cartasSiguientes),
    espejoActivadoPor: null,
  };
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
        esTrampaEquipo: null,
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
    fasePartida: "JUGANDO", // En mock saltamos directo a jugar
    opcionesCartasAccion: [],
    cartaAccionPropia: null,
    cartaAccionRival: null,
    cartaAccionParaModo: null,
    tablero: crearTableroInicial(),
    turnoActual: 1,
    cartasJugador: [cartas[0], cartas[1]],
    cartasOponente: [cartas[2], cartas[3]],
    cartasSiguientes: [cartas[4], cartas[5], cartas[6]], // cola FIFO de 3
    fichaSeleccionada: null,
    cartaSeleccionada: null,
    movimientosValidos: [],
    ganador: null,
    ultimoMovimiento: null,
    equipoCiego: null,
    restriccionSolo: null,
    espejoActivadoPor: null,
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
  equipo: EquipoID,
  restriccionSolo?: RestriccionSolo | null
): { fila: number; col: number }[] {
  const signo = equipo === 2 ? 1 : -1;
  const validos: { fila: number; col: number }[] = [];

  for (const { dc, df } of carta.movimientos) {
    if (
      restriccionSolo &&
      restriccionSolo.equipoAfectado === equipo
    ) {
      if (restriccionSolo.tipo === "SOLO_PARA_ADELANTE" && df < 0) continue;
      if (restriccionSolo.tipo === "SOLO_PARA_ATRAS" && df > 0) continue;
    }
    const nf = fila - df * signo;
    const nc = col + dc * signo;

    if (nf < 0 || nf >= DIM || nc < 0 || nc >= DIM) continue;
    if (tablero[nf][nc].ficha?.equipo === equipo) continue;
    // Las celdas de trampa disparada son injugables
    if (tablero[nf][nc].esTrampaEquipo === -1) continue;

    validos.push({ fila: nf, col: nc });
  }

  return validos;
}

// ─── Creación del estado a partir de datos del servidor ──────────────────────

/**
 * Formato de carta tal como la envía el servidor en PARTIDA_ENCONTRADA.
 * El servidor incluye los movimientos con coordenadas {x, y} sacadas de la BD.
 * Convención: x = delta de columna (dc), y = delta de fila (df).
 * Ambos valores son equivalentes a los dc/df del catálogo local (cartas.ts).
 */
export interface CartaServidor {
  nombre: string;
  movimientos: { x: number; y: number }[];
}

export interface CartaAccionServidor {
  nombre: string;
  accion?: string;
  estado?: string;
  equipo?: number;
}

/**
 * Convierte una carta del formato servidor al formato frontend.
 * Si el servidor no envía movimientos (retrocompatibilidad mock), se busca
 * por nombre en el catálogo local.
 */
function convertirCarta(carta: CartaServidor | string): CartaMovDef {
  // Formato antiguo: solo nombre (mock o versión anterior del servidor)
  if (typeof carta === "string") {
    const encontrada = TODAS_LAS_CARTAS.find((c) => c.nombre === carta);
    if (!encontrada) {
      console.warn(`[juego] Carta "${carta}" no encontrada en catálogo. Usando primera disponible.`);
      return TODAS_LAS_CARTAS[0];
    }
    return encontrada;
  }

  // Formato nuevo: objeto con nombre y movimientos del servidor
  const emoji = TODAS_LAS_CARTAS.find((c) => c.nombre === carta.nombre)?.emoji ?? "🃏";
  return {
    nombre: carta.nombre,
    emoji,
    // El servidor usa (x,y) que equivale a (dc,df) del frontend
    movimientos: carta.movimientos.map((m) => ({ dc: m.x, df: m.y })),
  };
}

/**
 * Construye el estado inicial de la partida usando los datos recibidos del servidor
 * en el mensaje PARTIDA_ENCONTRADA.
 *
 * El servidor envía las cartas con sus movimientos (x,y) ya calculados desde la BD.
 * Acepta tanto el formato nuevo { nombre, movimientos } como el antiguo string (mock).
 *
 * Convención de turnos: equipo 1 siempre empieza (Ciro no envía TU_TURNO,
 * la pantalla de partida lo gestiona con aguardandoInicio según el equipo asignado).
 *
 * @param datos  Datos del mensaje PARTIDA_ENCONTRADA
 */
/**
 * Parsea las cadenas de posición del servidor (formato Java Tablero) en un tablero frontend.
 * Formato eq1/eq2: "[x,y]" = rey, "(x,y)" = peón. x=col, y=fila.
 */
function tableroDesdeServidor(eq1: string, eq2: string, trampaJ1?: string | null, trampaJ2?: string | null): Celda[][] {
  const tablero: Celda[][] = Array.from({ length: DIM }, (_, fila) =>
    Array.from({ length: DIM }, (_, col) => ({
      ficha: null,
      esTrono: (fila === 0 && col === CENTRO) || (fila === DIM - 1 && col === CENTRO),
      esTrampaEquipo: null,
    }))
  );

  const colocar = (str: string, equipo: EquipoID) => {
    // Rey: [x,y]
    const reyRe = /\[(-?\d+),(-?\d+)\]/g;
    let m;
    while ((m = reyRe.exec(str)) !== null) {
      const col = parseInt(m[1]), fila = parseInt(m[2]);
      if (fila >= 0 && fila < DIM && col >= 0 && col < DIM)
        tablero[fila][col].ficha = { equipo, esRey: true };
    }
    // Peones: (x,y)
    const peonRe = /\((-?\d+),(-?\d+)\)/g;
    while ((m = peonRe.exec(str)) !== null) {
      const col = parseInt(m[1]), fila = parseInt(m[2]);
      if (fila >= 0 && fila < DIM && col >= 0 && col < DIM)
        tablero[fila][col].ficha = { equipo, esRey: false };
    }
    // Trampas: |x,y,activa|  activa=1 → sin disparar, activa=0 → disparada (injugable)
    const trampaRe = /\|(-?\d+),(-?\d+),(\d+)\|/g;
    while ((m = trampaRe.exec(str)) !== null) {
      const col = parseInt(m[1]), fila = parseInt(m[2]);
      const activa = parseInt(m[3]);
      if (fila >= 0 && fila < DIM && col >= 0 && col < DIM) {
        if (activa === 1) {
          tablero[fila][col].esTrampaEquipo = equipo; // trampa activa, aún no disparada
        } else if (activa === 0) {
          tablero[fila][col].esTrampaEquipo = -1; // trampa disparada → casilla injugable
        }
      }
    }
  };

  const aplicarTrampaGuardada = (pos: string | null | undefined, equipo: EquipoID) => {
    if (!pos) return;
    const [colRaw, filaRaw, activaRaw] = pos.split(",");
    const col = Number.parseInt(colRaw, 10);
    const fila = Number.parseInt(filaRaw, 10);
    const activa = activaRaw === undefined ? 1 : Number.parseInt(activaRaw, 10);
    if (Number.isNaN(col) || Number.isNaN(fila)) return;
    if (fila >= 0 && fila < DIM && col >= 0 && col < DIM) {
      tablero[fila][col].esTrampaEquipo = activa === 0 ? -1 : equipo;
    }
  };

  colocar(eq1, 1);
  colocar(eq2, 2);
  aplicarTrampaGuardada(trampaJ1, 1);
  aplicarTrampaGuardada(trampaJ2, 2);
  return tablero;
}

function normalizarCartaAccion(carta: { nombre: string; accion?: string; estado?: string } | null | undefined): CartaAccionJuego | null {
  if (!carta) return null;
  return {
    nombre: carta.nombre,
    accion: carta.accion ?? carta.nombre,
    estado: carta.estado,
  };
}

function cartaAccionEnIndice(cartas: CartaAccionServidor[] | undefined, indice: number): CartaAccionJuego | null {
  return normalizarCartaAccion(cartas?.[indice]);
}

export function crearEstadoDesdeServidor(datos: {
  cartas_jugador: (CartaServidor | string)[];
  cartas_oponente: (CartaServidor | string)[];
  carta_siguiente: (CartaServidor | string)[];
  equipo?: 1 | 2;
  tablero_eq1?: string;
  tablero_eq2?: string;
  trampa_j1_pos?: string | null;
  trampa_j2_pos?: string | null;
  /** Turno numérico del servidor: par=equipo1, impar=equipo2 */
  turno?: number;
  /** Cartas de acción (para partidas reanudadas). Puede venir como array [propia, rival]
   *  o como campos individuales carta_accion_propia / carta_accion_rival. */
  cartas_accion?: { nombre: string; accion: string }[];
  carta_accion_propia?: { nombre: string; accion: string } | null;
  carta_accion_rival?: { nombre: string; accion: string } | null;
  cartas_accion_jugador?: CartaAccionServidor[];
  cartas_accion_oponente?: CartaAccionServidor[];
}): EstadoJuego {
  const tieneTableroGuardado = datos.tablero_eq1 && datos.tablero_eq2;
  const tablero = tieneTableroGuardado
    ? tableroDesdeServidor(datos.tablero_eq1!, datos.tablero_eq2!, datos.trampa_j1_pos, datos.trampa_j2_pos)
    : crearTableroInicial();

  // Si viene turno del servidor lo usamos; si no, equipo 1 empieza siempre.
  const turnoActual: EquipoID =
    datos.turno !== undefined ? (datos.turno % 2 === 0 ? 1 : 2) : 1;

  // Restaurar cartas de acción para partidas reanudadas.
  // En la UI cartaAccionPropia/cartaAccionRival representan las dos cartas de acción
  // que tiene el jugador local (puede usar solo una); el nombre "Rival" se mantiene
  // por compatibilidad con el estado anterior.
  const cartaAccionPropia =
    normalizarCartaAccion(datos.carta_accion_propia ?? null) ??
    cartaAccionEnIndice(datos.cartas_accion_jugador, 0) ??
    normalizarCartaAccion(datos.cartas_accion?.[0] ?? null) ??
    null;
  const cartaAccionRival =
    normalizarCartaAccion(datos.carta_accion_rival ?? null) ??
    cartaAccionEnIndice(datos.cartas_accion_jugador, 1) ??
    normalizarCartaAccion(datos.cartas_accion?.[1] ?? null) ??
    null;

  return {
    fasePartida: tieneTableroGuardado ? "JUGANDO" : "COLOCAR_TRAMPA",
    opcionesCartasAccion: [],
    cartaAccionPropia,
    cartaAccionRival,
    cartaAccionParaModo: null,
    tablero,
    turnoActual,
    cartasJugador: datos.cartas_jugador.map(convertirCarta),
    cartasOponente: datos.cartas_oponente.map(convertirCarta),
    cartasSiguientes: datos.carta_siguiente.map(convertirCarta),
    fichaSeleccionada: null,
    cartaSeleccionada: null,
    movimientosValidos: [],
    ganador: null,
    ultimoMovimiento: null,
    equipoCiego: null,
    restriccionSolo: null,
    espejoActivadoPor: null,
  };
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
/**
 * equipoLocal: equipo del jugador local en esta pestaña (1 o 2).
 * Determina qué array (cartasJugador / cartasOponente) recibe la carta nueva
 * de la cola tras un movimiento, independientemente de quién mueva en este turno.
 * Por defecto 2 para mantener compatibilidad con el modo mock.
 */
export function ejecutarMovimiento(
  estado: EstadoJuego,
  origenFila: number,
  origenCol: number,
  destinoFila: number,
  destinoCol: number,
  carta: CartaMovDef,
  equipoLocal: EquipoID = 2,
  trampaActivada?: boolean
): ResultadoMovimiento {
  // Copia profunda del tablero
  const tablero: Celda[][] = estado.tablero.map((fila) =>
    fila.map((celda) => ({
      ...celda,
      ficha: celda.ficha ? { ...celda.ficha } : null,
    }))
  );

  const fichaMovida = tablero[origenFila][origenCol].ficha!;
  let esReyCapturado = false;
  let capturado = false;

  const esTrampaOponente = tablero[destinoFila][destinoCol].esTrampaEquipo && tablero[destinoFila][destinoCol].esTrampaEquipo !== (fichaMovida.equipo as EquipoID);

  if (trampaActivada || esTrampaOponente) {
    // La ficha muere y la celda queda injugable (-1 = disparada / calavera)
    tablero[destinoFila][destinoCol].ficha = null;
    tablero[destinoFila][destinoCol].esTrampaEquipo = -1;
    tablero[origenFila][origenCol].ficha = null;
  } else {
    const fichaDestino = tablero[destinoFila][destinoCol].ficha;
    capturado = fichaDestino !== null;
    esReyCapturado = capturado && fichaDestino!.esRey;

    tablero[destinoFila][destinoCol].ficha = fichaMovida;
    tablero[origenFila][origenCol].ficha = null;
  }

  // Victoria por trono: el rey llega al trono enemigo
  const victoriaPortrono =
    fichaMovida.esRey &&
    ((fichaMovida.equipo === 2 && destinoFila === 0 && destinoCol === CENTRO) ||
      (fichaMovida.equipo === 1 && destinoFila === DIM - 1 && destinoCol === CENTRO));

  // ─── Rotación de la cola ──────────────────────────────────────────────────
  const equipoActual = estado.turnoActual;

  // Detectar si el jugador que mueve tiene 3 cartas (situación post-Atrapasueños/ROBAR).
  // En ese caso la carta usada va al final del mazo (que tenía 2 → queda con 3),
  // y el jugador no recibe ninguna carta nueva (ya tenía una extra).
  const cartasMovedor =
    equipoActual === equipoLocal ? estado.cartasJugador : estado.cartasOponente;
  const tieneCartaExtra = cartasMovedor.length > 2;

  let cartaRecibida: CartaMovDef | undefined;
  let nuevasSiguientes: CartaMovDef[];
  const colaActual = estado.cartasSiguientes.filter(Boolean);

  if (tieneCartaExtra) {
    // Post-ROBAR: la carta usada pasa al final; nadie recibe carta del mazo (2→3 cartas en cola).
    nuevasSiguientes = [...colaActual, carta];
  } else {
    // Rotación normal FIFO: el jugador recibe la primera de la cola; la usada va al final.
    cartaRecibida = colaActual[0];
    nuevasSiguientes = [...colaActual.slice(1), carta];
  }

  // Si el equipo que mueve ahora ES el jugador local → actualizar cartasJugador.
  // Si el equipo que mueve ahora ES el oponente      → actualizar cartasOponente.
  const nuevasCartasJugador =
    equipoActual === equipoLocal
      ? tieneCartaExtra
        ? estado.cartasJugador.filter((c) => c.nombre !== carta.nombre)
        : estado.cartasJugador.map((c) =>
            c.nombre === carta.nombre ? (cartaRecibida ?? c) : c
          )
      : [...estado.cartasJugador];

  const nuevasCartasOponente =
    equipoActual !== equipoLocal
      ? tieneCartaExtra
        ? estado.cartasOponente.filter((c) => c.nombre !== carta.nombre)
        : estado.cartasOponente.map((c) =>
            c.nombre === carta.nombre ? (cartaRecibida ?? c) : c
          )
      : [...estado.cartasOponente];

  const ganador: EquipoID | null =
    esReyCapturado || victoriaPortrono ? equipoActual : null;

  const nuevoEstado: EstadoJuego = {
    ...estado,
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
    equipoCiego: estado.equipoCiego,
    restriccionSolo: resolverRestriccionSoloTrasMovimiento(estado.restriccionSolo, equipoActual),
  };

  return { nuevoEstado, capturado, esReyCapturado, victoriaPortrono };
}
