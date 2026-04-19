package com.example.onitama.lib

import android.graphics.drawable.Drawable
import android.icu.text.CaseMap.toLower
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp




data class Movimiento(val dc: Int, val df:Int)

data class Carta(
    val nombre: String,
    val imagen: String,
    val movimientos: List<Movimiento>
)

object Cartas{
    val todas_cartas = listOf(
        Carta("Tigre",   "🐯", listOf(Movimiento(0, -1), Movimiento(0, 2))),
        Carta("Dragon",   "🐉", listOf(Movimiento(-2, 1), Movimiento( 1, 1), Movimiento(-1, -1), Movimiento(2, 1))),
        Carta("Rana",   "🐸", listOf(Movimiento(-1, 1), Movimiento(1, -1), Movimiento(-2, 0))),
        Carta("Conejo",   "🐰", listOf(Movimiento(1, 1), Movimiento(-1, 1), Movimiento(2, 0))),
        Carta("Cangrejo",   "🦀", listOf(Movimiento(-2, 0), Movimiento(2, 0),Movimiento(0,1))),
        Carta("Elefante",   "🐘", listOf(Movimiento(1, 1), Movimiento(-1, 1), Movimiento(1, 0),Movimiento(-1, 0))),
        Carta("Ganso", "🦆", listOf(Movimiento(-1,1),Movimiento(-1,0),Movimiento(1,0),Movimiento(1,-1))),
        Carta("Gallo",   "🐓", listOf(Movimiento(1, 0),Movimiento(-1,0),Movimiento(-1,-1),Movimiento(1,1))),
        Carta("Mono",   "🐒", listOf(Movimiento(1, 1), Movimiento(-1, -1), Movimiento(-1, 1), Movimiento( 1,-1))),
        Carta("Mantis", "🦗", listOf(Movimiento(0, -1), Movimiento(-1,-1),Movimiento(1,1))),
        Carta("Caballo",   "🐴", listOf(Movimiento(0, 1), Movimiento(0, -1), Movimiento(-1, 0))),
        Carta("Buey", "🐂", listOf(Movimiento(0, 1), Movimiento(0,-1),Movimiento(1,0))),
        Carta("Grulla",   "🦢", listOf(Movimiento(1,-1),Movimiento(-1,-1), Movimiento(0,1))),
        Carta("Oso",   "🐻", listOf(Movimiento(1, 0), Movimiento(-1, 0), Movimiento(0, 1))),
        Carta("Aguila",   "🦅", listOf(Movimiento(-1, 0), Movimiento(-1, 1), Movimiento(-1,-1))),
        Carta("Cobra",   "🐍", listOf(Movimiento(-1, 0), Movimiento(1, 1), Movimiento(1,-1))),
        Carta("Murcielago", "", listOf(Movimiento(-1, -1), Movimiento(1, 1), Movimiento(0, -3)))

    )
    fun selectRandomCards(n: Int):List<Carta>{
        return todas_cartas.shuffled().take(n)

    }

    fun imagenCarta(carta: Carta): String {
        return carta.nombre.lowercase()
    }

    fun getCarta(nombre: String): Carta {
        return todas_cartas.find { it.nombre == nombre }!!
    }

}