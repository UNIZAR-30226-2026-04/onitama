package com.example.onitama.api

import android.util.Log
import com.example.onitama.Config
import com.example.onitama.DatosPerfil
import com.example.onitama.lib.Carta
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject


data class CartaYPuntos(
    val nombre: String,
    val puntos_necesarios: Int
)

val AuthClient = Auth()

suspend fun obtenerCartas(): List<CartaYPuntos>? {
    if (!AuthClient.usarServidor) {
        return null
    }

    val respuesta = withTimeoutOrNull(8_000L) {
        val requestJson = JSONObject().apply {
            put("tipo", "OBTENER_CARTAS")
        }
        AuthClient.enviarYEsperarRespuesta(requestJson.toString())
    } ?: throw Exception("El servidor no respondió a tiempo.")
    val tipo = respuesta.optString("tipo")
    when(tipo){
        "LISTA_CARTAS" ->{
            var resultado = mutableListOf<CartaYPuntos>()
            val cartasArray = respuesta.optJSONArray("cartas")
            if (cartasArray != null) {
                for (i in 0 until cartasArray.length()) {
                    val cartaJson = cartasArray.optJSONObject(i)
                    if (cartaJson != null) {
                        resultado.add(
                            CartaYPuntos(
                                nombre = cartaJson.optString("nombre"),
                                puntos_necesarios = cartaJson.optInt("puntos_necesarios")
                            )
                        )
                    }
                }
            }
            return resultado
        }
        else -> Log.e("ERROR", "Respuesta inesperada del servidor.")
    }
    return null
}