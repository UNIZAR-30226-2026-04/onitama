package com.example.onitama.ui.activities

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onitama.R
import com.example.onitama.api.BuscarPartida
import com.example.onitama.autoLogin
import kotlinx.coroutines.delay

class PartidaActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nombreUsuario = autoLogin.obtenerNombre(this) ?: "Jugador"
        val valorCores = autoLogin.obtenerCores(this)
        val valorKatanas = autoLogin.obtenerKatanas(this)

        setContent {
            //esta sirve para guardar el valor de la función de cancelar
            var funcionCancelar by remember { mutableStateOf<(() -> Unit)?>(null) }
            val servPartida = remember { BuscarPartida() }
            val scope = rememberCoroutineScope()
            // Un contenedor base opcional (útil para temas y colores de fondo por defecto)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {

                LaunchedEffect(Unit) {
                    val resultBuscOponente = servPartida.buscarPartida(scope, nombreUsuario, valorKatanas, 60000)
                    funcionCancelar = resultBuscOponente.cancel
                    val respuesta = resultBuscOponente.promise.await()
                    if (respuesta.estado == BuscarPartida.EstadoPartida.ENCONTRADA) {
                        // Si encuentra partida tendría que abrir la pantalla de juego
                        // val intentJuego = Intent(this@Buscar_PartidaActivity, JuegoActivity::class.java)
                        // startActivity(intentJuego)
                        // finish() // Cerramos la pantalla de búsqueda
                    }
                }


                MatchScreen(
                    nombre = nombreUsuario,
                    cores = valorCores,
                    katanas = valorKatanas,
                    tiempo = 60,
                    funcionCancelacion = funcionCancelar
                )
            }
        }
    }


    @Composable
    fun MatchScreen(
        nombre: String = "Jugador",
        cores: Int = 0,
        katanas: Int = 0,
        tiempo: Int = 0,
        funcionCancelacion: (() -> Unit)? = null
    ) {

        val context = LocalContext.current
        val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
        var tiempoEnSegundos by remember { mutableIntStateOf(tiempo) }


        LaunchedEffect(Unit) {
            while (tiempoEnSegundos > 0) { // Solo resta si es mayor que 0
                delay(1000L) // Esperamos 1 segundo
                tiempoEnSegundos++ // Restamos un segundo
            }
            // Aquí habrá que poner lo que pasa cuando se llega al timeout
        }





        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // ==========================================
            // 1. FONDOS SUPERPUESTOS
            // ==========================================


            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Black,
                                Color.LightGray, // Color inicial (arriba)
                                Color.Black      // Color final (abajo)
                            ),
                            startY = 0f,           // Empieza arriba
                            endY = 2500f           // Ajusta este valor según lo largo que quieras el degradado
                        )
                    )
            )


            //Misma cabecera que en el menu pero con el boton de perfil deshabilitado
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .align(Alignment.TopCenter) // Se ancla arriba del todo
                    .background(colorResource(id = R.color.azulFondo))
                    .padding(horizontal = 16.dp)
            ) {
                // A) Botón de Perfil (A diferencia del de menu principal este debe de estar deshabilitado)
                IconButton(
                    onClick = { /* Acción perfil */ },
                    enabled = false,
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                        .background(Color.White)
                ) {

                }

                // B) Título del juego
                Image(
                    painter = painterResource(id = R.drawable.onitama_text),
                    contentDescription = "Titulo",
                    modifier = Modifier
                        .padding(start = 30.dp, top = 16.dp)
                        .height(60.dp)
                        .align(Alignment.TopStart)


                )

                // C) Contadores (Katanas y Core)
                Row(
                    modifier = Modifier
                        .padding(top = 30.dp, bottom = 10.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(id = R.drawable.katanas),
                            contentDescription = "Katanas",
                            modifier = Modifier.size(30.dp)
                        )
                        Text(
                            katanas.toString(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontFamily = quattrocentoBold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(id = R.drawable.core),
                            contentDescription = "Core",
                            modifier = Modifier.height(30.dp)
                        )
                        Text(
                            cores.toString(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontFamily = quattrocentoBold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }



    }
}
