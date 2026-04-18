package com.example.onitama.ui.activities.cartas

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.onitama.AutoLogin
import com.example.onitama.api.obtenerCartas
import com.example.onitama.lib.Carta
import com.example.onitama.lib.Cartas
import com.example.onitama.lib.Movimiento
import com.example.onitama.ui.activities.MenuPrincipalActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class Cartas_activity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nombreUsuario = AutoLogin.obtenerNombre(this) ?: "Jugador"
        val valorCores = AutoLogin.obtenerCores(this)
        val valorKatanas = AutoLogin.obtenerKatanas(this)


        setContent {
            // Un contenedor base opcional (útil para temas y colores de fondo por defecto)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                CartasScreen(
                    nombre = nombreUsuario,
                    cores = valorCores,
                    katanas = valorKatanas
                )
            }
        }
    }
}

@Composable
fun CartasScreen(
    nombre: String = "Jugador",
    cores: Int = 0,
    katanas: Int = 0
) {
    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
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
        val cartas = runBlocking {
            obtenerCartas()
        }
        Column(
            Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
        ){
            Text(
                "MIS CARTAS",
                fontFamily = quattrocentoBold,
                fontSize = 50.sp,
                color = Color.Black,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )

            if (cartas != null) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(600.dp)
                        .padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ){
                    items(cartas.size) { card ->
                        val puntosNeeded = cartas[card].puntos_necesarios
                        Log.d("DEBUG", "Puntos necesarios: $puntosNeeded")
                        if(katanas >= puntosNeeded){
                            CartaCatalogo(
                                carta = Cartas.getCarta(cartas[card].nombre),
                                onClick = {}
                            )
                        }
                        else{
                            Box(
                                modifier = Modifier
                                    .padding(start = 15.dp)
                                    .height(200.dp)
                                    .width(340.dp)
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                                    // 3. 🎨 Feedback visual: Si está seleccionada, se pone azul
                                    .background( Color.LightGray)
                            ){
                                Column(
                                    Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ){
                                    Image(
                                        painterResource(id = R.drawable.bloqueado),
                                        contentDescription = "Imagen de candado",
                                        alignment = Alignment.Center,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .height(90.dp)
                                            .width(90.dp)
                                    )
                                    Row(

                                    ){
                                        Image(painterResource(id = R.drawable.katanas), contentDescription = "Katanas", modifier = Modifier.size(40.dp))
                                        Text(
                                            text = puntosNeeded.toString(),
                                            fontFamily = quattrocentoBold,
                                            fontSize = 30.sp,
                                        )
                                    }
                                }
                            }
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
                        val intent = Intent(context, MenuPrincipalActivity::class.java)
                        context.startActivity(intent)},
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(painterResource(R.drawable.espadas),
                        contentDescription = "Jugar")
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

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {  },
                    modifier = Modifier.size(70.dp)
                ) {
                    Image(painterResource(R.drawable.cards), contentDescription = "Cartas")
                }
                Text(
                    text = "MIS CARTAS",
                    fontFamily = quattrocentoBold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.offset(y = (-8).dp)
                )
            }
        }

    }
}

@Composable
fun CartaCatalogo(carta: Carta, onClick: () -> Unit) {

    val ancho = 340.dp
    val alto = 200.dp
    val context = LocalContext.current

    // 1. Usamos tu función, pero por si acaso tiene espacios, le ponemos replace
    val nombreSeguro = Cartas.imagenCarta(carta).replace(" ", "_")

    val imageResId = context.resources.getIdentifier(
        nombreSeguro,
        "drawable",
        context.packageName
    )

    //🛡️ PROTECCIÓN ANTI-CRASH: Si la imagen no existe (0), ponemos el logo por defecto
    val idSeguro = if (imageResId != 0) imageResId else R.drawable.onitama_text

    Box(
        modifier = Modifier
            .padding(start = 15.dp)
            .height(alto)
            .width(ancho)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            // 3. 🎨 Feedback visual: Si está seleccionada, se pone azul
            .background( Color.LightGray)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically // Centra el minigrid y la imagen
        ) {
            Column{
                Image(
                    painter = painterResource(id = idSeguro), // USAMOS LA VARIABLE SEGURA
                    contentDescription = carta.nombre,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(10.dp)
                        .height(130.dp)
                        .width(130.dp)
                )
                Text(
                    carta.nombre,
                    fontFamily = FontFamily(Font(R.font.quattrocento_bold)),
                    fontSize = 30.sp,
                    modifier = Modifier
                        .offset(y = (-2).dp)
                        .padding(start = 20.dp)
                )
            }

            MinigridCatalogo(carta.movimientos)
        }
    }
}

@Composable
fun MinigridCatalogo(movimientos: List<Movimiento>){
    val tamanoGrid = 7
    val centro = tamanoGrid / 2


    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        for (f in 0 until tamanoGrid) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (c in 0 until tamanoGrid) {
                    // Calculamos el desplazamiento relativo de esta celda respecto al centro
                    // En Onitama: df es filas (y), dc es columnas (x)
                    val dfRelativo = centro - f
                    val dcRelativo = c - centro

                    // Verificamos si este punto coincide con algún movimiento de la carta
                    val esMovimiento = movimientos.any { it.df == dfRelativo && it.dc == dcRelativo }
                    val esCentro = f == centro && c == centro

                    Box(
                        modifier = Modifier
                            .size(20.dp) // Tamaño de cada puntito del grid
                            .clip(RoundedCornerShape(16))
                            .border(1.dp, Color.Black)
                            .background(
                                when {
                                    esCentro -> Color.Black
                                    esMovimiento -> Color(0xFF2196F3) // Azul para movimientos
                                    else -> Color.White.copy(alpha = 0.3f) // Fondo tenue
                                }
                        )
                    )
                }
            }
        }
    }
}