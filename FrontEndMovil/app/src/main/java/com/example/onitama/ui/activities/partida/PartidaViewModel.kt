package com.example.onitama.ui.activities.partida

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onitama.PartidaActiva
import com.example.onitama.lib.Carta
import com.example.onitama.lib.Dificultad
import com.example.onitama.lib.EquipoID
import com.example.onitama.lib.EstadoJuego
import com.example.onitama.lib.JugadaIA
import com.example.onitama.lib.Posicion
import com.example.onitama.lib.calcularMejorMovimientoIA
import com.example.onitama.lib.calcularMovimientosValidos
import com.example.onitama.lib.crearEstadoInicial
import com.example.onitama.lib.crearEstadoServidor
import com.example.onitama.lib.ejecutarMovimiento
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PartidaViewModel : ViewModel() {

    private val _estado = MutableStateFlow(configurarEstadoInicial())
    val estado: StateFlow<EstadoJuego> = _estado.asStateFlow()
    val equipoBot = EquipoID.ARROJO //de momento el bot siempre es el rojo, ya si eso se merjorará más adelante


    private fun configurarEstadoInicial(): EstadoJuego {
        val datos = PartidaActiva.datosPartida

        return if (datos != null) {
            // PARTIDA PÚBLICA: Usamos los datos del servidor
            crearEstadoServidor(
                cartas_jugador = datos.cartas_jugador,
                cartas_oponente = datos.cartas_oponente,
                carta_siguiente = datos.carta_siguiente,
                equipo = datos.obtenerEquipoID()
            )
        } else {
            crearEstadoInicial()
        }
    }


    fun tocarCelda(pos: Posicion) {
        val actual = _estado.value
        //si le toca al bot se ignoran los clicks
        if(actual.turnoActual != equipoBot){
        // Si ya hay algo seleccionado y el destino es válido, movemos
            if (actual.movimientosValidos.contains(pos) && actual.fichaSeleccionada != null && actual.cartaSeleccionada != null) {
                val resultado = ejecutarMovimiento(
                    actual,
                    actual.fichaSeleccionada,
                    pos,
                    actual.cartaSeleccionada
                )
                _estado.value = resultado.nuevoEstado

                if (resultado.nuevoEstado.turnoActual == equipoBot && resultado.nuevoEstado.ganador == null) {
                    jugarTurnoBot()
                }
            }
            else if(actual.cartaSeleccionada != null){
                val celda = actual.tablero[pos.fila][pos.col]
                if (celda.ficha?.equipo == actual.turnoActual) {
                    _estado.value = actual.copy(
                        fichaSeleccionada = pos,
                        movimientosValidos = calcularMovimientosValidos(actual.tablero, pos.fila, pos.col, actual.cartaSeleccionada!!, actual.turnoActual)
                    )
                }
            }
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
                actual.turnoActual
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
        _estado.value = actual.copy(cartaSeleccionada = null, fichaSeleccionada = null, movimientosValidos = emptyList())
    }


    private fun jugarTurnoBot() {
        val estadoActual = _estado.value

        // Por seguridad, comprobamos que realmente es el turno del bot y no hay ganador
        if (estadoActual.turnoActual != EquipoID.ARROJO || estadoActual.ganador != null) return

        // Lanzamos una corrutina en el hilo Default (optimizado para cálculos pesados de IA)
        viewModelScope.launch(Dispatchers.Default) {


            //Calculamos la jugada
            val jugada = calcularMejorMovimientoIA(
                estado = estadoActual,
                // De momento así luego ya si eso añadimos un menú similar al que tenían las partidas privadas para elegir dificultad y equipo que quieres jugar
                equipoIA = equipoBot,
                equipoLocal = EquipoID.ABAZUL,
                dificultad = Dificultad.MEDIO
            )

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
            carta = jugada.carta
        )

        _estado.value = resultado.nuevoEstado
    }

}