package com.example.onitama.lib

import android.util.Log
import com.example.onitama.PartidaActiva
import com.example.onitama.api.Partida

// ─── Constantes del tablero ───────────────────────────────────────────────────
const val DIM = 7// Dimensión del tablero (7x7)
const val CENTRO = (DIM/2) // Columna central donde se ubican los tronos (índice 3)

// ─── Tipos ────────────────────────────────────────────────────────────────────

/** Representa las modalidades de emparejamiento del juego. */
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

/**
 * esta clase represeta en que fase esta la partida,
 * es necesaria para poder añadir las funcionalidades de colocar trampa y
 * seleccionar carta de acción.
 * **/
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

/**
 * Estado completo y centralizado de la partida (Single Source of Truth).
 * Contiene el tablero, cartas de movimientos, variables de interfaz (UI) y efectos activos.
 */
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

    /** Cartas de acción especiales disponibles y su estado */
    val cartasAccionPropia : List<String> = emptyList(),
    val cartasAccionRival: List<String> = emptyList(),

    val cartaAccionInicialElegida: String? = null,
    val cartaAccionYaUsada: Boolean = false,

    val modoAccion: String? = null, // Tipo de acción especial en ejecución

    val equipoCiego: EquipoID? = null, // Restringe la visibilidad del equipo afectado

    val espejoActivadoPor: EquipoID? = null, // Almacena qué equipo activó el efecto espejo

    val restriccionSolo: RestriccionSolo? = null, // Limitación de vectores de movimiento

    // Gestión de errores y previsualización de trampas en la UI
    val posicionTrampa: Posicion? = null,
    val mensajeErrorTrampa: String? = null,
    val posicionErrorTrampa: Posicion? = null
)

/** Modificadores direccionales impuestos por efectos de cartas especiales. */
enum class TipoRestriccion {
    SOLO_PARA_ADELANTE,
    SOLO_PARA_ATRAS
}

/** Almacena una restricción de movimiento activa y quién la aplicó. */
data class RestriccionSolo(
    val tipo: TipoRestriccion,

    /** Quien jugó la carta Dama/Finisterre */
    val caster: EquipoID,
)
/**
 * Evalúa si una restricción direccional debe disiparse tras un movimiento.
 * Se elimina si el rival del 'caster' ha realizado su turno.
 */
fun resolverRestrccionSoloTrasMovimiento (
    r: RestriccionSolo?,
    equipoQueMueve : EquipoID
) : RestriccionSolo ? {
    if (r == null) return null

    val rivalDelCaster = equipoQueMueve != r.caster
    return if (rivalDelCaster) null else r
}

/** Inicializa una nueva restricción direccional de movimiento. */
fun activarRestriccionSolo (
    caster: EquipoID,
    tipo: TipoRestriccion
) : RestriccionSolo {
    return RestriccionSolo (tipo, caster)
}

/** Invierte horizontalmente (Delta Columna) los vectores de un conjunto de cartas (Efecto Espejo). */
fun invertirCartasEspejo (
    cartas: List<Carta>
) : List<Carta> {
    return cartas.map { carta ->
        carta.copy(
            movimientos = carta.movimientos.map { mov ->
                mov.copy(dc = -mov.dc) // Invierte el eje de las columnas
            }
        )
    }
}

/**
 * Reestablece las cartas a su estado original si el rival consumió su turno
 * bajo los efectos del modificador ESPEJO.
 */
fun deshacerEspejoTrasMovimientoRival (
    estado: EstadoJuego,
    equipoQueMueve: EquipoID
) : EstadoJuego {
    val c = estado.espejoActivadoPor

    // Si no hay espejo activo o quien movió fue el mismo que lo activó, no cambia nada
    if (c == null || c == equipoQueMueve) {
        return estado
    }

    // Deshace la inversión aplicando el espejo nuevamente a todas las barajas
    return estado.copy(
        cartasJugador = invertirCartasEspejo(estado.cartasJugador),
        cartasOponente = invertirCartasEspejo(estado.cartasOponente),
        cartasSiguientes = invertirCartasEspejo(estado.cartasSiguientes),
        espejoActivadoPor = null
    )
}

// ─── Creación del estado inicial ──────────────────────────────────────────────

/** Construye el tablero 7×7 posicionando Reyes y Peones en los extremos opuestos. */
fun crearTableroInicial(): List<List<Celda>> {
    return List(DIM) { fila ->
        List(DIM) { col ->
            var ficha: Ficha? = null
            val esTronoSuperior = fila == 0 && col == CENTRO
            val esTronoInferior = fila == DIM - 1 && col == CENTRO

            // Colocación de piezas en la primera y última fila
            if (fila == 0 || fila == DIM - 1){
                val equipo = if (fila == 0) EquipoID.ROJO else EquipoID.AZUL
                ficha = Ficha(equipo, col == CENTRO) // El Rey se ubica en el centro
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
 * Crea el estado inicial para pruebas locales (Mock).
 * Selecciona 7 cartas aleatorias de la baraja global y distribuye las manos.
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
 * Calcula las coordenadas válidas a las que puede desplazarse una ficha dada una carta.
 * Filtra los límites del tablero, colisiones aliadas, trampas detonadas y restricciones activas.
 */
fun calcularMovimientosValidos (
    tablero: List<List<Celda>>,
    fila: Int,
    col: Int,
    cartaMov: Carta,
    equipoFicha: EquipoID,
    restriccion: RestriccionSolo? = null
): List<Posicion> {
    // El bando Azul avanza hacia arriba (matemáticamente resta filas) y el Rojo hacia abajo (suma filas)
    val signo = if(equipoFicha == EquipoID.AZUL) 1 else -1
    val validos = mutableListOf<Posicion>()

    for (movimientos in cartaMov.movimientos){
        // Filtrado por restricciones direccionales (Cartas Dama/Finisterre)
        if (restriccion != null) {
            if (restriccion.tipo == TipoRestriccion.SOLO_PARA_ADELANTE && movimientos.df < 0) {
                continue
            }
            if (restriccion.tipo == TipoRestriccion.SOLO_PARA_ATRAS && movimientos.df > 0) {
                continue
            }
        }

        // Mapeo vectorial relativo a la posición actual de la pieza
        val nf = fila - (movimientos.df * signo)
        val nc = col + (movimientos.dc * signo)

        // Fuera de los límites del tablero 7x7
        if (nf < 0 || nf >= DIM || nc < 0 || nc>= DIM){
            continue
        }

        // Colisión con una pieza del mismo equipo
        if (tablero[nf][nc].ficha?.equipo == equipoFicha){
            continue
        }

        // La casilla contiene una trampa ya detonada (-1 es injugable)
        if (tablero[nf][nc].esTrampaEquipo == -1) {
            continue
        }

        validos.add(Posicion(nf, nc))
    }
    return validos
}

/**
 * Verifica si un bando tiene movimientos reglamentarios disponibles en su turno.
 * Evita bloqueos blandos (soft-locks) si el jugador posee cartas de acción o jugadas válidas.
 */
fun tieneMovimientosPosibles(
    estado: EstadoJuego,
    equipo: EquipoID
): Boolean {
    val tieneCartaAccion = if (equipo == EquipoID.AZUL) {
        estado.cartasAccionPropia.isNotEmpty()
    } else {
        estado.cartasAccionRival.isNotEmpty()
    }

    // Si tiene una carta especial lista para activarse, se asume que retiene movilidad
    if (tieneCartaAccion && estado.modoAccion == null) {
        return true
    }

    val cartas = if (equipo == EquipoID.AZUL) estado.cartasJugador else estado.cartasOponente

    // Escaneo completo del tablero buscando al menos una jugada legal
    for (f in 0 until DIM) {
        for (c in 0 until DIM) {
            val celda = estado.tablero[f][c]

            if (celda.ficha?.equipo == equipo) {
                for (carta in cartas) {
                    val validos = calcularMovimientosValidos(
                        estado.tablero, f, c, carta, equipo, estado.restriccionSolo
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

/**
 * Modifica el estado del juego tras la invocación de una Carta de Acción mística.
 * Modifica las posiciones de las fichas o altera el comportamiento de las barajas.
 */
fun aplicarCartaAccion(
    estado: EstadoJuego,
    equipo: EquipoID,
    cartaNombre: String,
    x: Int, y: Int,          // Coordenadas objetivo aliadas
    x_op: Int, y_op: Int,    // Coordenadas objetivo rivales
    cartaRobar: String,
    tipo: String?
): EstadoJuego {
    val tablero = estado.tablero.map { fila -> fila.toMutableList() }.toMutableList()
    Log.d("LOG de partida", "Intentando Aplicar acción: $tipo")

    // ─── Ejecución de efectos físicos en el tablero ───
    when (tipo) {
        "REVIVIR" -> {
            if (y in 0 until DIM && x in 0 until DIM) {
                tablero[y][x] = tablero[y][x].copy(ficha = Ficha(equipo, false)) // Revive un peón
            }
        }

        "SALVAR_REY" -> {
            // Remueve al rey de su posición actual previa teletransportación
            for (f in 0 until DIM) {
                for (c in 0 until DIM) {
                    val ficha = tablero[f][c].ficha
                    if (ficha?.esRey == true && ficha.equipo == equipo) {
                        tablero[f][c] = tablero[f][c].copy(ficha = null)
                    }
                }
            }
            if (y in 0 until DIM && x in 0 until DIM) {
                tablero[y][x] = tablero[y][x].copy(ficha = Ficha(equipo, true))
            }
        }

        "SACRIFICIO" -> {
            // Elimina fulminantemente ambas fichas designadas
            if (y in 0 until DIM && x in 0 until DIM) {
                tablero[y][x] = tablero[y][x].copy(ficha = null)
            }
            if (y_op in 0 until DIM && x_op in 0 until DIM) {
                tablero[y_op][x_op] = tablero[y_op][x_op].copy(ficha = null)
            }
        }
    }

    // La carta 'ROBAR' no otorga cambio de turno inmediato al oponente
    val cambioTurno = (tipo != "ROBAR")
    val siguiente = if (equipo == EquipoID.AZUL) EquipoID.ROJO else EquipoID.AZUL
    val nuevoTurno = if (cambioTurno) siguiente else equipo

    // Actualización de los listados de cartas de acción consumidas
    val nuevasCartasAccionPropia = if (equipo == EquipoID.AZUL) estado.cartasAccionPropia - cartaNombre else estado.cartasAccionPropia
    val nuevasCartasAccionRival = if (equipo == EquipoID.ROJO) estado.cartasAccionRival - cartaNombre else estado.cartasAccionRival

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

    // ─── Ejecución de efectos lógicos y estructurales ───
    return when(tipo) {
        "ROBAR" -> {
            val miEquipo = PartidaActiva.datosPartida!!.obtenerEquipoID()
            val misCartas = if (equipo == miEquipo) estado.cartasJugador else estado.cartasOponente
            val susCartas = if (equipo == miEquipo) estado.cartasOponente else estado.cartasJugador
            val siguientes = estado.cartasSiguientes.toMutableList()
            val robar = susCartas.find { it.nombre == cartaRobar }

            if (robar != null && siguientes.isNotEmpty()) {
                val nueva = siguientes.removeAt(0)
                val nuevasJugador = misCartas + robar
                val nuevasOponente = susCartas.filter { it.nombre != cartaRobar } + nueva

                nuevoEstado.copy(
                    cartasJugador = if (equipo == miEquipo) nuevasJugador else nuevasOponente,
                    cartasOponente = if (equipo != miEquipo) nuevasJugador else nuevasOponente,
                    cartasSiguientes = siguientes,
                    turnoActual = equipo
                )
            } else {
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
            nuevoEstado.copy(equipoCiego = victima)
        }

        "SOLO_PARA_ADELANTE" -> {
            nuevoEstado.copy(restriccionSolo = activarRestriccionSolo(equipo, TipoRestriccion.SOLO_PARA_ADELANTE))
        }

        "SOLO_PARA_ATRAS" -> {
            nuevoEstado.copy(restriccionSolo = activarRestriccionSolo(equipo, TipoRestriccion.SOLO_PARA_ATRAS))
        }

        else -> nuevoEstado
    }
}

// ─── Creación del estado a partir de datos del servidor ──────────────────────

/** Estructura de transferencia de vectores proveniente del backend. */
data class MovimientoServidor (
    val x: Int, // Desplazamiento horizontal (Delta Columna)
    val y: Int  // Desplazamiento vertical (Delta Fila)
)

/** Modelo de carta serializado enviado por la API del servidor. */
data class CartaServidor (
    val nombre: String,
    val movimientos: List<MovimientoServidor>
)

/**
 * Transforma un objeto dinámico del servidor (`Any`) al formato interno `Carta`.
 * Soporta compatibilidad con cadenas de texto (Mocks antiguos) u objetos estructurados.
 */
fun convertirCarta(cartaS: Any): Carta {
    if (cartaS is String){
        val encontrada = Cartas.todas_cartas.find { it.nombre == cartaS }
        if (encontrada == null){
            println("[juego] Carta \"$cartaS\" no encontrada en catálogo. Usando primera disponible.")
            return Cartas.todas_cartas[0]
        }
        return encontrada
    }

    if (cartaS is CartaServidor){
        val imagen = Cartas.todas_cartas.find { it.nombre == cartaS.nombre }?.imagen ?: "🃏"
        return Carta(
            nombre = cartaS.nombre,
            imagen = imagen,
            movimientos = cartaS.movimientos.map { Movimiento(dc = it.x, df = it.y) }
        )
    }
    return Cartas.todas_cartas[0]
}

/**
 * Reconstruye la matriz bidimensional del tablero procesando cadenas formateadas del servidor.
 * **Importante:** Invierte los ejes `(END - pos)` para adaptar la perspectiva del rival en espejo.
 */
fun tableroDesdeServidor(
    trampa1: String, trampa2: String,
    eq1: String, eq2: String
): List<List<Celda>> {
    val nuevoTablero = MutableList(DIM) { fila ->
        MutableList(DIM) { col ->
            Celda(
                ficha = null,
                esTrono = (fila == 0 && col == CENTRO) || (fila == DIM - 1 && col == CENTRO),
                esTrampaEquipo = null
            )
        }
    }

    // Expresiones regulares para parsear fichas. Ejemplos: [X,Y] para Rey, (X,Y) para Peón
    val reyRe = Regex("\\[(-?\\d+),(-?\\d+)\\]")
    val peonRe = Regex("\\((-?\\d+),(-?\\d+)\\)")
    val END = DIM - 1 // Índice máximo (6)

    // Función anidada para parsear y posicionar las piezas en la matriz local
    fun colocar(data: String, equipo: EquipoID) {
        reyRe.findAll(data).forEach { m ->
            val colServidor = m.groupValues[1].toInt()
            val filaServidor = m.groupValues[2].toInt()
            val col = END - colServidor
            val fila = END - filaServidor

            if (fila in 0 until DIM && col in 0 until DIM) {
                nuevoTablero[fila][col] = nuevoTablero[fila][col].copy(ficha = Ficha(equipo, true))
            }
        }

        peonRe.findAll(data).forEach { m ->
            val colServidor = m.groupValues[1].toInt()
            val filaServidor = m.groupValues[2].toInt()
            val col = END - colServidor
            val fila = END - filaServidor

            if (fila in 0 until DIM && col in 0 until DIM) {
                nuevoTablero[fila][col] = nuevoTablero[fila][col].copy(ficha = Ficha(equipo, false))
            }
        }
    }

    colocar(eq1, EquipoID.AZUL)
    colocar(eq2, EquipoID.ROJO)

    // Parsea y ubica las trampas activas/detonadas guardadas en la BD
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
 * Inicializa o reanuda una partida usando el paquete de datos del WebSocket.
 * Si es una reanudación, reconstruye el tablero y aplica secuencialmente los efectos persistentes.
 */
fun crearEstadoServidor (
    cartas_jugador: List<Any>,
    cartas_oponente: List<Any>,
    carta_siguiente: List<Any>,
    tablero_eq1: String?, tablero_eq2: String?,
    esReanudada: Boolean,
    trampa_eq1: String?, trampa_eq2: String?,
    turno: Int?,
    cartas_accion_propia: List<Partida.CartaAccionJson>?,
    cartas_accion_rival: List<Partida.CartaAccionJson>?,
    equipoNuestro: EquipoID
): EstadoJuego {
    Log.d("LOG de partida", "Partida reanudada?: $esReanudada")

    val tablero = if (esReanudada) {
        tableroDesdeServidor(eq1 = tablero_eq1!!, eq2 = tablero_eq2!!, trampa1 = trampa_eq1!!, trampa2 = trampa_eq2!!)
    } else {
        crearTableroInicial()
    }

    // El turno 0 o pares pertenecen al bando Azul (Equipo 1)
    val turnoActual = if ((turno ?: 0) % 2 == 0) EquipoID.AZUL else EquipoID.ROJO
    val faseP = if (esReanudada) FasePartida.JUGANDO else FasePartida.COLOCAR_TRAMPA

    var estadoAPriori = EstadoJuego (
        fasePartida = faseP,
        tablero = tablero,
        turnoActual = turnoActual,
        cartasJugador = cartas_jugador.map { convertirCarta(it) },
        cartasOponente = cartas_oponente.map { convertirCarta(it) },
        cartasSiguientes = carta_siguiente.map { convertirCarta(it) },
        cartasAccionPropia = cartas_accion_propia?.map { it.nombre } ?: emptyList(),
        cartasAccionRival = cartas_accion_rival?.map { it.nombre } ?: emptyList()
    )

    // Re-aplicación de estados lógicos místicas (Cartas de acción activadas antes de la desconexión)
    var estadoIntermedio = estadoAPriori
    var estadoAPosteriori = estadoAPriori
    val equipoContrario = if(equipoNuestro == EquipoID.AZUL) EquipoID.ROJO else EquipoID.AZUL

    if (cartas_accion_propia != null) {
        for(carta in cartas_accion_propia){
            if(carta.estado == "ACTIVA"){
                Log.d("LOG de partida", "Intentando aplicar carta acción propia: ${carta.nombre} al reanudar partida")
                estadoIntermedio = aplicarCartaAccion(estadoAPriori, equipoNuestro, carta.nombre, 0, 0, 0, 0, "", carta.accion)
                estadoIntermedio = estadoIntermedio.copy(cartaAccionYaUsada = true)
            }
            else if(carta.estado == "NO USABLE"){
                estadoIntermedio = estadoIntermedio.copy(cartaAccionYaUsada = true)
            }
        }
    }
    if(cartas_accion_rival != null){
        for(carta in cartas_accion_rival){
            if(carta.estado == "ACTIVA"){
                Log.d("LOG de partida", "Intentando aplicar carta acción del rival: ${carta.nombre} al reanudar partida")
                estadoAPosteriori = aplicarCartaAccion(estadoIntermedio, equipoContrario, carta.nombre, 0, 0, 0, 0, "", carta.accion)
            }
        }
    }

    return estadoAPosteriori
}

/** Envoltorio de retorno que describe el impacto y las consecuencias de una jugada. */
data class ResultadoMovimiento (
    val nuevoEstado: EstadoJuego,
    val capturado: Boolean,
    val esReyCapturado: Boolean,
    val victoriaPorTrono: Boolean
)

/**
 * Realiza el movimiento físico de una ficha, procesa capturas ordinarias y muertes por trampa.
 * Se encarga de la rotación cíclica de las cartas (la carta usada va al fondo de la reserva
 * y el jugador toma la carta del frente de la fila de espera).
 */
fun ejecutarMovimiento (
    estado: EstadoJuego,
    origen: Posicion,
    destino: Posicion,
    carta: Carta,
    equipoLocal: EquipoID,
    trampaActivada: Boolean = false
): ResultadoMovimiento {
    val tablero = estado.tablero.map { fila -> fila.toMutableList() }.toMutableList()
    val fichaMovida = tablero[origen.fila][origen.col].ficha!!

    var capturado = false
    var esReyCapturado = false

    // Validación si se pisa una trampa enemiga
    val esTrampaOponente = tablero[destino.fila][destino.col].esTrampaEquipo != null &&
            tablero[destino.fila][destino.col].esTrampaEquipo != fichaMovida.equipo.id

    if (trampaActivada || esTrampaOponente) {
        capturado = true
        esReyCapturado = fichaMovida.esRey

        // Destrucción de la ficha y cambio de la celda a estado Injugable (-1)
        tablero[destino.fila][destino.col] = tablero[destino.fila][destino.col].copy(ficha = null, esTrampaEquipo = -1)
        tablero[origen.fila][origen.col] = tablero[origen.fila][origen.col].copy(ficha = null)
    }
    else {
        // Captura tradicional por asalto de casillas
        val fichaDestino = tablero[destino.fila][destino.col].ficha
        if (fichaDestino != null) {
            capturado = true
            esReyCapturado = fichaDestino.esRey
        }

        tablero[destino.fila][destino.col] = tablero[destino.fila][destino.col].copy(ficha = fichaMovida)
        tablero[origen.fila][origen.col] = tablero[origen.fila][origen.col].copy(ficha = null)
    }

    /** Condición de Victoria "La Senda del Arroyo": El rey reclama el trono inicial del rival */
    val victoriaPorTrono = fichaMovida.esRey && destino.col == CENTRO &&
            ((fichaMovida.equipo == EquipoID.AZUL && destino.fila == 0) ||
                    (fichaMovida.equipo == EquipoID.ROJO && destino.fila == DIM - 1))

    val equipoActual = estado.turnoActual
    val cartasMovedor = if (equipoActual == equipoLocal) estado.cartasJugador else estado.cartasOponente
    val tieneCartaExtra = cartasMovedor.size > 2

    /** ─── Mecánica del Ciclo de Cartas de Onitama ─── */
    var cartaRecibida: Carta? = null
    val nuevasSiguientes: List<Carta>

    if (tieneCartaExtra) {
        // Si tiene más de dos cartas por efectos especiales, devuelve la usada al fondo sin robar
        nuevasSiguientes = estado.cartasSiguientes + carta
    } else {
        // Toma la primera de la cola [0] e inserta la usada en el extremo final [2]
        cartaRecibida = estado.cartasSiguientes[0]
        nuevasSiguientes = estado.cartasSiguientes.drop(1) + carta
    }

    // Actualiza los mazos de la interfaz según quién efectuó el movimiento
    val nuevasCartasJugador = if (equipoActual == equipoLocal) {
        if (tieneCartaExtra) {
            estado.cartasJugador.filter { it.nombre != carta.nombre }
        } else {
            estado.cartasJugador.map { if (it.nombre == carta.nombre) cartaRecibida!! else it }
        }
    } else estado.cartasJugador

    val nuevasCartasOponente = if (equipoActual != equipoLocal) {
        if (tieneCartaExtra) {
            estado.cartasOponente.filter { it.nombre != carta.nombre }
        } else {
            estado.cartasOponente.map { if (it.nombre == carta.nombre) cartaRecibida!! else it }
        }
    } else estado.cartasOponente

    // Fin de juego si el rey fue ejecutado o se tomó un trono
    val ganador = if (esReyCapturado || victoriaPorTrono) equipoActual else null
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

    // Revierte efectos ópticos de espejo si se cumplen las condiciones
    val estadoFinal = deshacerEspejoTrasMovimientoRival(nuevoEstado, equipoActual)

    return ResultadoMovimiento(estadoFinal, capturado, esReyCapturado, victoriaPorTrono)
}