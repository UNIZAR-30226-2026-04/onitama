package com.example.onitama.ui.activities.profile

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.core.content.ContextCompat.startActivity
import com.example.onitama.R
import com.example.onitama.AutoLogin
import com.example.onitama.AutoLogin.cerrarSesion
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.ui.activities.Buscar_PartidaActivity
import com.example.onitama.ui.activities.Ini_Activity
import com.example.onitama.ui.activities.cartas.Cartas_activity
import com.example.onitama.ui.activities.partida.PartidaActivity

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Un contenedor base opcional (útil para temas y colores de fondo por defecto)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ProfileScreen(
                )
            }
        }
    }
}

@Composable
fun ProfileScreen(
) {
    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
    val context = LocalContext.current
    val datosUsuario by AutoLogin.sesion.collectAsState()
    if(datosUsuario == null) return
    val imageResId = context.resources.getIdentifier(
        datosUsuario?.avatar_id,
        "drawable",
        context.packageName
    )



    val idSeguro = if (imageResId != 0) imageResId else R.drawable.onitama_text

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // ==========================================
        // 1. FONDOS SUPERPUESTOS
        // ==========================================
        Image(
            painter = painterResource(id = R.drawable.fondomainmenu),
            contentDescription = "Fondo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent, // Color inicial (arriba)
                            Color.White      // Color final (abajo)
                        ),
                        startY = 0f,           // Empieza arriba
                        endY = 2000f           // Ajusta este valor según lo largo que quieras el degradado
                    )
                )
        )

        if(datosUsuario != null){
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 130.dp, bottom = 100.dp), // Deja espacio para no pisar la cabecera ni la barra inferior
                horizontalAlignment = Alignment.CenterHorizontally // Centra todo horizontalmente
            ) {
                item(){
                    Image(
                        painterResource(idSeguro),
                        contentDescription = "Imagen de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(180.dp)
                            .clip(CircleShape)
                    )

                    Text(
                        text = datosUsuario!!.nombre,
                        fontFamily = quattrocentoBold,
                        fontSize = 50.sp,
                        color = Color.Black
                    )
                    Text(
                        text = datosUsuario!!.correo,
                        fontFamily = quattrocentoBold,
                        fontSize = 30.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .height(80.dp)
                                .width(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.LightGray)
                                .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
                        ){
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Image(
                                    painterResource(id = R.drawable.katanas),
                                    contentDescription = "Katanas",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(start = 10.dp)
                                )
                                Text(
                                    datosUsuario?.puntos.toString(),
                                    color = Color.Black,
                                    fontSize = 25.sp,
                                    fontFamily = quattrocentoBold,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .height(80.dp)
                                .width(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.LightGray)
                                .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
                        ){
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painterResource(id = R.drawable.core),
                                    contentDescription = "Core",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .height(50.dp)
                                        .width(50.dp)
                                        .clip(CircleShape)

                                )
                                Text(
                                    datosUsuario?.cores.toString(),
                                    color = Color.Black,
                                    fontSize = 25.sp,
                                    fontFamily = quattrocentoBold,

                                    )
                            }
                        }

                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .height(80.dp)
                            .width(350.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.LightGray)
                            .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
                    ){
                        Text(
                            "Partidas Jugadas: ${datosUsuario?.partidas_jugadas}",
                            color = Color.Black,
                            fontSize = 25.sp,
                            fontFamily = quattrocentoBold,
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .height(80.dp)
                            .width(350.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.LightGray)
                            .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
                    ){
                        Text(
                            "Partidas Ganadas: ${datosUsuario?.partidas_ganadas}",
                            color = Color.Black,
                            fontSize = 25.sp,
                            fontFamily = quattrocentoBold,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                cerrarSesion(context)
                                val intent = Intent(context, Ini_Activity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                ManejadorGlobal.desconectar()
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .background(Color.LightGray)
                                .padding(top = 10.dp)
                                .height(80.dp)
                                .width(150.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, Color.Black, RoundedCornerShape(20.dp))
                        ){
                            Text(
                                "Cerrar Sesión",
                                color = Color.Black,
                                fontSize = 25.sp,
                                fontFamily = quattrocentoBold,
                            )
                        }
                    }
                }
            }
        }




        // ==========================================
        // 3. CABECERA (Contadores y Perfil)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter) // Se ancla arriba del todo
                .background(colorResource(id = R.color.azulFondo))
                .padding(horizontal = 16.dp)
        ) {

            if(datosUsuario != null){
                Image(
                    painterResource(idSeguro),
                    contentDescription = "Imagen de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterEnd)
                        .clip(CircleShape)
                )

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
                        Image(painterResource(id = R.drawable.katanas), contentDescription = "Katanas", modifier = Modifier.size(30.dp))
                        Text(datosUsuario?.puntos.toString(), color = Color.White, fontSize = 24.sp, fontFamily = quattrocentoBold, modifier = Modifier.padding(start = 4.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(id = R.drawable.core), contentDescription = "Core", modifier = Modifier.height(30.dp))
                        Text(datosUsuario?.cores.toString(), color = Color.White, fontSize = 24.sp, fontFamily = quattrocentoBold, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }

        // ==========================================
        // 4. BARRA INFERIOR DE TAREAS
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter) // Se ancla abajo del todo
        ) {
            // Fondo y botones laterales de la barra
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(63.dp)
                    .align(Alignment.BottomCenter)
                    .background(colorResource(id = R.color.azulBarraTareas)),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(60.dp)
                ){
                    Image(painterResource(R.drawable.tablero),
                        contentDescription = "Skins")
                }
                IconButton(
                    onClick = {
                        val intent = Intent(context, Cartas_activity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                              },

                    modifier = Modifier.size(60.dp)
                ) {
                    Image(painterResource(R.drawable.cards),
                        contentDescription = "Cards")
                }

                Spacer(modifier = Modifier.width(80.dp)) // Hueco para el botón central

                IconButton(
                    onClick = {},
                    modifier = Modifier.size(60.dp)
                ){
                    Image(painterResource(R.drawable.amigos),
                        contentDescription = "Amigos")
                }
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(
                        painterResource(R.drawable.carrito),
                        contentDescription = "Tienda")
                }
            }

            // Botón central "A JUGAR" sobresaliendo
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = { /* Iniciar partida rápida */ },
                    modifier = Modifier.size(70.dp)
                ) {
                    Image(painterResource(R.drawable.espadas), contentDescription = "Jugar")
                }
                Text(
                    text = "¡A JUGAR!",
                    fontFamily = quattrocentoBold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.offset(y = (-8).dp)
                )
            }
        }
    }
}