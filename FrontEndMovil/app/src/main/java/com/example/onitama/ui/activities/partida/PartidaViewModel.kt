package com.example.onitama.ui.activities.partida

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.PartidaActiva
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.api.Partida
import com.example.onitama.api.jsonPartida
import com.example.onitama.lib.Carta
import com.example.onitama.lib.Cartas
import com.example.onitama.lib.Dificultad
import com.example.onitama.lib.EquipoID
import com.example.onitama.lib.EstadoJuego
import com.example.onitama.lib.FasePartida
import com.example.onitama.lib.JugadaIA
import com.example.onitama.lib.ModoJuego
import com.example.onitama.lib.Posicion
import com.example.onitama.lib.calcularMejorMovimientoIA
import com.example.onitama.lib.calcularMovimientosValidos
import com.example.onitama.lib.crearEstadoInicial
import com.example.onitama.lib.crearEstadoServidor
import com.example.onitama.lib.ejecutarMovimiento
import com.example.onitama.lib.invertirCartasEspejo
import com.example.onitama.lib.activarRestriccionSolo
import com.example.onitama.lib.TipoRestriccion
import com.example.onitama.AutoLogin
import com.example.onitama.lib.aplicarCartaAccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class PartidaViewModel : ViewModel() {

    val END = 6;
    var modoJuegoActual: ModoJuego = ModoJuego.BOT
        private set
    var nivelDificultadBot: Dificultad = Dificultad.FACIL
        private set

    private val _estado = MutableStateFlow(crearEstadoInicial())
    var razon: String? = null
    val estado: StateFlow<EstadoJuego> = _estado.asStateFlow()

    var cartaAccionEnUso: String? = null
    private var sacrificioPieza: Posicion? = null
    private var estadoAntesCartaAccion: EstadoJuego? = null

    private val _mensajeCartaAccion = MutableStateFlow<String>("")
    val mensajeCartaAccion = _mensajeCartaAccion.asStateFlow()

    var mostrarPopPausa by mutableStateOf(false)
        private set

    private val _notificacionPausa = MutableStateFlow<Partida.RespuestaSolicitudPausa?>(null)
    val notificacionPausa = _notificacionPausa.asStateFlow()

    var mostrarPopCancel by mutableStateOf(false)
        private set

    var partida = Partida()

    var equipoPropio = EquipoID.AZUL //de momento el bot siempre es el rojo, ya si eso se mejorará más adelante



    fun iniciarPartida(modo: ModoJuego, dificultad: Dificultad = Dificultad.FACIL) {
        modoJuegoActual = modo
        nivelDificultadBot = dificultad

        val datos = PartidaActiva.datosPartida

        if (modo == ModoJuego.PUBLICA || modo == ModoJuego.PRIVADA) {
            Log.i("INFORMACION PARTIDA INICIADA", "{s}")
            if (datos != null) {
                equipoPropio = if (datos.equipo == 1) EquipoID.AZUL else EquipoID.ROJO

                // Primero construimos el tablero con las cartas del servidor
                val esNueva = (modo == ModoJuego.PUBLICA || modo == ModoJuego.PRIVADA && (datos.trampa_j2_pos== null ) && (datos.trampa_j1_pos == null))
                _estado.value = crearEstadoServidor(
                    cartas_jugador = datos.cartas_jugador.map { it.nombre },
                    cartas_oponente = datos.cartas_oponente.map { it.nombre },
                    carta_siguiente = datos.carta_siguiente.map { it.nombre },
                    tablero_eq1 = datos.tablero_eq1,
                    tablero_eq2 = datos.tablero_eq2,
                    turno = datos.turno,
                    cartas_accion_propia = datos.cartas_accion_jugador,
                    cartas_accion_rival = datos.cartas_accion_oponente,
                    esReanudada = !esNueva,
                    trampa_eq1 = datos.trampa_j1_pos,
                    trampa_eq2 = datos.trampa_j2_pos,
                    equipoNuestro = equipoPropio
                )



                // Luego conectamos el WebSocket para escuchar los turnos
                conectarAlServidor()
            }
        } else {
            // Es contra el Bot
            equipoPropio = EquipoID.AZUL // El jugador local
            _estado.value = crearEstadoInicial()
        }
    }

    private fun conectarAlServidor() {


        val sePudoEnviar = partida.enviarEstoyListo()
        Log.w("CHIVATO_WS", "¿Se pudo enviar ESTOY_LISTO?: $sePudoEnviar")

        if (sePudoEnviar) {
            viewModelScope.launch {
                ManejadorGlobal.mensajesEntrantes.collect { json ->
                    val jsonTolerante = Json {
                        ignoreUnknownKeys = true
                        classDiscriminator = "tipo"
                    }

                    try {

                        val mensaje = jsonTolerante.decodeFromString<Partida.MensajeServidor>(json.toString())

                        Log.w("CHIVATO_WS", "El ViewModel está procesando: $mensaje")

                        val actual = _estado.value
                        when (mensaje) {
                            is Partida.RespuestaTuTurno -> {
                                _estado.value = actual.copy(turnoActual = equipoPropio)
                                _mensajeCartaAccion.value = "¡Es tu turno!"
                            }

                            is Partida.RespuestaMover -> {
                                val filaOrigen = END - mensaje.fila_origen
                                val colOrigen = END - mensaje.col_origen
                                val origen = Posicion(filaOrigen, colOrigen)

                                val filaDestino = END - mensaje.fila_destino
                                val colDestino = END - mensaje.col_destino
                                val destino = Posicion(filaDestino, colDestino)
                                val carta = Cartas.getCarta(mensaje.carta)

                                Log.i("conexion servidor", "Mensaje de movimiento recibido")

                                val resultado = ejecutarMovimiento(
                                    estado = actual, 
                                    origen, 
                                    destino, 
                                    carta, 
                                    equipoPropio, 
                                    trampaActivada = mensaje.trampa_activada?: false
                                )

                                if(resultado.victoriaPorTrono){
                                    razon = "TRONO"
                                }
                                if(resultado.esReyCapturado) {
                                    razon = "REY CAPTURADO"
                                }
                                _estado.value = resultado.nuevoEstado.copy(turnoActual = equipoPropio)
                            }

                            is Partida.RespuestaMovimientoInvalido -> {
                                Log.e("Partida", "Error: Movimiento inválido")
                                desSeleccionarCarta()
                            }

                            is Partida.RespuestaDerrota -> {
                                razon = when (mensaje.motivo) {
                                    "SIN_MOV" -> "SIN_MOVIMIENTOS"
                                    "ABANDONO" -> "ABANDONO"
                                    "REY_EN_TRAMPA" -> "REY_EN_TRAMPA"
                                    "TIEMPO_AGOTADO" -> "TIEMPO_AGOTADO"
                                    "FIN_PARTIDA" -> when {
                                        razon == "TRONO" || razon == "REY CAPTURADO" -> razon!!
                                        else -> "FIN_PARTIDA"
                                    }

                                    else -> when {
                                        razon == "TRONO" || razon == "REY CAPTURADO" -> razon!!
                                        else -> "FIN_PARTIDA"
                                    }
                                }

                                _estado.value = _estado.value.copy(
                                    ganador = if (equipoPropio == EquipoID.AZUL) EquipoID.ROJO else EquipoID.AZUL,
                                    fasePartida = FasePartida.TERMINADA
                                )
                                Log.i("conexion servidor", "Mensaje de derrota recibido")
                            }

                            is Partida.RespuestaVictoria -> {
                                when (mensaje.motivo) {
                                    "ABANDONO" -> razon = "ABANDONO"
                                    "SIN_MOV" -> razon = "SIN_MOVIMIENTOS"
                                    "REY_EN_TRAMPA" -> razon = "REY_EN_TRAMPA"
                                    "TIEMPO_AGOTADO" -> razon = "TIEMPO_AGOTADO"
                                    "FIN_PARTIDA" -> {
                                        if (razon == null) razon = "FIN_PARTIDA"
                                    }
                                    else -> {
                                        if (razon == null) razon = "FIN_PARTIDA"
                                    }
                                }
                                _estado.value = _estado.value.copy(
                                    ganador = if (equipoPropio == EquipoID.ROJO) EquipoID.ROJO else EquipoID.AZUL,
                                    fasePartida = FasePartida.TERMINADA
                                )
                                Log.i("conexion servidor", "Mensaje de victoria recibido")
                            }

                            is Partida.RespuestaTerminarPartida ->{
                                if (mensaje.razon == "ABANDONO"){ razon ="ABANDONO" }
                                Log.i("conexion servidor", "Mensaje de terminar partida recibido")
                                _estado.value = _estado.value.copy(
                                    ganador = if (mensaje.ganador == EquipoID.ROJO.id.toString()) EquipoID.ROJO else EquipoID.AZUL,
                                    fasePartida = FasePartida.TERMINADA
                                )
                            }

                            is Partida.RespuestaSolicitudPausa -> {
                                _notificacionPausa.value = mensaje
                            }

                            is Partida.RespuestaPartidaPausada -> {
                                mostrarPopPausa = true
                            }

                            is Partida.RespuestaPartidaCancelada -> {
                                mostrarPopCancel = true

                            }

                            is Partida.RespuestaPausaRechazada -> { }

                            is Partida.RespuestaPartidaLista -> {
                                _estado.value = actual.copy(
                                    fasePartida = FasePartida.JUGANDO,
                                    cartasAccionPropia = mensaje.cartas_accion.map { it.nombre },
                                    cartaAccionInicialElegida = null,
                                    cartaAccionYaUsada = false 
                                )
                            }

                            is Partida.RespuestaSeleccioneCartaAccion -> {
                                _estado.value = actual.copy(
                                    fasePartida = FasePartida.ELEGIR_CARTA_ACCION,
                                    cartasAccionPropia = mensaje.cartas_accion.map { it.nombre }
                                )
                            }


                            is Partida.RespuestaTrampaActivada -> {
                                val fila = END - mensaje.fila
                                val columna = END - mensaje.columna

                                val nuevoTablero = actual.tablero.map {
                                    it.toMutableList()
                                }.toMutableList()

                                nuevoTablero[fila][columna] = nuevoTablero[fila][columna].copy(
                                    ficha = null,
                                    esTrampaEquipo = -1
                                )

                                _estado.value = actual.copy(
                                    tablero = nuevoTablero
                                )
                            }

                            is Partida.RespuestaCartaAccionJugada -> {
                                val jugador = if (equipoPropio == EquipoID.AZUL) {
                                    EquipoID.ROJO
                                }
                                else {
                                    EquipoID.AZUL
                                }
                                Log.d("LOG", "Carta accion jugada por el rival: ${mensaje.cartaAccion}")
                                if(mensaje.accion == "ROBAR"){
                                    Log.d("LOG", "Carta robada por el rival: ${mensaje.cartaRobar}")
                                }
                                val tipoAccion = mensaje.accion
                                Log.d("LOG", "Tipo de accion: $tipoAccion")
                                val nuevoEstado = aplicarCartaAccion(
                                    estado = _estado.value,
                                    equipo = jugador,
                                    cartaNombre = mensaje.cartaAccion,
                                    x = if (mensaje.x != -1) END - mensaje.x else -1,
                                    y = if (mensaje.y != -1) END - mensaje.y else -1,
                                    x_op = if (mensaje.x_op != -1) END - mensaje.x_op else -1,
                                    y_op = if (mensaje.y_op != -1) END - mensaje.y_op else -1,
                                    cartaRobar = mensaje.cartaRobar,
                                    tipo = tipoAccion
                                )

                                _estado.value = nuevoEstado.copy(
                                    cartaAccionYaUsada = if (jugador == equipoPropio) true else actual.cartaAccionYaUsada,
                                    modoAccion = null
                                )

                                if (jugador == equipoPropio) {
                                    cartaAccionEnUso = null
                                    sacrificioPieza = null
                                }
                            }

                            is Partida.RespuestaTrampaInvalida -> {
                                Log.e("Partida", "Error: No se puede poner una trampa en esa casilla")
                            }

                            is Partida.RespuestaCartaAccionInvalida -> {
                                Log.e("Partida", "Error: No puedes usar esta carta de acción ahora")
                                if (estadoAntesCartaAccion != null) {
                                    _estado.value = estadoAntesCartaAccion!!
                                    estadoAntesCartaAccion = null
                                    cartaAccionEnUso = null
                                    sacrificioPieza = null
                                }
                            }
                            is Partida.RespuestaPeonMuerto -> {
                                Log.d("Partida", "has colocado el peon resucitado en una casilla trampa")
                                val fila = END - mensaje.pos_y
                                val columna = END - mensaje.pos_x
                                var turnoActual = equipoPropio
                                if (fila > 3){
                                    turnoActual = if (equipoPropio == EquipoID.AZUL) EquipoID.ROJO else EquipoID.AZUL
                                }
                                val nuevoTablero = actual.tablero.map {
                                    it.toMutableList()
                                }.toMutableList()

                                nuevoTablero[fila][columna] = nuevoTablero[fila][columna].copy(
                                    ficha = null,
                                    esTrampaEquipo = -1
                                )

                                // Solo actualizamos el tablero y limpiamos la UI de la carta,
                                // respetando el turno que ya había avanzado 'ejecucionCartaAccion'
                                _estado.value = actual.copy(
                                    modoAccion = null,
                                    tablero = nuevoTablero,
                                    turnoActual = turnoActual
                                )
                            }

                            else -> {
                                println("LOG: Mensaje recibido no reconocido: $mensaje")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CHIVATO_WS", "Fallo al decodificar el mensaje: ${json.toString()}", e)
                        println("Mensaje ignorado (no pertenece a la lógica de partida)")
                    }
                }
            }
        }
    }

    fun tocarCelda(pos: Posicion) {
        val actual = _estado.value
        Log.d("LOG", "Toque en $pos durante fase ${actual.fasePartida}")
        when (actual.fasePartida) {
            FasePartida.COLOCAR_TRAMPA -> {
                if (actual.posicionTrampa != null) {
                    return
                }

                val hayFicha = actual.tablero[pos.fila][pos.col]

                if (hayFicha.ficha != null) {
                     val mensaje = "No puedes colocar la trampa sobre una pieza."

                    _estado.value = actual.copy(
                        mensajeErrorTrampa = mensaje,
                        posicionErrorTrampa = pos
                    )
                    
                    viewModelScope.launch {
                        delay(2000)
                        val estadoActual = _estado.value
                        if (estadoActual.posicionErrorTrampa == pos) {
                            _estado.value = estadoActual.copy(
                                mensajeErrorTrampa = null,
                                posicionErrorTrampa = null
                            )
                        }
                    }
                    return
                }

                val sePuede = if (equipoPropio == EquipoID.AZUL) {
                    pos.fila == 4 || pos.fila == 5
                }
                else {
                    pos.fila == 1 || pos.fila == 2
                }

                if (sePuede) {
                    _estado.value = actual.copy(
                        posicionTrampa = pos,
                        mensajeErrorTrampa = null,
                        posicionErrorTrampa = null
                    )

                    partida.enviarPonerTrampa(
                        equipo = equipoPropio.id,
                        fila = END - pos.fila,
                        columna = END - pos.col
                    )
                    
                } else {
                    val mensaje = "Debe colocarse en la 2º o 3º fila de tu lado."

                    _estado.value = actual.copy(
                        mensajeErrorTrampa = mensaje,
                        posicionErrorTrampa = pos
                    )
                    
                    viewModelScope.launch {
                        delay(2000)
                        val estadoActual = _estado.value
                        if (estadoActual.posicionErrorTrampa == pos) {
                            _estado.value = estadoActual.copy(
                                mensajeErrorTrampa = null,
                                posicionErrorTrampa = null
                            )
                        }
                    }
                }
            }

            FasePartida.JUGANDO -> {
                val celda = actual.tablero[pos.fila][pos.col]
                if (actual.modoAccion != null) {
                    Log.d("LOG", "Se está usando la carta ${actual.modoAccion} con la celda ${pos}")
                    val cartaAccion = actual.modoAccion ?: return
                    val nombreCarta = cartaAccionEnUso ?: return

                    when (cartaAccion) {
                        "REVIVIR" -> {
                            if (celda.ficha == null && ((pos.fila >=3 && equipoPropio == EquipoID.AZUL) || (pos.fila <=3 && equipoPropio == EquipoID.ROJO))) {
                                Log.d("LOG", "Eligiendo Casilla segura columna ${pos.col} y fila ${pos.fila}")
                                _mensajeCartaAccion.value = ""
                                ejecucionCartaAccion(nombreCarta, cartaAccion, posicionPropia = pos)
                            } else {
                                _mensajeCartaAccion.value = "¡Elige una en tu mitad del campo!"
                                return
                            }
                        }

                        "SACRIFICIO" -> {

                            if (sacrificioPieza == null){

                                if (celda.ficha != null && celda.ficha.equipo == actual.turnoActual) {
                                    Log.d("LOG", "Sacrificio, eligiendo peon a sacrificar")
                                    sacrificioPieza = pos
                                    _mensajeCartaAccion.value = "Ahora selecciona el peón rival"
                                } else {
                                    _mensajeCartaAccion.value = "¡Esa no es una de tus piezas!"
                                    return
                                }
                            }
                            else if (celda.ficha != null && celda.ficha.equipo != equipoPropio) {
                                Log.d("LOG", "Sacrificio, eligiendo peon del rival a asesinar")
                                _mensajeCartaAccion.value = ""
                                ejecucionCartaAccion(nombreCarta, cartaAccion, posicionPropia = sacrificioPieza, posicionRival = pos)
                            } else {
                                _mensajeCartaAccion.value = "¡Debes seleccionar un peón rival válido!"
                            }
                        }

                        "SALVAR_REY" -> {
                            if (celda.ficha == null && ((pos.fila >=3 && equipoPropio == EquipoID.AZUL) || (pos.fila <=3 && equipoPropio == EquipoID.ROJO))){
                                Log.d("LOG", "Eligiendo Casilla segura")
                                _mensajeCartaAccion.value = ""
                                ejecucionCartaAccion(nombreCarta, cartaAccion, posicionPropia = pos)
                            } else {
                                _mensajeCartaAccion.value = "¡Elige una vacía en tu mitad del campo!"
                            }
                        }
                        "ROBAR" -> {
                            _mensajeCartaAccion.value = "¡Toca una de las cartas del rival para robarla, no el tablero!"
                        }
                    }
                }
                else {
                    Log.d("LOG", "modoaccion es null y el turno es ${actual.turnoActual}")
                    //si le toca al oponente se ignoran los clicks
                    if(actual.turnoActual == equipoPropio){
                        Log.d("LOG", "Nos toca")
                    // Si ya hay algo seleccionado y el destino es válido, movemos
                        if (actual.movimientosValidos.contains(pos) && actual.fichaSeleccionada != null && actual.cartaSeleccionada != null) {
                            Log.d("LOG", "Toque en $pos durante fase ${actual.fasePartida} con carta seleccionada ${actual.cartaSeleccionada.nombre} y la ficha seleccionada ${actual.fichaSeleccionada}")
                            val resultado = ejecutarMovimiento(
                                actual,
                                actual.fichaSeleccionada,
                                pos,
                                actual.cartaSeleccionada,
                                equipoPropio
                            )
                            if(resultado.victoriaPorTrono){
                                razon = "TRONO"
                            }
                            if(resultado.esReyCapturado){
                                razon = "REY CAPTURADO"
                            }
                            _estado.value = resultado.nuevoEstado

                            if (resultado.nuevoEstado.turnoActual != equipoPropio) {
                                if (modoJuegoActual == ModoJuego.BOT) {
                                    if (resultado.nuevoEstado.ganador == null) {
                                        jugarTurnoBot()
                                    }
                                }
                                else {
                                    partida.enviarMovimiento(
                                        Partida.MensajeMover(
                                            equipo = equipoPropio.id,
                                            col_origen = END - actual.fichaSeleccionada.col ,
                                            fila_origen = END - actual.fichaSeleccionada.fila ,
                                            col_destino = END - pos.col,
                                            fila_destino = END - pos.fila,
                                            carta = actual.cartaSeleccionada.nombre
                                        ))
                                }
                            }
                        }
                        else if (actual.cartaSeleccionada != null) {
                            Log.d("LOG", "Toque en $pos durante fase ${actual.fasePartida} con carta seleccionada ${actual.cartaSeleccionada.nombre}")
                            Log.d("LOG", "en esa celda hay una ficha? ${celda.ficha != null}")
                            if (celda.ficha?.equipo == actual.turnoActual) {
                                Log.d("LOG", "en esa celda hay una ficha tuya, todo bien")
                                _estado.value = actual.copy(
                                    fichaSeleccionada = pos,
                                    movimientosValidos = calcularMovimientosValidos(
                                        actual.tablero, 
                                        pos.fila,
                                        pos.col, 
                                        actual.cartaSeleccionada, 
                                        actual.turnoActual,
                                        actual.restriccionSolo
                                    )
                                )
                            }
                        }
                    }else{
                        Log.d("LOG", "No nos toca")
                    }
                }
            }
            else -> {}
        }
    }

    fun elegirCartaAccionInicial(
        nombreCarta: String
    ) {
        val actual = _estado.value

        if (actual.cartaAccionInicialElegida != null) {
            return
        }
        
        if (actual.fasePartida == FasePartida.ELEGIR_CARTA_ACCION) {
            _estado.value = actual.copy(
                cartaAccionInicialElegida = nombreCarta
            )
            
            partida.enviarSeleccionAccion(
                nombreCarta, 
                equipoPropio.id
            )        
        }
    }

    fun seleccionarCarta(carta: Carta) {
        val actual = _estado.value


        if (actual.fichaSeleccionada != null) {
            // 1. Calculamos los movimientos con la ficha actual y la carta nueva
            val posibles = calcularMovimientosValidos(
                actual.tablero,
                actual.fichaSeleccionada.fila,
                actual.fichaSeleccionada.col,
                carta,
                actual.turnoActual,
                actual.restriccionSolo
            )

            println("LOG: Carta seleccionada -> ${carta.nombre}. Movimientos hallados: ${posibles.size}")

            // 2. Actualizamos el estado UNA SOLA VEZ con copy
            _estado.value = actual.copy(
                cartaSeleccionada = carta,
                movimientosValidos = posibles
            )
        } else {
            println("LOG: Carta seleccionada -> ${carta.nombre}.")
            // Como la idea es que se seleccione primero la carta y luego la ficha, pero los movimientos solo se reslatan si hay ficha, la carta se selecciona igualmente
            _estado.value = actual.copy(cartaSeleccionada = carta)
        }
    }

    fun desSeleccionarCarta() {
        val actual = _estado.value
        _estado.value = actual.copy(
            cartaSeleccionada = null, 
            fichaSeleccionada = null, 
            movimientosValidos = emptyList(),
            modoAccion = null
        )
    }

    fun activarCartaAccion (
        nombreCarta: String
    ) {
        val actual = _estado.value

        if (actual.turnoActual == equipoPropio && !actual.cartaAccionYaUsada) {
            val carta = obtenerCartaAccion(nombreCarta) ?: return

            cartaAccionEnUso = nombreCarta
        
            if (carta == "ESPEJO" || 
                carta == "CEGAR" ||
                carta == "SOLO_PARA_ADELANTE" ||
                carta == "SOLO_PARA_ATRAS") {
                ejecucionCartaAccion(nombreCarta, carta)
            }   
            else {
                _estado.value = actual.copy(
                    modoAccion = carta
                )
                _mensajeCartaAccion.value = when (carta) {
                    "REVIVIR" -> "Selecciona una casilla vacía de tu campo." 
                    "SACRIFICIO" -> "Selecciona uno de tus peones."
                    "SALVAR_REY" -> "Selecciona destino para tu rey."
                    else -> ""
                }
            }
        }
        else {
            return
        }
    }

    private fun obtenerCartaAccion(
        nombre: String
    ): String? {
        return when (nombre) {
            "Pensatorium" -> "ESPEJO"
            "Atrapasueños" -> "ROBAR"
            "Requiem" -> "SACRIFICIO"
            "Santo Grial" -> "REVIVIR"
            "La Dama del Mar" -> "SOLO_PARA_ADELANTE"
            "Finisterra" -> "SOLO_PARA_ATRAS"
            "Brujeria" -> "CEGAR"
            "Illusia" -> "SALVAR_REY"
            else -> null
        }
    }

    fun ejecucionCartaAccion(
        nombreCarta: String,
        cartaAccion: String,
        posicionPropia: Posicion? = null,
        posicionRival: Posicion? = null,
        cartaARobar: String = ""
    ) {
        val mensaje = Partida.MensajeJugarCartaAccion(
            nombreCarta,
            equipoPropio.id,
            x = if (posicionPropia != null) END - posicionPropia.col else -1,
            y = if (posicionPropia != null) END - posicionPropia.fila else -1,
            x_op = if (posicionRival != null) END - posicionRival.col else -1,
            y_op = if (posicionRival != null) END - posicionRival.fila else -1,
            cartaRobar = cartaARobar
        )

        estadoAntesCartaAccion = _estado.value

        val estadoConAccion = aplicarCartaAccion(
            estado = _estado.value,
            equipo = equipoPropio,
            cartaNombre = nombreCarta,
            x = if (posicionPropia != null) posicionPropia.col else -1,
            y = if (posicionPropia != null) posicionPropia.fila else -1,
            x_op = if (posicionRival != null) posicionRival.col else -1,
            y_op = if (posicionRival != null) posicionRival.fila else -1,
            cartaRobar = cartaARobar,
            tipo = cartaAccion
        )

        val turnoSiguiente = if (cartaAccion == "ROBAR") _estado.value.turnoActual else if(equipoPropio == EquipoID.AZUL) EquipoID.ROJO else EquipoID.AZUL
        _estado.value = estadoConAccion.copy(
            modoAccion = null,
            cartaAccionYaUsada = true,
            turnoActual = turnoSiguiente
        )

        val sePuedeJugar = partida.enviarJugarCartaAccion(mensaje)
        if (!sePuedeJugar) {
            _estado.value = estadoAntesCartaAccion!!
            estadoAntesCartaAccion = null
            cartaAccionEnUso = null
            sacrificioPieza = null
        } else {
            estadoAntesCartaAccion = null
        }
    }


    fun activarPausa() {
        val datos = PartidaActiva.datosPartida ?: return

        val jugador = AutoLogin.sesion.value?.nombre ?: "Jugador"
        val rival = datos.oponente
        val idPartida = datos.partida_id

        val exito = partida.enviarSolicitudPausa(
            jugador,
            rival,
            idPartida
        )
    }

    fun enviarAceptarPausa(
        idNotificacion: Int,
        miNombre: String
    ) {
        val exito = partida.enviarAceptarPausa(
            idNotificacion,
            miNombre
        )

        if (exito) {
            _notificacionPausa.value = null
        }
    }

    fun enviarRechazarPausa(
        idNotificacion: Int,
        miNombre: String
    ) {
        val exito = partida.enviarRechazarPausa(
            idNotificacion,
            miNombre
        )

        if (exito) {
            _notificacionPausa.value = null
        }
    }

    private fun jugarTurnoBot() {
        val estadoActual = _estado.value

        // Por seguridad, comprobamos que realmente es el turno del bot y no hay ganador
        val ia = if (equipoPropio == EquipoID.ROJO) EquipoID.AZUL else EquipoID.ROJO
        if (estadoActual.turnoActual != ia || estadoActual.ganador != null) return

        // Lanzamos una corrutina en el hilo Default (optimizado para cálculos pesados de IA)
        viewModelScope.launch(Dispatchers.Default) {

            delay(2000)
            //Calculamos la jugada
            var jugada = calcularMejorMovimientoIA(
                estado = estadoActual,
                // De momento así luego ya si eso añadimos un menú similar al que tenían las partidas privadas para elegir dificultad y equipo que quieres jugar
                equipoIA = ia,
                equipoLocal = equipoPropio,
                dificultad = nivelDificultadBot
            )
            Log.i("LOG BOT", "Jugada calculada: $jugada")


            // 3. Aplicamos la jugada en el hilo principal
            if (jugada != null) {
                withContext(Dispatchers.Main) {
                    aplicarJugadaEnEstado(jugada)
                }
            }
        }
    }

    private fun aplicarJugadaEnEstado(jugada: JugadaIA) {
        val actual = _estado.value

        val posicionOrigen = Posicion(jugada.origenFila, jugada.origenCol)
        val posicionDestino = Posicion(jugada.destinoFila, jugada.destinoCol)

        val resultado = ejecutarMovimiento(
            estado = actual,
            origen = posicionOrigen,
            destino = posicionDestino,
            carta = jugada.carta,
            equipoPropio
        )

        _estado.value = resultado.nuevoEstado
    }

    override fun onCleared() {
        super.onCleared()
        partida.desconectarPartida()
        println("LOG: ViewModel destruido, conexión WebSocket limpiada.")
    }

    fun botonAbandonar(){
        partida.enviarAbandono(equipoPropio.id)
        razon = "ABANDONO"
    }

}