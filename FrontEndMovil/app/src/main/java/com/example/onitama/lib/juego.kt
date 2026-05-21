package com.example.onitama.lib

import android.util.Log
import com.example.onitama.PartidaActiva
import com.example.onitama.api.Partida

// ─── Constantes del tablero ───────────────────────────────────────────────────
const val DIM = 7
const val CENTRO = (DIM/2)

// ─── Tipos ────────────────────────────────────────────────────────────────────

enum class ModoJuego {
    PUBLICA,
    PRIVADA,
    BOT
}

enum class EquipoID (val id: Int){
    ROJO(2),
    AZUL(1);
}

data class Ficha(
    val equipo: EquipoID,
    val esRey: Boolean
)

data class Celda (
    val ficha: Ficha?,
    /** true si esta casilla es el trono de algún equipo */
    val esTrono: Boolean,

    /**
    * 1 | 2 = trampa activa de ese equipo (aún no disparada).
    * -1    = trampa disparada (casilla injugable).
    * null  = sin trampa.
    */
    val esTrampaEquipo: Int? = null
)

enum class FasePartida {
    COLOCAR_TRAMPA,
    ELEGIR_CARTA_ACCION,
    JUGANDO,
    TERMINADA
}


data class Posicion (
    val fila: Int,
    val col: Int
)

data class EstadoJuego (
    val fasePartida: FasePartida,

    val tablero: List<List<Celda>>,
    val turnoActual: EquipoID,

    /** 2 cartas del jugador local (equipo 2) */
    val cartasJugador: List<Carta>,

    /** 2 cartas del oponente (equipo 1) */
    val cartasOponente: List<Carta>,

    /**
     * Cola de 3 cartas en espera.
     * - cartasSiguientes[0] es la que recibirá quien juegue.
     * - La carta usada se añade al final (índice 2).
     */
    val cartasSiguientes: List<Carta>,

    /** Ficha que el jugador ha pulsado (highlight amarillo) */
    val fichaSeleccionada: Posicion? = null,

    /** Carta que el jugador ha seleccionado (activa las casillas azules) */
    val cartaSeleccionada: Carta? = null,

    /** Casillas destino válidas según la ficha y carta seleccionadas */
    val movimientosValidos: List<Posicion> = emptyList(),

    /** Equipo ganador; null mientras la partida siga en curso */
    val ganador: EquipoID? = null,

    /** Origen y destino del último movimiento (para feedback visual) */
    val ultimoMovimiento: Pair<Posicion, Posicion>? = null,

    
    val cartasAccionPropia : List<String> = emptyList(),
    val cartasAccionRival: List<String> = emptyList(),

    val cartaAccionInicialElegida: String? = null,
    val cartaAccionYaUsada: Boolean = false,

    val modoAccion: String? = null,
    
    val equipoCiego: EquipoID? = null,

    val espejoActivadoPor: EquipoID? = null,

    val restriccionSolo: RestriccionSolo? = null,
    
    val posicionTrampa: Posicion? = null,
    val mensajeErrorTrampa: String? = null,
    val posicionErrorTrampa: Posicion? = null
)

enum class TipoRestriccion {
    SOLO_PARA_ADELANTE,
    SOLO_PARA_ATRAS
}

data class RestriccionSolo(
    val tipo: TipoRestriccion,

    /** Quien jugó la carta Dama/Finisterre */
    val caster: EquipoID,
)

fun resolverRestrccionSoloTrasMovimiento (
    r: RestriccionSolo?,
    equipoQueMueve : EquipoID
) : RestriccionSolo ? {
    if (r == null) {
        return null
    }

    var rivalDelCaster = equipoQueMueve != r.caster

    return if (rivalDelCaster) {
        null
    }
    else {
        r
    }
}

fun activarRestriccionSolo (
    caster: EquipoID,
    tipo: TipoRestriccion
) : RestriccionSolo {
    return RestriccionSolo (
        tipo,
        caster
    )
}

/** Misma transformación que al aplicar ESPEJO (invertir dc en cada vector). */
fun invertirCartasEspejo (
    cartas: List<Carta>
) : List<Carta> {
    return cartas.map { carta ->
        carta.copy(
            movimientos = carta.movimientos.map { mov -> 
                mov.copy(dc = -mov.dc)
            }
        )
    }
}

/**
 * Si el servidor habría deshecho ESPEJO en este movimiento (mueve quien no jugó Pensatorium),
 * alinea las cartas del cliente con Partida.moverFicha.
 */
fun deshacerEspejoTrasMovimientoRival (
    estado: EstadoJuego,
    equipoQueMueve: EquipoID
) : EstadoJuego {
    val c = estado.espejoActivadoPor

    if (c == null || c == equipoQueMueve) {
        return estado
    }

    return estado.copy(
            cartasJugador = invertirCartasEspejo(estado.cartasJugador),
            cartasOponente = invertirCartasEspejo(estado.cartasOponente),
            cartasSiguientes = invertirCartasEspejo(estado.cartasSiguientes),
            espejoActivadoPor = null
        )
}

// ─── Creación del estado inicial ──────────────────────────────────────────────

/** Construye el tablero 7×7 con fichas en posición inicial */
fun crearTableroInicial(): List<List<Celda>> {
    return List(DIM) { fila ->
        List(DIM) { col ->
            var ficha: Ficha? = null
            val esTronoSuperior = fila == 0 && col == CENTRO
            val esTronoInferior = fila == DIM - 1 && col == CENTRO
            if (fila == 0 || fila == DIM - 1){
                val equipo: EquipoID
                if (fila == 0){
                    equipo = EquipoID.ROJO
                }
                else {
                    equipo = EquipoID.AZUL
                }
                ficha = Ficha(equipo, col == CENTRO)
            }
            Celda(
                ficha = ficha,
                esTrono = esTronoSuperior || esTronoInferior,
                esTrampaEquipo = null
            )
        }
    }
}

/**
 * Crea el estado inicial de una partida local (mock).
 * Se reparten 7 cartas aleatorias: 2 jugador + 2 oponente + 3 en cola.
 */
fun crearEstadoInicial(): EstadoJuego {
    val cartas = Cartas.selectRandomCards(7)
    return EstadoJuego(
        fasePartida = FasePartida.JUGANDO,
        tablero = crearTableroInicial(),
        turnoActual = EquipoID.AZUL,
        cartasJugador = listOf(cartas[0], cartas[1]),
        cartasOponente = listOf(cartas[2], cartas[3]),
        cartasSiguientes = listOf(cartas[4], cartas[5], cartas[6])
    )
}

/**
 * Devuelve las casillas a las que puede moverse la ficha en (fila, col)
 * utilizando la carta dada.
 *
 */
fun calcularMovimientosValidos (
    tablero: List<List<Celda>>,
    fila: Int,
    col: Int,
    cartaMov: Carta,
    equipoFicha: EquipoID,
    restriccion: RestriccionSolo? = null
): List<Posicion> {
    val signo = if(equipoFicha == EquipoID.AZUL) 1 else -1

    val validos = mutableListOf<Posicion>()

    for (movimientos in cartaMov.movimientos){
         if (restriccion != null) {
            if (restriccion.tipo == TipoRestriccion.SOLO_PARA_ADELANTE && movimientos.df < 0) {
                continue
            }
            if (restriccion.tipo == TipoRestriccion.SOLO_PARA_ATRAS && movimientos.df > 0) {
                continue
            }
        }

        val nf = fila - (movimientos.df * signo)
        val nc = col + (movimientos.dc * signo)

        if (nf < 0 || nf >= DIM || nc < 0 || nc>= DIM){
            continue
        }

        if (tablero[nf][nc].ficha?.equipo == equipoFicha){
            continue
        }

        if (tablero[nf][nc].esTrampaEquipo == -1) {
            continue
        }

        validos.add(Posicion(nf, nc))
    }
    return validos
}

fun tieneMovimientosPosibles(
    estado: EstadoJuego, 
    equipo: EquipoID
): Boolean {
    val tieneCartaAccion = if (equipo == EquipoID.AZUL) {
        estado.cartasAccionPropia.isNotEmpty()
    }
    else {
        estado.cartasAccionRival.isNotEmpty()
    }

    if (tieneCartaAccion && estado.modoAccion == null) {
        return true
    }

    val cartas = if (equipo == EquipoID.AZUL) {
        estado.cartasJugador
    }
    else {
        estado.cartasOponente
    }

    for (f in 0 until DIM) {
        for (c in 0 until DIM) {
            val celda = estado.tablero[f][c]

            if (celda.ficha?.equipo == equipo) {
                for (carta in cartas) {
                    val validos = calcularMovimientosValidos(
                        estado.tablero,
                        f,
                        c,
                        carta,
                        equipo,
                        estado.restriccionSolo
                    )

                    if (validos.isNotEmpty()){
                        return true
                    }
                }
            }
        }
    }

    return false
}

fun aplicarCartaAccion(
    estado: EstadoJuego,
    equipo: EquipoID,
    cartaNombre: String,
    x: Int,
    y: Int,
    x_op: Int,
    y_op: Int,
    cartaRobar: String,
    tipo: String?
): EstadoJuego {
    val tablero = estado.tablero.map {
        fila -> fila.toMutableList()
    }.toMutableList()
    Log.d("LOG de partida", "Intentando Aplicar acción: $tipo")
    when (tipo) {
        "REVIVIR" -> {
            if (y in 0 until DIM && x in 0 until DIM) {
                tablero[y][x] = tablero[y][x].copy(
                    ficha = Ficha(
                        equipo, 
                        false
                    )
                )
            }
        }

        "SALVAR_REY" -> {
            for (f in 0 until DIM) {
                for (c in 0 until DIM) {
                    val ficha = tablero[f][c].ficha
                    if (ficha?.esRey == true && ficha.equipo == equipo) {
                        tablero[f][c] = tablero[f][c].copy(
                            ficha = null
                        )
                    }
                }
            }

            if (y in 0 until DIM && x in 0 until DIM) {
                tablero[y][x] = tablero[y][x].copy(
                    ficha = Ficha(
                        equipo, 
                        true
                    )
                )
            }
        }

        "SACRIFICIO" -> {
            if (y in 0 until DIM && x in 0 until DIM) {
                tablero[y][x] = tablero[y][x].copy(
                    ficha = null
                )
            }
            if (y_op in 0 until DIM && x_op in 0 until DIM) {
                tablero[y_op][x_op] = tablero[y_op][x_op].copy(
                    ficha = null
                )
            }
        }
    }

    val cambioTurno = (tipo != "ROBAR")
    val siguiente = if (equipo == EquipoID.AZUL) {
        EquipoID.ROJO
    }
    else {
        EquipoID.AZUL
    }
    val nuevoTurno = if (cambioTurno) {
        siguiente 
    }
    else {
        equipo
    } 

    val nuevasCartasAccionPropia = if (equipo == EquipoID.AZUL) {
        estado.cartasAccionPropia - cartaNombre
    }
    else {
        estado.cartasAccionPropia
    }

    val nuevasCartasAccionRival = if (equipo == EquipoID.ROJO) {
        estado.cartasAccionRival - cartaNombre
    }
    else {
        estado.cartasAccionRival
    }

    var nuevoEstado = estado.copy(
        tablero = tablero,
        turnoActual = nuevoTurno,
        fichaSeleccionada = null,
        cartaSeleccionada = null,
        movimientosValidos = emptyList(),
        modoAccion = null,
        cartasAccionPropia = nuevasCartasAccionPropia,
        cartasAccionRival = nuevasCartasAccionRival,
    )

    return when(tipo) {
        "ROBAR" -> {
            val miEquipo = PartidaActiva.datosPartida!!.obtenerEquipoID()
            val misCartas = if (equipo == miEquipo) {
                estado.cartasJugador
            }
            else {
                estado.cartasOponente
            }

            val susCartas = if (equipo == miEquipo) {
                estado.cartasOponente
            }
            else {
                estado.cartasJugador
            }

            val siguientes = estado.cartasSiguientes.toMutableList()
            val robar = susCartas.find {
                it.nombre == cartaRobar
            }

            if (robar != null && siguientes.isNotEmpty()) {
                val nueva = siguientes.removeAt(0)
                val nuevasJugador = misCartas + robar
                val nuevasOponente = susCartas.filter {
                    it.nombre != cartaRobar
                } + nueva

                nuevoEstado.copy(
                    cartasJugador = if (equipo == miEquipo) nuevasJugador else nuevasOponente,
                    cartasOponente = if (equipo != miEquipo) nuevasJugador else nuevasOponente,
                    cartasSiguientes = siguientes,
                    turnoActual = equipo
                )
            }
            else {
                nuevoEstado
            }
        }

        "ESPEJO" -> {
            nuevoEstado.copy(
                cartasJugador = invertirCartasEspejo(estado.cartasJugador),
                cartasOponente = invertirCartasEspejo(estado.cartasOponente),
                cartasSiguientes = invertirCartasEspejo(estado.cartasSiguientes),
                espejoActivadoPor = equipo
            )
        }

        "CEGAR" -> {
            val victima = if (equipo == EquipoID.AZUL) EquipoID.ROJO else EquipoID.AZUL
            nuevoEstado.copy(
                equipoCiego = victima
            )
        }

        "SOLO_PARA_ADELANTE" -> {
            nuevoEstado.copy(
                restriccionSolo = activarRestriccionSolo(equipo, TipoRestriccion.SOLO_PARA_ADELANTE)
            )
        }

        "SOLO_PARA_ATRAS" -> {
            nuevoEstado.copy(
                restriccionSolo = activarRestriccionSolo(equipo, TipoRestriccion.SOLO_PARA_ATRAS)
            )
        }

        else -> nuevoEstado
    }
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
fun convertirCarta(cartaS: Any):Carta{
    // Formato antiguo: solo nombre (mock o versión anterior del servidor)
    if (cartaS is String){
        val encontrada = Cartas.todas_cartas.find {
            it.nombre == cartaS
        }

        if (encontrada == null){
            println("[juego] Carta \"$cartaS\" no encontrada en catálogo. Usando primera disponible.")
            return Cartas.todas_cartas[0]
        }
        return encontrada
    }

    if (cartaS is CartaServidor){
        // Formato nuevo: objeto con nombre y movimientos del servidor
        val imagen = Cartas.todas_cartas.find {
            it.nombre == cartaS.nombre
        }?.imagen ?: "🃏"

        return Carta(
            nombre = cartaS.nombre,
            imagen,
            movimientos = cartaS.movimientos?.map {
                Movimiento(dc = it.x, df = it.y)
            }?: emptyList()
        )
    }
    return Cartas.todas_cartas[0]
}

fun tableroDesdeServidor(
    trampa1: String,
    trampa2: String,
    eq1: String,
    eq2: String
) : List<List<Celda>> {
    val nuevoTablero = MutableList(DIM) { fila ->
        MutableList(DIM) { col ->
            Celda(
                ficha = null,
                esTrono = (fila == 0 && col == CENTRO) || (fila == DIM - 1 && col == CENTRO),
                esTrampaEquipo = null
            )
        }
    }

    val reyRe = Regex("\\[(-?\\d+),(-?\\d+)\\]")
    val peonRe = Regex("\\((-?\\d+),(-?\\d+)\\)")


    // Misma constante que usas en el ViewModel para invertir (asumo que DIM - 1 es 6)
    val END = DIM - 1

    fun colocar(
        data: String,
        equipo: EquipoID
    ) {
        reyRe.findAll(data).forEach { m ->
            val colServidor = m.groupValues[1].toInt()
            val filaServidor = m.groupValues[2].toInt()

            // ¡Aplicamos la inversión aquí!
            val col = END - colServidor
            val fila = END - filaServidor

            if (fila in 0 until DIM && col in 0 until DIM) {
                nuevoTablero[fila][col] = nuevoTablero[fila][col].copy(ficha = Ficha(equipo, true))
            }
        }

        peonRe.findAll(data).forEach { m ->
            val colServidor = m.groupValues[1].toInt()
            val filaServidor = m.groupValues[2].toInt()

            // ¡Aplicamos la inversión aquí!
            val col = END - colServidor
            val fila = END - filaServidor

            if (fila in 0 until DIM && col in 0 until DIM) {
                nuevoTablero[fila][col] = nuevoTablero[fila][col].copy(ficha = Ficha(equipo, false))
            }
        }
    }

    colocar(eq1, EquipoID.AZUL)
    colocar(eq2, EquipoID.ROJO)


    fun colocarTrampaManual(datos: String?, equipo: EquipoID) {
        if (datos.isNullOrBlank()) return

        try {
            val partes = datos.split(",")
            if (partes.size == 3) {
                val colServidor = partes[0].toInt()
                val filaServidor = partes[1].toInt()
                val activa = partes[2].toInt()

                val col = END - colServidor
                val fila = END - filaServidor

                if (fila in 0 until DIM && col in 0 until DIM) {
                    nuevoTablero[fila][col] = nuevoTablero[fila][col].copy(
                        esTrampaEquipo = if (activa == 1) equipo.id else -1
                    )
                    Log.d("DEBUG_TRAMPA", "Trampa reanudada en $fila,$col para ${equipo.id} (Activa: $activa)")
                }
            }
        } catch (e: Exception) {
            Log.e("ERROR_TRAMPA", "Error procesando trampa: $datos")
        }
    }

    colocarTrampaManual(trampa1, EquipoID.AZUL)
    colocarTrampaManual(trampa2, EquipoID.ROJO)

    return nuevoTablero
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
    tablero_eq1: String?,
    tablero_eq2: String?,
    esReanudada: Boolean,
    trampa_eq1: String?,
    trampa_eq2: String?,
    
    /** Turno numérico del servidor: par=equipo1, impar=equipo2 */
    turno: Int?,

    /** Cartas de acción (para partidas reanudadas). Puede venir como array [propia, rival]
    *  o como campos individuales carta_accion_propia / carta_accion_rival. */
    cartas_accion_propia: List<Partida.CartaAccionJson>?,
    cartas_accion_rival: List<Partida.CartaAccionJson>?
): EstadoJuego {
    Log.d("LOG de partida", "Partida reanudada?: $esReanudada")
    val tablero = if (esReanudada) {
        tableroDesdeServidor(eq1 = tablero_eq1!!, eq2 = tablero_eq2!!,trampa1 = trampa_eq1!!, trampa2 = trampa_eq2!!)
    }
    else {
        crearTableroInicial()
    }

    val turnoActual = if ((turno ?: 0) % 2 == 0) {
        EquipoID.AZUL // El turno 0 (par) siempre es del Equipo 1 (Azul)
    } else {
        EquipoID.ROJO // El turno 1 (impar) siempre es del Equipo 2 (Rojo)
    }

    val faseP = if (esReanudada) {
        FasePartida.JUGANDO
    }
    else {
        FasePartida.COLOCAR_TRAMPA
    }
    
    return EstadoJuego (
        fasePartida = faseP,
        tablero = tablero,
        turnoActual = turnoActual,
        cartasJugador = cartas_jugador.map { convertirCarta(it) },
        cartasOponente = cartas_oponente.map { convertirCarta(it) },
        cartasSiguientes = carta_siguiente.map { convertirCarta(it) },
        cartasAccionPropia = cartas_accion_propia?.map { it.nombre } ?: emptyList(),
        cartasAccionRival = cartas_accion_rival?.map { it.nombre } ?: emptyList()
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
    carta: Carta,
    equipoLocal: EquipoID,
    trampaActivada: Boolean = false
): ResultadoMovimiento {
    val tablero = estado.tablero.map { fila ->
        fila.toMutableList()
    }.toMutableList()

    val fichaMovida = tablero[origen.fila][origen.col].ficha!!

    var capturado = false
    var esReyCapturado = false

    val esTrampaOponente = tablero[destino.fila][destino.col].esTrampaEquipo != null && 
                           tablero[destino.fila][destino.col].esTrampaEquipo != fichaMovida.equipo.id

    if (trampaActivada || esTrampaOponente) {
        capturado = true
        esReyCapturado = fichaMovida.esRey

        tablero[destino.fila][destino.col] = tablero[destino.fila][destino.col].copy(
            ficha = null,
            esTrampaEquipo = -1
        )

        tablero[origen.fila][origen.col] = tablero[origen.fila][origen.col].copy(
            ficha = null
        )
    }
    else {
        val fichaDestino = tablero[destino.fila][destino.col].ficha
        
        if (fichaDestino != null) {
            capturado = true
            esReyCapturado = fichaDestino.esRey
        }

        tablero[destino.fila][destino.col] = tablero[destino.fila][destino.col].copy(
            ficha = fichaMovida
        )

        tablero[origen.fila][origen.col] = tablero[origen.fila][origen.col].copy(
            ficha = null
        )
    }

    /** Victoria por trono: el rey llega al trono enemigo */
    val victoriaPorTrono = fichaMovida.esRey && destino.col == CENTRO &&
            ((fichaMovida.equipo == EquipoID.AZUL && destino.fila == 0) ||
                    (fichaMovida.equipo == EquipoID.ROJO && destino.fila == DIM - 1))

    val equipoActual = estado.turnoActual

    val cartasMovedor = if (equipoActual == equipoLocal) {
        estado.cartasJugador
    }
    else {
        estado.cartasOponente
    }

    val tieneCartaExtra = cartasMovedor.size > 2

    /** El jugador recibe la primera carta de la cola; la carta usada va al final.*/
    var cartaRecibida: Carta? = null
    val nuevasSiguientes: List<Carta>

    if (tieneCartaExtra) {
        nuevasSiguientes = estado.cartasSiguientes + carta
    }
    else {
        cartaRecibida = estado.cartasSiguientes[0]
        nuevasSiguientes = estado.cartasSiguientes.drop(1) + carta
    }

    /**
     * Si el equipo que mueve ahora ES el jugador local → actualizar cartasJugador.
     * Si el equipo que mueve ahora ES el oponente → actualizar cartasOponente.
     */
    val nuevasCartasJugador = if (equipoActual == equipoLocal) {
        if (tieneCartaExtra) {
            estado.cartasJugador.filter {
                it.nombre != carta.nombre
            }
        } 
        else {
            estado.cartasJugador.map {
            if (it.nombre == carta.nombre) {
                cartaRecibida!!
            } 
            else {
                it
            } 
            }
        }
    } 
    else {
        estado.cartasJugador
    } 

    val nuevasCartasOponente = if (equipoActual != equipoLocal) {
        if (tieneCartaExtra) {
            estado.cartasOponente.filter {
                it.nombre != carta.nombre
            }
        }
        else {
            estado.cartasOponente.map {
            if (it.nombre == carta.nombre) {
                cartaRecibida!!
            } 
            else {
                it
            }
        }
        } 
    } 
    else {
        estado.cartasOponente
    } 

    var ganador: EquipoID?
    if (esReyCapturado || victoriaPorTrono){
        ganador = equipoActual
    }
    else {
        ganador = null
    }

    val siguiente = if (equipoActual == EquipoID.ROJO) EquipoID.AZUL else EquipoID.ROJO
    
    var nuevoEstado = estado.copy(
        fasePartida = if (ganador != null) FasePartida.TERMINADA else estado.fasePartida,
        tablero = tablero,
        turnoActual = siguiente,
        cartasJugador = nuevasCartasJugador,
        cartasOponente = nuevasCartasOponente,
        cartasSiguientes = nuevasSiguientes,
        fichaSeleccionada = null,
        cartaSeleccionada = null,
        movimientosValidos = emptyList(),
        ganador = ganador,
        ultimoMovimiento = Pair(origen, destino),
        equipoCiego = estado.equipoCiego,
        restriccionSolo = resolverRestrccionSoloTrasMovimiento(estado.restriccionSolo, equipoActual)
    )

    val puedeMoverDespues = tieneMovimientosPosibles(nuevoEstado, siguiente)

    val gana = when {
        esReyCapturado || victoriaPorTrono || !puedeMoverDespues -> equipoActual
        else -> null
    }

    val estadoFinal = deshacerEspejoTrasMovimientoRival(nuevoEstado, equipoActual)

    return ResultadoMovimiento(estadoFinal, capturado, esReyCapturado, victoriaPorTrono)
}