package com.example.onitama.ui.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onitama.R
import com.example.onitama.PartidaActiva
import com.example.onitama.api.BuscarPartida
import com.example.onitama.api.jsonPartida
import com.example.onitama.api.Partida
import com.example.onitama.AutoLogin
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.ui.activities.partida.PartidaActivity
import com.example.onitama.ui.perfil.Perfil_Activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

class Buscar_PartidaActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nombreUsuario = AutoLogin.obtenerNombre(this) ?: "Jugador"
        val valorCores = AutoLogin.obtenerCores(this)
        val valorKatanas = AutoLogin.obtenerKatanas(this)

        val tipoPartida = intent.getStringExtra("MODO_JUEGO") ?: "PUBLICA"

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
                    if (!ManejadorGlobal.estaConectado()){
                        ManejadorGlobal.conectarYMantener()
                    }

                    if (tipoPartida == "PUBLICA") {
                        val resultBuscOponente = servPartida.buscarPartida(scope, nombreUsuario, valorKatanas, 60000)
                        funcionCancelar = resultBuscOponente.cancel
                        val respuesta = resultBuscOponente.promise.await()
                        if (respuesta.estado == BuscarPartida.EstadoPartida.ENCONTRADA) {
                            // Si encuentra partida tendría que abrir la pantalla de juego
                            val intentJuego = Intent(this@Buscar_PartidaActivity, PartidaActivity::class.java).apply {
                                putExtra("MODO_JUEGO", "PUBLICA")
                            }
                            startActivity(intentJuego)
                            finish() // Cerramos la pantalla de búsqueda
                        }
                    }
                    else {
                        funcionCancelar = {
                            finish()
                        }

                        ManejadorGlobal.mensajesEntrantes.collectLatest { json ->
                            val tipo = json.optString("tipo")

                            when (tipo) {
                                "PARTIDA_PRIVADA_ENCONTRADA" -> {
                                    try {
                                        val datos = jsonPartida.decodeFromString<Partida.RespuestaPartidaPrivadaEncontrada>(json.toString())
                                        PartidaActiva.datosPartida = datos.toPartidaEncontrada()

                                        val intentJuego = Intent(this@Buscar_PartidaActivity, PartidaActivity::class.java).apply {
                                            putExtra("MODO_JUEGO", "PRIVADA")
                                        }
                                        startActivity(intentJuego)
                                        finish() // Cerramos la pantalla de búsqueda                                    
                                    }
                                    catch (e: Exception) {
                                        Log.e("PARTIDA_PRIVADA", "Error al procesar la partida privada", e)
                                    }
                                }

                                "ERROR_NO_UNIDO", "INVITACION_RECHAZADA", "NOTIFICACION_CANCELADA" -> {
                                    finish()
                                }
                            }
                        }
                    }
                }

                val textoWaitingPublicScreen = if (tipoPartida == "PRIVADA") "Esperando al amigo..." else "Buscando Oponente..."

                WaitingPublicScreen(
                    nombre = nombreUsuario,
                    cores = valorCores,
                    katanas = valorKatanas,
                    tiempo = 120,
                    texto = textoWaitingPublicScreen,
                    funcionCancelacion = funcionCancelar
                )
            }
        }
    }


    @Composable
    fun WaitingPublicScreen(
        nombre: String = "Jugador",
        cores: Int = 0,
        katanas: Int = 0,
        tiempo: Int = 0,
        texto: String = "Buscando Oponente...",
        funcionCancelacion: (() -> Unit)? = null
    ) {

        val context = LocalContext.current
        val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
        var tiempoEnSegundos by remember { mutableIntStateOf(tiempo) }
        val datosUsuario by AutoLogin.sesion.collectAsState()

        LaunchedEffect(Unit) {
            while (tiempoEnSegundos > 0) { // Solo resta si es mayor que 0
                delay(1000L) // Esperamos 1 segundo
                tiempoEnSegundos-- // Restamos un segundo
            }
            funcionCancelacion?.invoke()
        }


        //Variables de tiempo que se usarán para llenar las cajas
        val minutos = tiempoEnSegundos / 60
        val segundos = tiempoEnSegundos % 60

        val minDecena = minutos / 10
        val minUnidad = minutos % 10
        val secDecena = segundos / 10
        val secUnidad = segundos % 10


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

                val imageResId = context.resources.getIdentifier(
                    datosUsuario?.avatar_id,
                    "drawable",
                    context.packageName
                )
                // A) Botón de Perfil (A diferencia del de menu principal este debe de estar deshabilitado)
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(imageResId),
                        contentDescription = "Imagen de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .background(Color.White)
                            ,
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = datosUsuario?.nombre?.take(1)?.uppercase() ?: "",
                            color = colorResource(id = R.color.azulFondo),
                            fontSize = 32.sp,
                            fontFamily = quattrocentoBold
                        )
                    }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ){

            Text(
                text = "COMENZANDO LA PARTIDA",
                fontFamily = quattrocentoBold,
                fontSize = 30.sp,
                color = Color.LightGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .padding(all = 5.dp)


            )

            Text(
                text = texto,
                fontFamily = quattrocentoBold,
                fontSize = 20.sp,
                color = Color.DarkGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .padding(all = 5.dp)
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .width(90.dp)
                        .padding(5.dp)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Text(
                        text = minDecena.toString(),
                        fontFamily = quattrocentoBold,
                        fontSize = 100.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                        modifier = Modifier
                            .padding(all = 5.dp)

                    )
                }

                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .width(90.dp)
                        .padding(5.dp)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Text(
                        text = minUnidad.toString(),
                        fontFamily = quattrocentoBold,
                        fontSize = 100.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                        modifier = Modifier
                            .padding(all = 5.dp)


                    )
                }

                Text(
                    text = ":",
                    fontFamily = quattrocentoBold,
                    fontSize = 100.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                    modifier = Modifier
                        .padding(all = 2.dp)
                )

                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .width(90.dp)
                        .padding(5.dp)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Text(
                        text = secDecena.toString(),
                        fontFamily = quattrocentoBold,
                        fontSize = 100.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                        modifier = Modifier
                            .padding(all = 5.dp)

                    )
                }

                Box(
                    modifier = Modifier
                        .height(120.dp)
                        .width(90.dp)
                        .padding(5.dp)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Text(
                        text = secUnidad.toString(),
                        fontFamily = quattrocentoBold,
                        fontSize = 100.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Right,
                        modifier = Modifier
                            .padding(all = 5.dp)


                    )
                }

            }
            Button(
                onClick = {
                    //se cierra y se vuelve a la pantalla anterior (cuando consiga la comunicación api, añado la función cancelar búsqueda)
                    funcionCancelacion?.invoke()
                    (context as? Activity)?.finish()
                },
                modifier = Modifier
                    .size(width = 220.dp, height = 70.dp)
                    .padding(top = 15.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray )
            ) {
                Text(
                    "CANCELAR",
                    fontFamily = quattrocentoBold,
                    fontSize = 30.sp,
                    color = colorResource(R.color.azulFondo)
                )
            }
            if(tiempoEnSegundos == 0){
                Text(
                    "NO SE HA ENCONTRADO UN OPONENTE",
                    fontFamily = quattrocentoBold,
                    fontSize = 20.sp,
                    color = Color.Red,
                    modifier = Modifier
                        .padding(top = 10.dp)

                )
                Button(
                    onClick = {
                        (context as? Activity)?.recreate()
                    },
                    modifier = Modifier
                        .size(width = 240.dp, height = 70.dp)
                        .padding(top = 15.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray )
                ) {
                    Text(
                        "REINTENTAR",
                        fontFamily = quattrocentoBold,
                        fontSize = 30.sp,
                        color = colorResource(R.color.azulFondo)
                    )
                }

            }
        }


    }
}
