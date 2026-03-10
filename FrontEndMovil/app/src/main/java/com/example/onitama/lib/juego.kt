package com.example.onitama.lib


// ─── Constantes del tablero ───────────────────────────────────────────────────
const val DIM = 7
const val CENTRO = (DIM/2)

// ─── Tipos ────────────────────────────────────────────────────────────────────

enum class EquipoID (val id: Int){
    ID1(1),
    ID2(2)
}

interface Ficha{
    val equipo: EquipoID
    val esRey: Boolean
}

interface Celda {
    val ficha: Ficha?
    /** true si esta casilla es el trono de algún equipo */
    val esTrono: Boolean
}