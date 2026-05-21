/**
 * IA COMPETITIVA DE ONITAMA - "IRON BOT GRANDMASTER"
 * 
 * Implementación 'State of the Art' ejecutada en el cliente.
 * Arquitectura basada en motores de ajedrez modernos:
 * - Zobrist Hashing y Tablas de Transposición
 * - Iterative Deepening (Gestión de tiempo estricta)
 * - Move Ordering heurístico (Hash move, Capturas, Centro)
 * - Quiescence Search (Anti-Efecto Horizonte)
 * - Evaluación experta ( Piece-Square Tables, Clustering, Card Denial)
 */

import {
    type EstadoJuego,
    type EquipoID,
    DIM,
    CENTRO,
} from "./juego";
import { type CartaMovDef } from "./cartas";

// ─── Tipos BASE ───────────────────────────────────────────────────────────────

export type Dificultad = "facil" | "medio" | "dificil";

export interface JugadaIA {
    origenFila: number;
    origenCol: number;
    destinoFila: number;
    destinoCol: number;
    carta: CartaMovDef;
}

interface JugadaSim {
    carta: CartaMovDef;
    ox: number; oy: number;
    dx: number; dy: number;
}

// ─── CONFIGURACIÓN DEL MOTOR ──────────────────────────────────────────────────

const TIME_LIMIT_MS: Record<Dificultad, number> = {
    facil: 500,    // 0.5s de reflexión
    medio: 1000,    // 1s de reflexión
    dificil: 2000, // 2s de reflexión (Torneo)
};

// ─── ESTRUCTURAS DE SIMULACIÓN Y ZOBRIST HASHING ──────────────────────────────

interface EstadoSim {
    // Tablero 1D de 49 posiciones para máxima velocidad (y*7 + x)
    // 0=Vacío, 1=Peón1, 2=Peón2, 3=Rey1, 4=Rey2
    board: Uint8Array;

    // Arrays de cartas
    cEq1: CartaMovDef[];
    cEq2: CartaMovDef[];
    cCentro: CartaMovDef;

    hash: number;
}

// Generador pseudo-aleatorio para Zobrist (Seed fijo para consistencia)
let pseudoRandSeed = 0x12345678;
function xorshift32() {
    pseudoRandSeed ^= pseudoRandSeed << 13;
    pseudoRandSeed ^= pseudoRandSeed >> 17;
    pseudoRandSeed ^= pseudoRandSeed << 5;
    return pseudoRandSeed >>> 0;
}

// Zobrist Tables
// [PIEZA 1-4][CASILLA 0-48]
const ZOBRIST_PIECES: number[][] = Array.from({ length: 5 }, () => Array(49).fill(0));
const ZOBRIST_TURN = xorshift32();
// Simplificación: Hash para cartas lo haremos sumando IDs únicos de las cartas por equipo

let isZobristInitialized = false;
function initZobrist() {
    if (isZobristInitialized) return;
    for (let p = 1; p <= 4; p++) {
        for (let i = 0; i < 49; i++) {
            ZOBRIST_PIECES[p][i] = xorshift32();
        }
    }
    isZobristInitialized = true;
}

// Hash liviano y estable para strings.
// Lo usamos para evitar colisiones tontas en la TT por nombres parecidos.
function fnv1a32(str: string, seed: number = 0x811c9dc5): number {
    let h = seed >>> 0;
    for (let i = 0; i < str.length; i++) {
        h ^= str.charCodeAt(i);
        h = Math.imul(h, 0x01000193) >>> 0;
    }
    return h >>> 0;
}

function getCardStrHash(cEq1: CartaMovDef[], cEq2: CartaMovDef[], cCentro: CartaMovDef): number {
    // Importante: no solo cuenta "qué cartas hay", sino también en qué lado están.
    // Mismo set de cartas en manos distintas = estado distinto de partida.
    let h = 0x811c9dc5;
    h = fnv1a32(`C:${cCentro.nombre}|`, h);
    for (const c of cEq1) h = fnv1a32(`1:${c.nombre}|`, h);
    for (const c of cEq2) h = fnv1a32(`2:${c.nombre}|`, h);
    return h >>> 0;
}

function computeZobrist(estado: EstadoSim, turno: number): number {
    let h = 0;
    for (let i = 0; i < 49; i++) {
        const p = estado.board[i];
        if (p > 0) h ^= ZOBRIST_PIECES[p][i];
    }
    if (turno === 2) h ^= ZOBRIST_TURN;

    // Incorporamos el mazo de cartas al hash
    h ^= getCardStrHash(estado.cEq1, estado.cEq2, estado.cCentro);
    return h >>> 0;
}

// ─── TABLA DE TRANSPOSICIÓN ───────────────────────────────────────────────────

enum TTFlag {
    EXACT, ALPHA, BETA
}

interface TTEntry {
    depth: number;
    score: number;
    flag: TTFlag;
    bestMove: JugadaSim | null;
}

const TT = new Map<number, TTEntry>();

function clearTT() {
    TT.clear();
}

// ─── CONVERSIÓN DE ESTADOS ────────────────────────────────────────────────────

function toIndex(x: number, y: number) { return y * DIM + x; }

function crearEstadoSim(est: EstadoJuego, eqLocal: number): EstadoSim {
    initZobrist();
    const board = new Uint8Array(49);
    for (let f = 0; f < DIM; f++) {
        for (let c = 0; c < DIM; c++) {
            const fi = est.tablero[f][c].ficha;
            if (fi) {
                if (fi.esRey) board[toIndex(c, f)] = fi.equipo === 1 ? 3 : 4;
                else board[toIndex(c, f)] = fi.equipo === 1 ? 1 : 2;
            }
        }
    }

    const c1 = eqLocal === 1 ? est.cartasJugador : est.cartasOponente;
    const c2 = eqLocal === 2 ? est.cartasJugador : est.cartasOponente;

    const e: EstadoSim = {
        board,
        cEq1: [...c1],
        cEq2: [...c2],
        cCentro: { ...est.cartasSiguientes[0] },
        hash: 0
    };
    e.hash = computeZobrist(e, 1); // Turno base (se cambiará dinámicamente)
    return e;
}

function cloneSim(e: EstadoSim): EstadoSim {
    return {
        board: new Uint8Array(e.board),
        cEq1: [...e.cEq1],
        cEq2: [...e.cEq2],
        cCentro: { ...e.cCentro },
        hash: e.hash
    };
}

// ─── LOGICA DE TABLERO SIMULADO ───────────────────────────────────────────────

function esAmigo(p: number, eq: number) {
    if (eq === 1) return p === 1 || p === 3;
    if (eq === 2) return p === 2 || p === 4;
    return false;
}
function esEnemigo(p: number, eq: number) {
    if (p === 0) return false;
    return !esAmigo(p, eq);
}

// ─── GENERADOR DE JUGADAS ─────────────────────────────────────────────────────

function generarJugadas(e: EstadoSim, eq: number, soloCapturas: boolean = false): JugadaSim[] {
    const jugadas: JugadaSim[] = [];
    const cartas = eq === 1 ? e.cEq1 : e.cEq2;

    for (const c of cartas) {
        for (let i = 0; i < 49; i++) {
            const p = e.board[i];
            if (!esAmigo(p, eq)) continue;

            const ox = i % DIM;
            const oy = Math.floor(i / DIM);

            for (const mov of c.movimientos) {
                // Keep movement convention aligned with juego.ts:
                // eq=2 -> nc = col + dc, nf = fila - df
                // eq=1 -> nc = col - dc, nf = fila + df
                const dx = eq === 1 ? ox - mov.dc : ox + mov.dc;
                const dy = eq === 1 ? oy + mov.df : oy - mov.df;

                if (dx < 0 || dx >= DIM || dy < 0 || dy >= DIM) continue;

                const target = e.board[toIndex(dx, dy)];
                if (esAmigo(target, eq)) continue;

                if (soloCapturas && target === 0) continue; // Si es quiescence, forzar capturas

                jugadas.push({ carta: c, ox, oy, dx, dy });
            }
        }
    }
    return jugadas;
}

function aplicarJugada(e: EstadoSim, j: JugadaSim, eq: number) {
    const fromIdx = toIndex(j.ox, j.oy);
    const posTo = toIndex(j.dx, j.dy);

    const pzFrom = e.board[fromIdx];
    const pzDest = e.board[posTo];

    // Zobrist Update: Quitar pieza de origen
    e.hash ^= ZOBRIST_PIECES[pzFrom][fromIdx];
    // Zobrist Update: Quitar posible pieza capturada
    if (pzDest !== 0) {
        e.hash ^= ZOBRIST_PIECES[pzDest][posTo];
    }

    // Mover
    e.board[posTo] = pzFrom;
    e.board[fromIdx] = 0;

    // Zobrist Update: Añadir pieza al nuevo destino
    e.hash ^= ZOBRIST_PIECES[pzFrom][posTo];

    // Rotar cartas
    const equipoCartas = eq === 1 ? e.cEq1 : e.cEq2;
    const idx = equipoCartas.findIndex(c => c.nombre === j.carta.nombre);

    // Quitar hash viejo de cartas
    e.hash ^= getCardStrHash(e.cEq1, e.cEq2, e.cCentro);

    if (idx !== -1) {
        const usada = equipoCartas.splice(idx, 1)[0];
        const nueva = e.cCentro;
        equipoCartas.push(nueva);
        e.cCentro = usada;
    }

    // Poner hash nuevo de cartas
    e.hash ^= getCardStrHash(e.cEq1, e.cEq2, e.cCentro);

    // Cambio de turno en Zobrist
    e.hash ^= ZOBRIST_TURN;
}

// ─── EVALUACIÓN HEURÍSTICA EXPERTA ────────────────────────────────────────────

// Piece-Square Tables (0-48 index). Bonus por centro y líneas avanzadas.
const PST_P1 = [
    0, 0, 10, 0, 10, 0, 0,
    0, 10, 15, 20, 15, 10, 0,
    10, 15, 20, 30, 20, 15, 10,
    15, 20, 30, 40, 30, 20, 15,
    10, 20, 35, 50, 35, 20, 10,
    5, 15, 25, 60, 25, 15, 5,
    0, 0, 0, 100, 0, 0, 0
];
// Invertido para P2
const PST_P2 = PST_P1.slice().reverse();

const MATE_SCORE = 100000;

function compruebaTrono(e: EstadoSim): number {
    let r1 = false, r2 = false;
    for (let i = 0; i < 49; i++) {
        if (e.board[i] === 3) {
            r1 = true;
            if (i === toIndex(CENTRO, DIM - 1)) return 1; // Llegó al trono del equipo 2
        }
        if (e.board[i] === 4) {
            r2 = true;
            if (i === toIndex(CENTRO, 0)) return 2; // Llegó al trono del equipo 1
        }
    }
    if (!r1) return 2;
    if (!r2) return 1;
    return 0;
}

function esMovimientoGanadorInmediato(e: EstadoSim, j: JugadaSim, eq: number): boolean {
    const fromPiece = e.board[toIndex(j.ox, j.oy)];
    const destPiece = e.board[toIndex(j.dx, j.dy)];

    // 1) Ganar capturando rey.
    if (eq === 1 && destPiece === 4) return true;
    if (eq === 2 && destPiece === 3) return true;

    // 2) Ganar entrando al templo rival con nuestro rey.
    if (eq === 1 && fromPiece === 3 && j.dx === CENTRO && j.dy === DIM - 1) return true;
    if (eq === 2 && fromPiece === 4 && j.dx === CENTRO && j.dy === 0) return true;

    return false;
}

function tieneVictoriaEnUno(e: EstadoSim, eq: number): boolean {
    // "¿Si me toca jugar ahora, tengo mate/victoria inmediata?"
    // Se usa para detectar blunders y para extender quiescence con amenazas reales.
    const jugadas = generarJugadas(e, eq, false);
    for (const j of jugadas) {
        if (esMovimientoGanadorInmediato(e, j, eq)) return true;
    }
    return false;
}

function generaJugadasTacticas(e: EstadoSim, eq: number): JugadaSim[] {
    const jugadas = generarJugadas(e, eq, false);
    const tacticas: JugadaSim[] = [];

    for (const j of jugadas) {
        const destPiece = e.board[toIndex(j.dx, j.dy)];
        if (destPiece !== 0 || esMovimientoGanadorInmediato(e, j, eq)) {
            tacticas.push(j);
            continue;
        }

        // También tratamos como táctica una jugada que "deja preparada" una victoria en 1.
        // Esto ayuda a que quiescence no corte justo antes del golpe táctico.
        const child = cloneSim(e);
        aplicarJugada(child, j, eq);
        if (compruebaTrono(child) === 0 && tieneVictoriaEnUno(child, eq)) {
            tacticas.push(j);
        }
    }

    return tacticas;
}

function evaluate(e: EstadoSim, eqTurno: number): number {
    const estadoTrono = compruebaTrono(e);
    if (estadoTrono !== 0) {
        return estadoTrono === eqTurno ? MATE_SCORE : -MATE_SCORE;
    }

    let scoreP1 = 0;
    let scoreP2 = 0;

    // 1. Material & PST
    for (let i = 0; i < 49; i++) {
        const p = e.board[i];
        if (p === 0) continue;

        let pVal = 0;
        if (p === 1) pVal = 100 + PST_P1[i];
        else if (p === 3) pVal = 1000 + PST_P1[i] * 0.5; // El rey valora menos el PST temprano
        else if (p === 2) pVal = 100 + PST_P2[i];
        else if (p === 4) pVal = 1000 + PST_P2[i] * 0.5;

        if (p === 1 || p === 3) scoreP1 += pVal;
        else scoreP2 += pVal;
    }

    // 2. Clustering (Defensa mutua)
    // Bonus ligero si los peones están adyacentes a amigos
    for (let y = 0; y < DIM; y++) {
        for (let x = 0; x < DIM; x++) {
            const idx = toIndex(x, y);
            const p = e.board[idx];
            if (p === 0) continue;

            let amigosCerca = 0;
            for (let dy = -1; dy <= 1; dy++) {
                for (let dx = -1; dx <= 1; dx++) {
                    if (dx === 0 && dy === 0) continue;
                    const nx = x + dx; const ny = y + dy;
                    if (nx >= 0 && nx < DIM && ny >= 0 && ny < DIM) {
                        const adj = e.board[toIndex(nx, ny)];
                        if (esAmigo(adj, p === 1 || p === 3 ? 1 : 2)) amigosCerca++;
                    }
                }
            }
            if (p === 1 || p === 3) scoreP1 += amigosCerca * 8;
            else scoreP2 += amigosCerca * 8;
        }
    }

    // 3. Card Mobility
    scoreP1 += (e.cEq1[0].movimientos.length + e.cEq1[1].movimientos.length) * 5;
    scoreP2 += (e.cEq2[0].movimientos.length + e.cEq2[1].movimientos.length) * 5;

    return eqTurno === 1 ? (scoreP1 - scoreP2) : (scoreP2 - scoreP1);
}

// ─── SEARCH LOGIC (Move Ordering, Quiescence, AlfaBeta) ───────────────────────

function scoreMove(e: EstadoSim, m: JugadaSim, eq: number, hashMove: JugadaSim | null): number {
    if (hashMove && m.carta.nombre === hashMove.carta.nombre && m.dx === hashMove.dx && m.dy === hashMove.dy && m.ox === hashMove.ox) {
        return 1000000; // Prioridad absoluta al PV (Principal Variation) de la pasada anterior
    }

    let score = 0;
    const destPiece = e.board[toIndex(m.dx, m.dy)];

    // Prioridad a capturas (MVV-LVA approx)
    if (destPiece !== 0) {
        score += 10000;
        if (destPiece === 3 || destPiece === 4) score += 50000; // Capturar rey!
    }

    // Prioridad a ganar por trono
    if (e.board[toIndex(m.ox, m.oy)] === 3 && m.dx === CENTRO && m.dy === DIM - 1) score += 90000;
    if (e.board[toIndex(m.ox, m.oy)] === 4 && m.dx === CENTRO && m.dy === 0) score += 90000;

    // Prioridad posicional (ir hacia el centro)
    const pst = eq === 1 ? PST_P1 : PST_P2;
    score += pst[toIndex(m.dx, m.dy)] - pst[toIndex(m.ox, m.oy)];

    return score;
}

// Variable de escape suave por tiempo agotado
let timeIsUp = false;
let endTime = 0;

function quiescence(e: EstadoSim, alpha: number, beta: number, eqTurno: number): number {
    if (Date.now() >= endTime) { timeIsUp = true; return 0; }

    const stand_pat = evaluate(e, eqTurno);
    if (stand_pat >= beta) return beta;
    if (alpha < stand_pat) alpha = stand_pat;

    // Antes solo extendíamos capturas.
    // Ahora extendemos capturas + amenazas inmediatas al rey/templo.
    const tacticalMoves = generaJugadasTacticas(e, eqTurno);
    tacticalMoves.sort((a, b) => scoreMove(e, b, eqTurno, null) - scoreMove(e, a, eqTurno, null));
    for (const j of tacticalMoves) {
        const child = cloneSim(e);
        aplicarJugada(child, j, eqTurno);

        const score = -quiescence(child, -beta, -alpha, eqTurno === 1 ? 2 : 1);
        if (timeIsUp) return 0;

        if (score >= beta) return beta;
        if (score > alpha) alpha = score;
    }
    return alpha;
}

function minimaxAB(e: EstadoSim, depth: number, alpha: number, beta: number, eqTurno: number): number {
    if (Date.now() >= endTime) { timeIsUp = true; return 0; }

    const originalAlpha = alpha;
    const hash = e.hash;

    // Transposition Table lookup
    const ttEntry = TT.get(hash);
    if (ttEntry && ttEntry.depth >= depth) {
        if (ttEntry.flag === TTFlag.EXACT) return ttEntry.score;
        if (ttEntry.flag === TTFlag.ALPHA && ttEntry.score <= alpha) return alpha;
        if (ttEntry.flag === TTFlag.BETA && ttEntry.score >= beta) return beta;
    }

    const estadoGameOver = compruebaTrono(e);
    if (estadoGameOver !== 0) {
        return estadoGameOver === eqTurno ? MATE_SCORE + depth : -(MATE_SCORE + depth);
    }

    if (depth <= 0) {
        return quiescence(e, alpha, beta, eqTurno);
    }

    const jugadas = generarJugadas(e, eqTurno, false);
    if (jugadas.length === 0) return evaluate(e, eqTurno); // Ahogado? No deberia pasar

    // Move Ordering
    jugadas.sort((a, b) => scoreMove(e, b, eqTurno, ttEntry?.bestMove || null) - scoreMove(e, a, eqTurno, ttEntry?.bestMove || null));

    let bestMove: JugadaSim | null = null;
    let bScore = -Infinity;

    for (const j of jugadas) {
        const child = cloneSim(e);
        aplicarJugada(child, j, eqTurno);

        const score = -minimaxAB(child, depth - 1, -beta, -alpha, eqTurno === 1 ? 2 : 1);
        if (timeIsUp) return 0; // abortar

        if (score > bScore) {
            bScore = score;
            bestMove = j;
            if (score > alpha) alpha = score;
            if (alpha >= beta) break; // Prune
        }
    }

    if (!timeIsUp) {
        // Save to TT
        let flag = TTFlag.EXACT;
        if (bScore <= originalAlpha) flag = TTFlag.ALPHA;
        else if (bScore >= beta) flag = TTFlag.BETA;
        TT.set(hash, { depth, score: bScore, flag, bestMove });
    }

    return bScore;
}


// ─── API PÚBLICA (INTERFAZ EXPORTADA) ─────────────────────────────────────────

export function calcularMejorMovimientoIA(
    estado: EstadoJuego,
    equipoIA: EquipoID,
    equipoLocal: EquipoID,
    dificultad: Dificultad
): JugadaIA | null {
    console.log(`\n[Iron Bot] Motor GRANDMASTER iniciado. Nivel: ${dificultad.toUpperCase()}`);

    const rootSim = crearEstadoSim(estado, equipoLocal);

    // Clear TT to prevent memory leak and stale collision across moves
    clearTT();

    timeIsUp = false;
    endTime = Date.now() + TIME_LIMIT_MS[dificultad];

    let globalBestMove: JugadaSim | null = null;
    let maxDepthReached = 0;
    const rivalEq: EquipoID = equipoIA === 1 ? 2 : 1;

    // ITERATIVE DEEPENING
    // Limitamos a Profundidad Maxima 15 para evitar loops infinitos teóricos, pero el tiempo cortará antes
    for (let currentDepth = 1; currentDepth <= 15; currentDepth++) {
        const lastTick = Date.now();
        if (lastTick >= endTime) break;

        // Start search for this depth
        const jugadas = generarJugadas(rootSim, equipoIA);
        if (jugadas.length === 0) break;

        // Ordenamos las jugadas raiz basandonos en el TT (el mejor mov de la prof anterior)
        const rootTT = TT.get(rootSim.hash);
        jugadas.sort((a, b) => scoreMove(rootSim, b, equipoIA, rootTT?.bestMove || null) - scoreMove(rootSim, a, equipoIA, rootTT?.bestMove || null));

        let currentDepthBest: JugadaSim | null = null;
        let alpha = -Infinity;
        const beta = Infinity;

        for (const j of jugadas) {
            const child = cloneSim(rootSim);
            aplicarJugada(child, j, equipoIA);

            const score = -minimaxAB(child, currentDepth - 1, -beta, -alpha, equipoIA === 1 ? 2 : 1);
            if (timeIsUp) break; // Cortar el for interno. Ya no nos fiamos de este nivel

            let adjustedScore = score;
            // Cinturón de seguridad: si esta jugada deja al rival ganar en 1,
            // la hundimos en score aunque minimax no haya llegado a verlo.
            if (compruebaTrono(child) === 0 && tieneVictoriaEnUno(child, rivalEq)) {
                adjustedScore -= 75000;
            }

            if (adjustedScore > alpha) {
                alpha = adjustedScore;
                currentDepthBest = j;
            }
        }

        if (timeIsUp) {
            console.log(`Tiempo agotado calculando P${currentDepth}. Se descarta este nivel.`);
            break;
        }

        // Si completamos el nivel limpio, guardamos la mejor jugada asegurada
        if (currentDepthBest) {
            globalBestMove = currentDepthBest;
            maxDepthReached = currentDepth;
            TT.set(rootSim.hash, { depth: currentDepth, score: alpha, flag: TTFlag.EXACT, bestMove: currentDepthBest });

            // Si encontramos jaque mate absoluto a favor, paramos (MATE_SCORE es enorme)
            if (alpha > 90000) {
                console.log(`MATE detectado en ${currentDepth} movimientos! Cortando bucle.`);
                break;
            }
        }
    }

    console.log(`Jugada completada. Profundidad real máxima alcanzada: ${maxDepthReached}. TT Size: ${TT.size} nodos.`);

    if (!globalBestMove) {
        // Fallback absoluto por si algo raro pasa en T=0
        const fallbacks = generarJugadas(rootSim, equipoIA);
        if (fallbacks.length === 0) return null;
        globalBestMove = fallbacks[Math.floor(Math.random() * fallbacks.length)];
    }

    return {
        origenFila: globalBestMove.oy,
        origenCol: globalBestMove.ox,
        destinoFila: globalBestMove.dy,
        destinoCol: globalBestMove.dx,
        carta: globalBestMove.carta,
    };
}
