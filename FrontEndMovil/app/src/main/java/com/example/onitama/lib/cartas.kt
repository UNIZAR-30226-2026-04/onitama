package com.example.onitama.lib

data class Movimiento(val dc: Int, val df:Int)

data class carta(
    val nombre: String,
    val imagen: String,
    val movimientos: List<Movimiento>
)

object cartas{
    val todas_cartas = listOf(
        carta("Tigre",   "🐯", listOf(Movimiento(0, -1), Movimiento(0, 2))),
        carta("Dragon",   "🐉", listOf(Movimiento(-2, 1), Movimiento( -1, -1), Movimiento(1, 1), Movimiento(2, 1))),
        carta("Rana",   "🐸", listOf(Movimiento(-1, 1), Movimiento(1, -1), Movimiento(-2, 0))),
        carta("Conejo",   "🐰", listOf(Movimiento(1, 1), Movimiento(-1, -1), Movimiento(2, 0))),
        carta("Cangrejo",   "🦀", listOf(Movimiento(-2, 0), Movimiento(2, 0),Movimiento(0,1))),
        carta("Elefante",   "🐘", listOf(Movimiento(1, 1), Movimiento(-1, 1), Movimiento(1, 0),Movimiento(-1, 0))),
        carta("Ganso", "🦆", listOf(Movimiento(-1,1),Movimiento(-1,0),Movimiento(1,0),Movimiento(1,-1))),
        carta("Gallo",   "🐓", listOf(Movimiento(1, 0),Movimiento(-1,0),Movimiento(-1,-1),Movimiento(1,1))),
        carta("Mono",   "🐒", listOf(Movimiento(1, 1), Movimiento(-1, -1), Movimiento(-1, 1), Movimiento( 1,-1))),
        carta("Mantis", "🦗", listOf(Movimiento(0, -1), Movimiento(-1,-1),Movimiento(1,1))),
        carta("Caballo",   "🐴", listOf(Movimiento(0, 1), Movimiento(0, -1), Movimiento(-1, 0))),
        carta("Buey", "🐂", listOf(Movimiento(0, 1), Movimiento(0,-1),Movimiento(1,0))),
        carta("Grulla",   "🦢", listOf(Movimiento(1,-1),Movimiento(-1,-1), Movimiento(0,1))),
        carta("Oso",   "🐻", listOf(Movimiento(1, 0), Movimiento(-1, 0), Movimiento(0, 1))),
        carta("Aguila",   "🦅", listOf(Movimiento(-1, 0), Movimiento(-1, 1), Movimiento(-1,-1))),
        carta("Cobra",   "🐍", listOf(Movimiento(-1, 0), Movimiento(1, 1), Movimiento(1,-1)))

    )
    fun selectRandomCards(n: Int):List<carta>{
        return todas_cartas.shuffled().take(n)

    }

}