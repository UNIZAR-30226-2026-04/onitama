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

package com.example.onitama.lib

// ─── Tipos BASE ───────────────────────────────────────────────────────────────

enum class Dificultad {
    FACIL,
    MEDIO,
    DIFICIL
}

data class JugadaIA (
    val origenFila: Int,
    val origenCol: Int, 
    val destinoFila: Int,
    val destinoCol: Int,
    val carta: CartaMovDef
)

data class JugadaSim (
    val carta: CartaMovDef,
    val origenX: Int,
    val origenY: Int,
    val destinoX: Int,
    val destinoY: Int
)

// ─── CONFIGURACIÓN DEL MOTOR ──────────────────────────────────────────────────

val TIME_LIMIT_MS = mapOf {
    Dificultad.FACIL to 500,
    Dificultad.MEDIO to 1000,
    Dificultad.DIFICIL to 2000
}

// ─── ESTRUCTURAS DE SIMULACIÓN Y ZOBRIST HASHING ──────────────────────────────

data class EstadoSim (
    // Tablero 1D de 49 posiciones para máxima velocidad (y*7 + x)
    // 0=Vacío, 1=Peón1, 2=Peón2, 3=Rey1, 4=Rey2
    val board: IntArray,

    // Arrays de cartas
    val cEq1: MutableList<CartaMovDef>,
    val cEq2: MutableList<CartaMovDef>,
    val cCentro: CartaMovDef,

    var hash: Int
)

// Generador pseudo-aleatorio para Zobrist (Seed fijo para consistencia)
var pseudoRandSeed = 0x12345678
fun xorshift32(): Int {
    pseudoRandSeed = pseudoRandSeed xor (pseudoRandSeed shl 13)
    pseudoRandSeed = pseudoRandSeed xor (pseudoRandSeed shr 17)
    pseudoRandSeed = pseudoRandSeed xor (pseudoRandSeed shl 5)
    return pseudoRandSeed
}

// Zobrist Tables
// [PIEZA 1-4][CASILLA 0-48]
val ZOBRIST_PIECES: Array<IntArray> = Array(5) { IntArray(49) }
val ZOBRIST_TURN = xorshift32()

// Simplificación: Hash para cartas lo haremos sumando IDs únicos de las cartas por equipo

var isZobristInitialized = false

fun initZobrist() {
    if (isZobristInitialized) {
        return
    }

    var p = 1

    while (p <= 4) {
        var i = 0

        while (i < 49) {
            ZOBRIST_PIECES[p][i] = xorshift32()
            i = i + 1 
        }

        p = i + 1
    }
    isZobristInitialized = true
}

// Hash liviano y estable para strings.
// Lo usamos para evitar colisiones tontas en la TT por nombres parecidos.
fun fnv1a32 (
    str: String,
    seed: Int = 0x811c9dc5
): Int {
    var h = seed
    var i = 0

    while (i < str.lenght){
        h = h xor str[i].code
        h = h * 0x01000193
        i = i + 1
    }

    return h
}

fun getCardStrHah (
    cEq1: List<CartaMovDef>,
    cEq2: List<CartaMovDef>,
    cCentro: CartaMovDef
) : Int {
    // Importante: no solo cuenta "qué cartas hay", sino también en qué lado están.
    // Mismo set de cartas en manos distintas = estado distinto de partida.
    var h = 0x811c9dc5
    h = fnv1a32 ("C:${cCentro.nombre}|", h)
    var i = 0

    while (i < cEq1.size) {
        h = fnv1a32("1:${cEq1[i].nombre}|", h)
        i = i + 1
    }

    i = 0

    while (i < cEq2.size) {
        h = fnv1a32("2:${cEq2[i].nombre}|", h)
        i = i + 1
    }

    return h
}

fun computeZobrist (
    estado: EstadoSim,
    turno: Int
) : Int {
    var h = 0
    var i = 0

    while (i < 49) {
        val p = estado.board[i]
        if (p > 0){
            h = h xor ZOBRIST_PIECES[p][i]
        }

        i = i + 1
    }

    if (turno == 2) {
        h = h xor ZOBRIST_TURN
    }

    // Incorporamos el mazo de cartas al hash   
    h = h xor getCardStrHash(estado.cEq1, estado.cEq2, estado.cCentro)
    return h
}


// ─── TABLA DE TRANSPOSICIÓN ───────────────────────────────────────────────────

enum class TTFlag {
    EXACT,
    ALPHA,
    BETA
}

data class TTEntry {
    val depth: Int,
    val score: Int, 
    val flag: TTFlag,
    val bestMove: JugadaSim?
}

val TT = mutableMapOf<Int, TTEntry>()

fun clearTT() {
    TT.clear()
}

// ─── CONVERSIÓN DE ESTADOS ────────────────────────────────────────────────────

fun toIndex (
    x: Int,
    y: Int
) {
    var resultado = 0
    resultado = y * DIM + x

    return resultado
}

fun crearEstadoSim (
    est: EstadoJuego,
    eqLocal: Int
) : EstadoSim {
    initZobrist()

    val board = Array(49) { 0 }
    var f = 0
    
    while (f < DIM) {
        var c = 0

        while (c < DIM) {
            val fi = est.tablero[f][c].ficha

            if (fi != null) {
                if (fi.esRey){
                    if (fi.equipo == 1) {
                        board[toIndex(c, f)] = 3
                    } 
                    else {
                        board[toIndex(c, f)] = 4
                    }
                }
                else {
                    if (fi.equipo == 1) {
                        board[toIndex(c, f)] = 1 
                    }
                    else {
                        board[toIndex(c, f)] = 2
                    }
                }
            }

            c = c + 1
        }

        f = f + 1
    }

    var c1
    var c2

    if (eqLocal == 1) {
        c1 = est.cartasJugador
    }  
    else {
        c1 =  est.cartasOponente
    }

    if (eqLocal == 2) {
        c2 = est.cartasJugador 
    }
    else {
        c2 = est.cartasOponente
    }

    val e: EstadoSim = {
        board = board,
        cEq1: c1.toMutableList(),
        cEq2: c2.toMutableList,
        cCentro: est.cartasSiguientes[0],
        hash: 0
    }
    e.hash = computeZobrist(e, 1) // Turno base (se cambiará dinámicamente)
    return e
}

fun cloneSim (
    e: EstadoSim
) : EstadoSim {
    return EstadoSim {
        board: e.board.copyOf(),
        cEq1 = e.cEq1.toMutableList(),
        cEq2 = e.cEq2.toMutableList(),
        cCentro = e.cCentro,
        hash = e.hash
    }
}


// ─── LOGICA DE TABLERO SIMULADO ───────────────────────────────────────────────

fun esAmigo (
    p: Int,
    eq: Int
) : Boolean {
    if (eq == 1) {
        return p == 1
    }
    else {
        return p == 3
    }

    if (eq == 2) {
        return p == 2
    } 
    else {
        return p == 4
    }
    
    return false
}

fun esEnemigo (
    p: Int,
    eq : Int
) : Boolean {
    if (p == 0) {
        return false
    }
    return !esAmigo(p, eq)
}


// ─── GENERADOR DE JUGADAS ─────────────────────────────────────────────────────

fun generarJugadas (
    e: EstadoSim,
    eq: Int
    soloCapturas: Boolean = false
) : List<JugadasSim> {
    val jugadas: mutableListOf<JugadaSim>()
    var cartas
    
    if (eq == 1) {
        cartas = e.cEq1
    } 
    else {
        cartas = e.cEq2
    } 

    var c = 0
    while (c < cartas.size) {
        val carta = cartas[c]
        var i = 0

        while (i <= 48) {
            val p = e.board[i]
            if(!esAmigo(p, eq)){
                i = i +`1
                continue
            }

            val origenX = i % DIM
            val origenY = i / DIM

            var mov = 0
            while (mov < c.movimientos.size) {
                val movimiento = c.movimientos[mov]

                // Keep movement convention aligned with juego.ts:
                // eq=2 -> nc = col + dc, nf = fila - df
                // eq=1 -> nc = col - dc, nf = fila + df
                var destinoX 
                var destinoY 

                if (eq == 1) {
                    destinoX = origenX - mov.dc
                }
                else {
                    destinoX = origenX + mov.dc
                }

                if (eq == 1) { 
                    destinoY = origenY - mov.df 
                }
                else {
                    destinoY = origenY - mov.df
                }
            
                if (destinoX < 0 || destinoX >= DIM || destinoY < 0 || destinoY >= DIM) {
                    mov = mov + 1
                    continue
                }

                val target = e.board[toIndex(destinoX, destinoY)]
                if (esAmigo(target, eq)){
                    mov = mov + 1
                    continue
                }

                if (soloCapturas && target == 0) {
                    mov = mov + 1
                    continue
                }

                jugadas.add(JugadaSim(
                    carta, 
                    origenX, 
                    origenY, 
                    destinoX, 
                    destinoY
                ))
            }
            i = i + 1
        }
        c = c + 1
    }
    return jugadas
}

fun aplicarJugada(
    e: EstadoSim,
    j: JugadaSim, 
    eq: Int
) {
    val fromIdx = toIndex(j.origenX, j.origenY)
    val posTo = toIndex(j.destinoX, j.destinoY)

    val pzFrom = e.board[fromIdx]
    val pzDest = e.board[posTo]
    
    // Zobrist Update: Quitar pieza de origen
    e.hash = e.hash xor ZOBRIST_PIECES[pzFrom][fromIdx]
    // Zobrist Update: Quitar posible pieza capturada

    if (pzDest != 0) {
        e.hash = e.hash xor ZOBRIST_PIECES[pzDest][posTo]
    }

    // Mover
    e.board[posTo] = pzFrom
    e.board[from.Idx] = 0

    // Zobrist Update: Añadir pieza al nuevo destino
    e.hash = e.hash xor ZOBRIST_PIECES[pzFrom][posTo]

    // Rotar cartas 
    var equipoCartas

    if (eq == 1){
        equipoCartas = e.cEq1
    } 
    else {
        equipoCartas = e.cEq2
    }

    val idx = equipoCartas.indexOfFirst { it.nombre == j.carta.nombre }

    // Quitar hash viejo de cartas 
    e.hash = e.hash xor getCardStrHash(e.cEq1, e.cEq2, e.cCentro)

    if (idx != -1) {
        val usada = equipoCartas.removeAt(idx)
        val nueva = e.cCentro
        equipoCartas.add(nueva)
        e.cCentro = usada
    }

    // Poner hash nuevo de cartas
    e.hash = e.hash xor getCardStrHash(e.cEq1, e.cEq2, e.cCentro)

    // Cambio de turno en Zobrist
    e.hash = e.hash xor ZOBRIST_TURN
}


// ─── EVALUACIÓN HEURÍSTICA EXPERTA ────────────────────────────────────────────

// Piece-Square Tables (0-48 index). Bonus por centro y líneas avanzadas.
val PST_P1 = intArrayOf(
    0, 0, 10, 0, 10, 0, 0,
    0, 10, 15, 20, 15, 10, 0,
    10, 15, 20, 30, 20, 15, 10,
    15, 20, 30, 40, 30, 20, 15,
    10, 20, 35, 50, 35, 20, 10,
    5, 15, 25, 60, 25, 15, 5,
    0, 0, 0, 100, 0, 0, 0
)

// Invertido para P2
val PST_P2 = PST_P1.reversedArray()

const val MATE_SCORE = 100000

fun compruebaTrono ( 
    e: EstadoSim
) : Int {
    var r1 = false
    var r2 = false 
    var i = 0

    while (i < 49) {
        if (e.board[i] == 3) {
            r1 = true 
            if (i == toIndex(CENTRO, DIM - 1)){
                return 1  // Llegó al trono del equipo 2
            }
        }

        if (e.board[i] == 4) {
            r2 = true 
            if (i == toIndex(CENTRO, 0)){
                return 2  // Llegó al trono del equipo 1
            }
        }
        
        i = i + 1
    }

    if (!r1) {
        return 2
    }

    if (!r2) {
        return 1
    }

    return 0
}

fun esMovimientoGanadorInmediato (
    e: EstadoSim,
    j: JugadaSim
    eq: Int
) : Boolean {
    val fromPiece = e.board[toIndex(j.origenX, j.origenY)]
    val destPiece = e.board[toIndex(j.destinoX, j.destinoY)]

    // 1) Ganar capturando rey.
    if (eq == 1 && destPiece == 4) {
        return true
    }

    if (eq == 2 && destPiece == 3) {
        return true
    }

    // 2) Ganar entrando al templo rival con nuestro rey.
    if (eq == 1 && fromPiece == 3 && j.destinoX == CENTRO && j.destinoY == DIM - 1) {
        return true
    }

     if (eq == 1 && fromPiece == 3 && j.destinoX == CENTRO && j.destinoY == 0) {
        return true
    }

    return false
}

fun tieneVictoriaEnUno (
    e: EstadoSim,
    eq: Int
) : Boolean {
    // "¿Si me toca jugar ahora, tengo mate/victoria inmediata?"
    // Se usa para detectar blunders y para extender quiescence con amenazas reales.
    val jugadas = generarJugadas(e, eq, false)

    var jugada = 0

    while (jugada < jugadas.size) {
        if (esMovimientoGanadorInmediato(e, jugadas[jugada], eq)) {
            return true
        }

        jugada = jugada + 1
    }
    
    return false
}

fun generaJugadasTacticas (
    e: EstadoSim,
    eq: Int
) : List<JugadaSim> {
    val jugadas = generarJugadas(e, eq, false)
    val tacticas = mutableListOf<JugadaSim>()

    var jugada = 0

    while (jugada < jugadas.size) {
        val j = jugadas[jugada]
        val destPiece = e.board[toIndex(j.dx, j.dy)]
        
        if (destPiece !== 0 || esMovimientoGanadorInmediato(e, j, eq)) {
            tacticas.add(j)
            jugada = jugada + 1
            continue
        }

        // También tratamos como táctica una jugada que "deja preparada" una victoria en 1.
        // Esto ayuda a que quiescence no corte justo antes del golpe táctico.
        val child = cloneSim(e)
        aplicarJugada(child, j, eq)
        if (compruebaTrono(child) === 0 && tieneVictoriaEnUno(child, eq)) {
            tacticas.add(j)
        }

        jugada = jugada + 1
    }

    return tacticas
}

fun evaluate (
    e: EstadoSim,
    eqTurno : Int
) : Int {
    val estadoTrono = compruebaTrono(e)

    if (estadoTrono !== 0) {
        if (estadoTrono === eqTurno)  {
            return MATE_SCORE
        } 
        else { 
            return -MATE_SCORE
        }
    }

    var scoreP1 = 0
    var scoreP2 = 0

    // 1. Material & PST
    var i = 0

    while (i < 49) {
        val p = e.board[i]
        if (p == 0) {
            i = i + 1
            continue
        }

        var pVal = 0

        if (p == 1) {
            pVal = 100 + PST_P1[i]
        }
        else if (p == 2) {
            pVal = 100 + PST_P2[i]
        }
        else if (p == 3) {
            pVal = 1000 + (PST_P1[i] * 0.5).toInt()
        }
        else if (p == 4){
            pVal = 1000 + (PST_P2[i] * 0.5).toInt()
        }
        else {
            pVal = 0
        }

        if (p == 1 || p == 3) {
            scoreP1 += pVal
        }
        else {
            scoreP2 += pVal
        }

        i = i + 1
    }

    
    // 2. Clustering (Defensa mutua)
    // Bonus ligero si los peones están adyacentes a amigos
    var y = 0

    while (y < DIM) {
        var x = 0

        while (x < DIM) {
            val indice = toIndex(x, y)
            val p = e.board[indice]
            if (p == 0) {
                x = x + 1
                continue
            }

            var amigosCerca = 0
            var dy = -1

            while (dy <= 1) {
                var dx = -1 

                while (dx <= 1) {
                    if (dx == 0 && dy == 0) {
                        dx = dx + 1
                        continue
                    }

                    val nx = x + dx
                    val ny = y + dy

                    if (nx >= 0 && nx < DIM && ny >= 0 && ny < DIM) {
                        val adj = e.board[toIndex(nx, ny)]

                        var equipo = 0

                        if (p == 1 || p == 3) {
                            equipo = 1
                        }
                        else {
                            equipo = 2
                        }

                        if (esAmigo(adj, equipo)) {
                            amigosCerca++
                        }
                    }

                    dx = dx + 1
                }

                dy = dy + 1
            }
            
            if (p == 1 || p == 3) {
                scoreP1 += amigosCerca * 8
            }
            else {
                scoreP2 += amigosCerca * 8
            }

            x = x + 1
        }

        y = y + 1
    }

    // 3. Card Mobility
    scoreP1 += (e.cEq1[0].movimientos.size + e.cEq1[1].movimientos.size) * 5
    scoreP2 += (e.cEq2[0].movimientos.size + e.cEq2[1].movimientos.size) * 5

    if (eqTurno == 1) {
        return (scoreP1 - scoreP2)
    }
    else {
        return (scoreP2 - scoreP1)
    }
}

// ─── SEARCH LOGIC (Move Ordering, Quiescence, AlfaBeta) ───────────────────────

fun scoreMove (
    e: EstadoSim,
    m: JugadaSim,
    eq: Int,
    hashMove: JugadaSim?
) : Int {
    if (hashMove  != null && m.carta.nombre == hashMove.carta.nombre && m.destinoX == hashMove.destinoX && m.destinoY == hashMove.destinoY && m.origenX === hashMove.origenX) {
        return 1000000 // Prioridad absoluta al PV (Principal Variation) de la pasada anterior
    }

    var score = 0
    val destPiece = e.board[toIndex(m.dx, m.dy)]

    // Prioridad a capturas (MVV-LVA approx)
    if (destPiece !== 0) {
        score += 10000
        if (destPiece === 3 || destPiece === 4) {
            score += 50000 // Capturar rey!
        }
    }

    // Prioridad a ganar por trono
    if (e.board[toIndex(m.ox, m.oy)] === 3 && m.dx === CENTRO && m.dy === DIM - 1) {
        score += 90000
    }

    if (e.board[toIndex(m.ox, m.oy)] === 4 && m.dx === CENTRO && m.dy === 0) {
        score += 90000
    }

    // Prioridad posicional (ir hacia el centro)
    var pst 
    if (eq == 1) {
        pst = PST_P1
    }
    else {
        pst = PST_P2
    }

    score += pst[toIndex(m.dx, m.dy)] - pst[toIndex(m.ox, m.oy)]

    return score
}

// Variable de escape suave por tiempo agotado
var timeIsUp = false
var endTime: Long = 0

fun quiescence (
    e: EstadoSim,
    alpha: Int,
    beta: Int,
    eqTurno: Int
) : Int {
    if (System.currentTimeMillis() >= endTime) {
        timeIsUp = true
        return 0
    }

    val stand_pat = evaluate(e, eqTurno)
    if (stand_pat >= beta) {
        return beta
    }

    var alpha = alpha
    if (alpha < stand_pat) {
        alpha = stand_pat
    }

    // Antes solo extendíamos capturas.
    // Ahora extendemos capturas + amenazas inmediatas al rey/templo.
    val tacticalMoves = generaJugadasTacticas(e, eqTurno)
    
    val sortedMove = tacticalMoves.sortedByDescending { scoreMove(e, it, eqTurno, null) }
    var jugada = 0

    while (jugada < sortedMoves.size) {
        val j = sortedMoves[jugada]
        val child = cloneSim(e)
        aplicarJugada(child, j, eqTurno)

        var equipo = 0

        if (eqTurno == 1) {
            equipo = 2
        }
        else {
            equipo = 1
        }

        val score = -quiescence(child, -beta, -alpha, equipo)

        if (timeIsUp) {
            return 0
        }

        if (score >= beta) {
            return beta
        }

        if (score > alpha) {
            alpha = score
        }

        jugada = jugada + 1
    }

    return alpha
}

fun minimaxAB (
    e: EstadoSim,
    depth: Int,
    alpha: Int, 
    beta: Int,
    eqTurno: Int
) : Int {
    if (System.currentTimeMillis() >= endTime) {
        timeIsUp = true
        return 0
    }

    val originalAlpha = alpha
    val hash = e.hash

    // Transposition Table lookup
    val ttEntry = TT(hash)
    if (ttEntry && ttEntry.depth >= depth) {
        if (ttEntry.flag === TTFlag.EXACT) {
            return ttEntry.score
        }

        if (ttEntry.flag === TTFlag.ALPHA && ttEntry.score <= alpha) {
            return alpha
        } 

        if (ttEntry.flag === TTFlag.BETA && ttEntry.score >= beta) {
            return beta
        }
    }

    val estadoGameOver = compruebaTrono(e)
    if (estadoGameOver != 0) {
        if (estadoGameOver == eqTurno) {
            return MATE_SCORE + depth
        }
        else {
            return -(MATE_SCORE + depth)
        }
    }

    if (depth <= 0) {
        return quiescence(e, alpha, beta, eqTurno)
    }

    val jugadas = generarJugadas(e, eqTurno, false)
    if (jugadas.isEmpty()) {
        return evaluate(e, eqTurno) // Ahogado? No deberia pasar
    }

    // Move Ordering
    val sortedJugadas = jugadas.sortedByDescending { scoreMove(e, it, eqTurno, ttEntry?.bestMove) }

    var bestMove: JugadaSim? = null
    var bScore = Int.MIN_VALUE

    var jugada = 0

    while (jugada < sortedJugadas.size) {
        val j = sortedJugadas[jugada]
        val child = cloneSim(e)

        aplicarJugada(child, j, eqTurno)

        var equipo = 0

        if (eqTurno == 1) {
            equipo = 2
        }
        else {
            equipo = 1
        }

        val score = -minimaxAB(child, depth, -1, -beta, -alpha, equipo)

        if (timeIsUp) {
            return 0 // abortar
        }

        if (score > bScore) {
            bScore = score
            bestMove = j 
            if (score > alpha) {
                alpha = score
            }
            
            if (alpha >= beta) {
                break // Prune
            }
        }

        jugada = jugada + 1
    }

    if (!timeIsUp) {
        // Save to TT
        val flag = TTFlag.EXACT
        
        if (bScore <= originalAlpha) {
            flag = TTFlag.ALPHA
        } 
        else if (bScore >= beta) {
            flag = TTFlag.BETA
        }
        TT[hash] = TTEntry(depth, bScore, flag, bestMove);
    }

    return bScore;
}

// ─── API PÚBLICA (INTERFAZ EXPORTADA) ─────────────────────────────────────────

fun calcularMejorMovimientoIA (
    estado: EstadoJuego,
    equipoIA: EquipoID,
    equipoLocal: EquipoID,
    dificultad: Dificultad
) : JugadaIA? {
   println("\n[Iron Bot] Motor GRANDMASTER iniciado. Nivel: ${dificultad.name}")

    val rootSim = crearEstadoSim(estado, equipoLocal)

    // Clear TT to prevent memory leak and stale collision across moves
    clearTT()

    timeIsUp = false
    endTime = System.currentTimeMillis() + TIME_LIMIT_MS[dificultad]!!

    val globalBestMove: JugadaSim? = null
    val maxDepthReached = 0

    var equipo = 0
    if (equipoIA == 1){
        equipo = 2
    }
    else {
        equipo = 1
    }

    val rivalEq: EquipoID = equipo

    // ITERATIVE DEEPENING
    // Limitamos a Profundidad Maxima 15 para evitar loops infinitos teóricos, pero el tiempo cortará antes
    var currentDepth = 1

    while (currentDepth <= 15) {
        val lastTick = System.currentTimeMillis()

        if (lastTick >= endTime) {
            break
        }

        // Start search for this depth
        val jugadas = generarJugadas(rootSim, equipoIA)
        
        if (jugadas.isEmpty()) {
            break
        }

        // Ordenamos las jugadas raiz basandonos en el TT (el mejor mov de la prof anterior)
        val rootTT = TT[rootSim.hash]
        val sortedJugadas = jugadas.sortedByDescending { scoreMove(rootSim, it, equipoIA, rootTT?.bestMove) }

        var currentDepthBest: JugadaSim? = null
        var alpha = Int.MIN_VALUE;
        val beta = Int.MAX_VALUE;

        var jugada = 0

        while (jugada < sortedJugadas.size) {
            val j = sortedJugadas[jugada]
            val child = cloneSim(rootSim)

            aplicarJugada(child, j, equipoIA)

            var equipo = 0

            if (equipoIA == 1) {
                equipo = 2
            }
            else {
                equipo = 1
            }

            val score = -minimaxAB(child, currentDepth - 1, -beta, -alpha, equipo)

            if (timeIsUp) {
                break // Cortar el for interno. Ya no nos fiamos de este nivel
            }

            var adjustedScore = score
            // Cinturón de seguridad: si esta jugada deja al rival ganar en 1,
            // la hundimos en score aunque minimax no haya llegado a verlo.
            
            if (compruebaTrono(child) === 0 && tieneVictoriaEnUno(child, rivalEq)) {
                adjustedScore -= 75000
            }
        
            if (adjustedScore > alpha) {
                alpha = adjustedScore
                currentDepthBest = j
            }

            jugada = jugada + 1
        }

        if (timeIsUp) {
            println("Tiempo agotado calculando P${currentDepth}. Se descarta este nivel.")
            break
        }

        // Si completamos el nivel limpio, guardamos la mejor jugada asegurada
        if (currentDepthBest != null) {
            globalBestMove = currentDepthBest
            maxDepthReached = currentDepth
            TT[rootSim.hash] = TTEntry(currentDepth, alpha, TTFlag.EXACT, currentDepthBest)

            // Si encontramos jaque mate absoluto a favor, paramos (MATE_SCORE es enorme)
            if (alpha > 90000) {
                println("MATE detectado en ${currentDepth} movimientos! Cortando bucle.")
                break
            }
        }

        currentDepth = currentDepth + 1
    }

    println("Jugada completada. Profundidad real máxima alcanzada: ${maxDepthReached}. TT Size: ${TT.size} nodos.")

    if (globalBestMove == null) {
        // Fallback absoluto por si algo raro pasa en T=0
        val fallbacks = generarJugadas(rootSim, equipoIA)

        if (fallbacks.isEmpty()) {
            return null
        }

        globalBestMove = fallbacks.random()
    }

    return JugadaIA{
        origenFila = globalBestMove.origenY,
        origenCol = globalBestMove.origenX,
        destinoFila = globalBestMove.destinoY,
        destinoCol = globalBestMove.destinoX,
        carta = globalBestMove.carta,
    };
}

