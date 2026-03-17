package com.example.onitama.lib


// ─── Constantes del tablero ───────────────────────────────────────────────────
const val DIM = 7
const val CENTRO = (DIM/2)

// ─── Tipos ────────────────────────────────────────────────────────────────────

enum class EquipoID (val id: Int){
    ARROJO(1),
    ABAZUL(2)
}

data class Ficha(
    val equipo: EquipoID,
    val esRey: Boolean
)

data class Celda (
    val ficha: Ficha?,
    /** true si esta casilla es el trono de algún equipo */
    val esTrono: Boolean
)

data class Posicion (
    val fila: Int,
    val col: Int
)

data class EstadoJuego (
    val tablero: List<List<Celda>>,
    val turnoActual: EquipoID,

    /** 2 cartas del jugador local (equipo 2) */
    val cartasJugador: List<carta>,

    /** 2 cartas del oponente (equipo 1) */
    val cartasOponente: List<carta>,

    /**
     * Cola de 3 cartas en espera.
     * - cartasSiguientes[0] es la que recibirá quien juegue.
     * - La carta usada se añade al final (índice 2).
     */
    val cartasSiguientes: List<carta>,

    /** Ficha que el jugador ha pulsado (highlight amarillo) */
    val fichaSeleccionada: Posicion?,

    /** Carta que el jugador ha seleccionado (activa las casillas azules) */
    val cartaSeleccionada: carta?,

    /** Casillas destino válidas según la ficha y carta seleccionadas */
    val movimientosValidos: List<Posicion>,

    /** Equipo ganador; null mientras la partida siga en curso */
    val ganador: EquipoID?,

    /** Origen y destino del último movimiento (para feedback visual) */
    val ultimoMovimiento: Pair<Posicion, Posicion>?
)

// ─── Creación del estado inicial ──────────────────────────────────────────────

/** Construye el tablero 7×7 con fichas en posición inicial */
fun crearTableroInicial(): List<List<Celda>> {
    return List(DIM) { fila ->
        List(DIM) { col ->
            var ficha: Ficha? = null
            if (fila == 0 || fila == DIM - 1){
                val equipo: EquipoID
                if (fila == 0){
                    equipo = EquipoID.ARROJO
                }
                else {
                    equipo = EquipoID.ABAZUL
                }
                ficha = Ficha(equipo, col == CENTRO)
            }
            Celda(
                ficha = ficha,
                esTrono = (fila == 0 || fila == DIM - 1) && col == CENTRO
            )
        }
    }
}

/**
 * Crea el estado inicial de una partida local (mock).
 * Se reparten 7 cartas aleatorias: 2 jugador + 2 oponente + 3 en cola.
 */
fun crearEstadoInicial(): EstadoJuego {
    val cartas = cartas.selectRandomCards(7)
    return EstadoJuego(
        tablero = crearTableroInicial(),
        turnoActual = EquipoID.ABAZUL,
        cartasJugador = listOf(cartas[0], cartas[1]),
        cartasOponente = listOf(cartas[2], cartas[3]),
        cartasSiguientes = listOf(cartas[4], cartas[5], cartas[6]),
        fichaSeleccionada = null,
        cartaSeleccionada = null,
        movimientosValidos = emptyList(),
        ganador = null,
        ultimoMovimiento = null,
    )
}

/**
 * Devuelve las casillas a las que puede moverse la ficha en (fila, col)
 * utilizando la carta dada.
 *
 *  - Equipo 2 (abajo)
 *  - Equipo 1 (arriba)
 */
fun calcularMovimientosValidos (
    tablero: List<List<Celda>>,
    fila: Int,
    col: Int,
    cartaMov: carta,
    equipo: EquipoID
): List<Posicion> {
    var signo: Int
    if (equipo == EquipoID.ABAZUL) {
        signo = 1
    }
    else {
        signo = -1
    }

    val validos = mutableListOf<Posicion>()

    for (movimientos in cartaMov.movimientos){
        val nf = fila - (movimientos.df * signo)
        val nc = col + (movimientos.dc * signo)

        if (nf < 0 || nf >= DIM || nc < 0 || nc>= DIM){
            continue
        }
        if (tablero[nf][nc].ficha?.equipo == equipo){
            continue
        }

        validos.add(Posicion(nf, nc))
    }
    return validos
}

// ─── Creación del estado a partir de datos del servidor ──────────────────────

/**
 * Formato de carta tal como la envía el servidor en PARTIDA_ENCONTRADA.
 * El servidor incluye los movimientos con coordenadas {x, y} sacadas de la BD.
 * Convención: x = delta de columna (dc), y = delta de fila (df).
 * Ambos valores son equivalentes a los dc/df del catálogo local (cartas.ts).
 */
data class MovimientoServidor (
    val x: Int,
    val y: Int
)

data class CartaServidor (
    val nombre: String,
    val movimientos: List<MovimientoServidor>
)

/**
 * Convierte una carta del formato servidor al formato frontend.
 * Si el servidor no envía movimientos (retrocompatibilidad mock), se busca
 * por nombre en el catálogo local.
 */
fun convertirCarta(cartaS: Any):carta{
    // Formato antiguo: solo nombre (mock o versión anterior del servidor)
    if (cartaS is String){
        val encontrada = cartas.todas_cartas.find {
            it.nombre == cartaS
        }

        if (encontrada == null){
            println("[juego] Carta \"$cartaS\" no encontrada en catálogo. Usando primera disponible.")
            return cartas.todas_cartas[0]
        }
        return encontrada
    }

    if (cartaS is CartaServidor){
        // Formato nuevo: objeto con nombre y movimientos del servidor
        val imagen = cartas.todas_cartas.find {
            it.nombre == cartaS.nombre
        }?.imagen ?: "🃏"

        return carta(
            nombre = cartaS.nombre,
            imagen,
            movimientos = cartaS.movimientos?.map {
                Movimiento(dc = it.x, df = it.y)
            }?: emptyList()
        )
    }
    return cartas.todas_cartas[0]
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
 */
fun crearEstadoServidor (
    cartas_jugador: List<Any>,
    cartas_oponente: List<Any>,
    carta_siguiente: List<Any>,
    equipo: EquipoID?
): EstadoJuego {
    return EstadoJuego (
        tablero = crearTableroInicial(),
        turnoActual = EquipoID.ARROJO,
        cartasJugador = cartas_jugador.map { convertirCarta(it) },
        cartasOponente = cartas_oponente.map { convertirCarta(it) },
        cartasSiguientes = carta_siguiente.map { convertirCarta(it) },
        fichaSeleccionada = null,
        cartaSeleccionada = null,
        movimientosValidos = emptyList(),
        ganador = null,
        ultimoMovimiento = null
    )
}

/**
 * Ejecución de un movimiento
 */
data class ResultadoMovimiento (
    val nuevoEstado: EstadoJuego,
    val capturado: Boolean,
    val esReyCapturado: Boolean,
    val victoriaPorTrono: Boolean
)


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
fun ejecutarMovimiento (
    estado: EstadoJuego,
    origen: Posicion,
    destino: Posicion,
    carta: carta,
    equipoLocal: EquipoID = EquipoID.ABAZUL
): ResultadoMovimiento {
    val tablero = estado.tablero.map { fila ->
        fila.toMutableList()
    }.toMutableList()

    val fichaMovida = tablero[origen.fila][origen.col].ficha!!
    val fichaDestino = tablero[destino.fila][destino.col].ficha

    val capturado = fichaDestino != null
    val esReyCapturado = capturado && fichaDestino!!.esRey

    tablero[destino.fila][destino.col] = tablero[destino.fila][destino.col].copy(ficha = fichaMovida)
    tablero[origen.fila][origen.col] = tablero[origen.fila][origen.col].copy(ficha = null)

    /** Victoria por trono: el rey llega al trono enemigo */
    val victoriaPorTrono = fichaMovida.esRey &&
            ((fichaMovida.equipo === EquipoID.ABAZUL && destino.fila == 0 && destino.col === CENTRO) ||
                    (fichaMovida.equipo === EquipoID.ARROJO && destino.fila === DIM - 1 && destino.col === CENTRO));

    /** El jugador recibe la primera carta de la cola; la carta usada va al final.*/
    val cartaRecibida = estado.cartasSiguientes[0]
    val nuevasSiguientes = listOf(estado.cartasSiguientes[1], estado.cartasSiguientes[2], carta)

    val equipoActual = estado.turnoActual

    /**
     * Si el equipo que mueve ahora ES el jugador local → actualizar cartasJugador.
     * Si el equipo que mueve ahora ES el oponente      → actualizar cartasOponente.
     */
    val nuevasCartasJugador = if (equipoActual == equipoLocal) {
        estado.cartasJugador.map {
            if (it.nombre == carta.nombre) cartaRecibida else it
        }
    } else estado.cartasJugador

    val nuevasCartasOponente = if (equipoActual != equipoLocal) {
        estado.cartasOponente.map {
            if (it.nombre == carta.nombre) cartaRecibida else it
        }
    } else estado.cartasOponente

    var ganador: EquipoID?
    if (esReyCapturado || victoriaPorTrono){
        ganador = equipoActual
    }
    else {
        ganador = null
    }

    val nuevoEstado = estado.copy(
        tablero,
        turnoActual = if (equipoActual == EquipoID.ARROJO) EquipoID.ABAZUL else EquipoID.ARROJO,
        cartasJugador = nuevasCartasJugador,
        cartasOponente = nuevasCartasOponente,
        cartasSiguientes = nuevasSiguientes,
        fichaSeleccionada = null,
        cartaSeleccionada = null,
        movimientosValidos = emptyList(),
        ganador,
        ultimoMovimiento = Pair(origen, destino)
    )

    return ResultadoMovimiento(nuevoEstado, capturado, esReyCapturado, victoriaPorTrono)
}