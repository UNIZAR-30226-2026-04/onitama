package com.example.onitama.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.onitama.R
import com.example.onitama.AutoLogin
import com.example.onitama.ui.activities.partida.PartidaActivity

class MenuPrincipalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Un contenedor base opcional (útil para temas y colores de fondo por defecto)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainMenuScreen(
                )
            }
        }
    }
}

@Composable
fun MainMenuScreen(
) {
    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
    var menuPrivadoDesplegado by remember { mutableStateOf(false) }
    var menuEntrenamientoDesplegado by remember  {mutableStateOf(false) }
    val alphaOtrosBotones by animateFloatAsState(targetValue = if (menuPrivadoDesplegado || menuEntrenamientoDesplegado) 0.3f else 1f)
    val context = LocalContext.current
    val datosUsuario by AutoLogin.sesion.collectAsState()

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

        // ==========================================
        // 2. Parte que se desplegara al hacer click en el botón de partida privada
        // ==========================================
        if (menuPrivadoDesplegado) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable {
                        menuPrivadoDesplegado = false
                    } // Si tocas fuera, se cierra
            )
        }

        // ==========================================
        // 2. Parte que se desplegara al hacer click en el botón de partida de entrenamiento
        // ==========================================
        if (menuEntrenamientoDesplegado) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable {
                        menuEntrenamientoDesplegado = false
                    } // Si tocas fuera, se cierra
            )
        }

        // ==========================================
        // 2. SECCIÓN CENTRAL (Botones de Partida)
        // ==========================================
        // Usamos un Column centrado para apilar las opciones de juego
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 130.dp, bottom = 100.dp), // Deja espacio para no pisar la cabecera ni la barra inferior
            horizontalAlignment = Alignment.CenterHorizontally // Centra todo horizontalmente
        ) {

            // --- Partida Online ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 20.dp).alpha(alphaOtrosBotones)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.publicmatch),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(150.dp).padding(end = 16.dp)
                )
                Button(
                    onClick = {
                        val intent = Intent(context, Buscar_PartidaActivity::class.java)
                        context.startActivity(intent)
                    },
                    enabled = !menuPrivadoDesplegado && !menuEntrenamientoDesplegado,
                    modifier = Modifier.size(width = 220.dp, height = 100.dp),
                    shape = RoundedCornerShape(16.dp), // Reemplaza @drawable/boton_esquinas_redondas
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("PARTIDA ONLINE", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                }
            }

            // --- Partida Entrenamiento ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {   Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp).alpha(alphaOtrosBotones)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ironbot),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(150.dp).padding(end = 16.dp)
                    )
                    Button(
                        onClick = { menuEntrenamientoDesplegado = !menuEntrenamientoDesplegado },
                        enabled = !menuPrivadoDesplegado,
                        modifier = Modifier.size(width = 220.dp, height = 100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (menuEntrenamientoDesplegado) Color.LightGray else Color.White)
                    ) {
                        Text("PARTIDA ENTRENAMIENTO", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                    }
                }

                // Opciones de dificultad a elegir
                AnimatedVisibility(visible = menuEntrenamientoDesplegado) {
                    Column(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .padding(start = 116.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        // Botón dificultad fácil
                        Button(
                            onClick = {
                                val intent = Intent(context, PartidaActivity::class.java).apply {
                                    putExtra("MODO_JUEGO", "BOT")
                                    putExtra("DIFICULTAD", "FACIL")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(width = 200.dp, height = 60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ){
                            Text("NIVEL FÁCIL", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botón dificultad media
                        Button(
                            onClick = {
                                val intent = Intent(context, PartidaActivity::class.java).apply {
                                    putExtra("MODO_JUEGO", "BOT")
                                    putExtra("DIFICULTAD", "MEDIO")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(width = 200.dp, height = 60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ){
                            Text("NIVEL MEDIO", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Botón dificultad difícil
                        Button(
                            onClick = {
                                val intent = Intent(context, PartidaActivity::class.java).apply {
                                    putExtra("MODO_JUEGO", "BOT")
                                    putExtra("DIFICULTAD", "DIFICIL")
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(width = 200.dp, height = 60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ){
                            Text("NIVEL DIFÍCIL", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                        }
                    }
                }
            }

            // --- Partida Privada ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            )
            {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.privatematch),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(150.dp).padding(end = 16.dp)
                    )
                    Button(
                        onClick = { menuPrivadoDesplegado = !menuPrivadoDesplegado },
                        enabled = !menuEntrenamientoDesplegado,
                        modifier = Modifier.size(width = 220.dp, height = 100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (menuPrivadoDesplegado) Color.LightGray else Color.White)
                    ) {
                        Text("PARTIDA PRIVADA", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                    }
                }
                AnimatedVisibility(visible = menuPrivadoDesplegado) {
                    Column(
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .padding(start = 116.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        Button(
                            onClick = { /* Acción Continuar Partida Privada */ },
                            modifier = Modifier.size(width = 200.dp, height = 60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ){
                            Text("CONTINUAR PARTIDA", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { /* Acción Empezar Partida Privada */ },
                            modifier = Modifier.size(width = 200.dp, height = 60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ){
                            Text("NUEVA PARTIDA", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
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
            Log.d("DEBUG", "Imagen: ${datosUsuario?.avatar_id}")
            val imageResId = context.resources.getIdentifier(
                datosUsuario?.avatar_id,
                "drawable",
                context.packageName
            )


            //🛡️ PROTECCIÓN ANTI-CRASH: Si la imagen no existe (0), ponemos el logo por defecto
            val idSeguro = if (imageResId != 0) imageResId else R.drawable.onitama_text

            // A) Botón de Perfil
            /*IconButton(
                onClick = { /* Acción perfil */ },
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterEnd)
                    .clip(CircleShape)
                    .background(idSeguro)
            ) {

            }*/
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
                        context.startActivity(intent)},
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