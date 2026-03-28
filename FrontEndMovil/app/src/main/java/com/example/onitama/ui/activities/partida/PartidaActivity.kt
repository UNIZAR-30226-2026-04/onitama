package com.example.onitama.ui.activities.partida

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.lifecycle.ViewModel
import com.example.onitama.R
import com.example.onitama.api.BuscarPartida
import com.example.onitama.autoLogin
import com.example.onitama.lib.Carta
import com.example.onitama.lib.Cartas
import com.example.onitama.lib.EquipoID
import com.example.onitama.lib.EstadoJuego
import com.example.onitama.lib.ModoJuego
import com.example.onitama.lib.Movimiento
import com.example.onitama.lib.Posicion
import com.example.onitama.lib.calcularMovimientosValidos
import com.example.onitama.lib.crearEstadoInicial
import kotlinx.coroutines.delay

class PartidaActivity: AppCompatActivity() {
    private val viewModel: PartidaViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nombreUsuario = autoLogin.obtenerNombre(this) ?: "Jugador"
        val valorCores = autoLogin.obtenerCores(this)
        val valorKatanas = autoLogin.obtenerKatanas(this)

        val modoString = intent.getStringExtra("MODO_JUEGO") ?: ModoJuego.BOT.name
        val modoJuego = ModoJuego.valueOf(modoString)

        setContent {
            // Observamos el estado del ViewModel. Cuando cambie, la UI se repintará sola.
            val estadoJuego = viewModel.estado.collectAsState().value

            Surface(modifier = Modifier.fillMaxSize()) {
                MatchScreen(
                    nombre = nombreUsuario,
                    cores = valorCores,
                    katanas = valorKatanas,
                    estado = estadoJuego // Pasamos el estado a la UI
                )
            }
        }
    }


    @Composable
    fun MatchScreen(
        nombre: String = "Jugador",
        cores: Int = 0,
        katanas: Int = 0,
        estado: EstadoJuego
    ) {

        val context = LocalContext.current
        val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))



        Box(
            modifier = Modifier.Companion
                .fillMaxSize()
                .background(Color.Companion.White)
        ) {
            // ==========================================
            // 1. FONDOS SUPERPUESTOS
            // ==========================================


            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .background(
                        brush = Brush.Companion.verticalGradient(
                            colors = listOf(
                                Color.Companion.Black,
                                Color.Companion.LightGray, // Color inicial (arriba)
                                Color.Companion.Black      // Color final (abajo)
                            ),
                            startY = 0f,           // Empieza arriba
                            endY = 2500f           // Ajusta este valor según lo largo que quieras el degradado
                        )
                    )
            )

            Column(
                horizontalAlignment = Alignment.Companion.Start,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.Companion
                    .fillMaxWidth()
            ) {
                //Misma cabecera que en el menu pero con el boton de perfil deshabilitado
                Box(
                    modifier = Modifier.Companion
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(colorResource(id = R.color.azulFondo))
                        .padding(horizontal = 16.dp)
                ) {
                    // A) Botón de Perfil (A diferencia del de menu principal este debe de estar deshabilitado)
                    IconButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.Companion
                            .size(80.dp)
                            .align(Alignment.Companion.CenterEnd)
                            .clip(CircleShape)
                            .background(Color.Companion.White)
                    ) {

                    }

                    // B) Título del juego
                    Image(
                        painter = painterResource(id = R.drawable.onitama_text),
                        contentDescription = "Titulo",
                        modifier = Modifier.Companion
                            .padding(start = 30.dp, top = 16.dp)
                            .height(60.dp)
                            .align(Alignment.Companion.TopStart)
                    )

                    // C) Contadores (Katanas y Core)
                    Row(
                        modifier = Modifier.Companion
                            .padding(top = 30.dp, bottom = 10.dp)
                            .align(Alignment.Companion.BottomCenter),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                            Image(
                                painterResource(id = R.drawable.katanas),
                                contentDescription = "Katanas",
                                modifier = Modifier.Companion.size(30.dp)
                            )
                            Text(
                                katanas.toString(),
                                color = Color.Companion.White,
                                fontSize = 24.sp,
                                fontFamily = quattrocentoBold,
                                modifier = Modifier.Companion.padding(start = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                            Image(
                                painterResource(id = R.drawable.core),
                                contentDescription = "Core",
                                modifier = Modifier.Companion.height(30.dp)
                            )
                            Text(
                                cores.toString(),
                                color = Color.Companion.White,
                                fontSize = 24.sp,
                                fontFamily = quattrocentoBold,
                                modifier = Modifier.Companion.padding(start = 4.dp)
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.Companion.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        modifier = Modifier.Companion
                            .padding(start = 10.dp)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Companion.White),
                        painter = painterResource(id = R.drawable.ironbot),
                        contentDescription = "Imagen del rival",

                        )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Iron",
                            fontFamily = quattrocentoBold,
                            fontSize = 30.sp,
                            color = Color.Companion.White
                        )
                        /*if (nombreOponente != "Iron") {
                            Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                                Image(
                                    painterResource(id = R.drawable.katanas),
                                    contentDescription = "Katanas",

                                    modifier = Modifier.Companion
                                        .size(30.dp)
                                )
                                Text(
                                    katanas.toString(),
                                    color = Color.Companion.White,
                                    fontSize = 24.sp,
                                    fontFamily = quattrocentoBold,
                                    modifier = Modifier.Companion.padding(start = 4.dp)
                                )
                            }

                        }*/
                    }

                    Button(
                        onClick = {
                            //se cierra y se vuelve a la pantalla anterior (cuando consiga la comunicación api, añado la función cancelar búsqueda)

                            (context as? Activity)?.finish()
                        },
                        modifier = Modifier.Companion
                            .size(width = 220.dp, height = 55.dp)
                            .padding(top = 15.dp, start = 30.dp)
                            .align(Alignment.Companion.Top),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Companion.LightGray)
                    ) {
                        Text(
                            "ABANDONAR",
                            fontFamily = quattrocentoBold,
                            fontSize = 20.sp,
                            color = colorResource(R.color.azulFondo)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Companion.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    estado.cartasOponente.forEach { carta ->
                        CartaBoton(
                            carta = carta,
                            seleccionada = false,
                            onClick = {}
                        )
                    }

                }

                Box(
                    modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho disponible
                    contentAlignment = Alignment.Center // Centra lo que tenga dentro
                ) {
                    GridTablero(
                        estado = estado,
                        onCasillaClick = { pos -> viewModel.tocarCelda(pos) }
                    )
                    if (estado.ganador != null) {
                        AlertDialog(
                            onDismissRequest = { /* No dejamos que lo cierre tocando fuera */ },
                            title = { Text(text = "¡Partida Finalizada!") },
                            text = {
                                val mensaje = if (estado.ganador == EquipoID.ABAZUL) "¡Has ganado!" else "Ha ganado el Bot"
                                Text(text = mensaje)
                            },
                            confirmButton = {
                                Button(onClick = {
                                    // Aquí deberías salir de la activity o reiniciar el ViewModel
                                    (context as? Activity)?.finish()
                                }) {
                                    Text("Salir")
                                }
                            }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Companion.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    estado.cartasJugador.forEach { carta ->
                        CartaBoton(
                            carta = carta,
                            seleccionada = estado.cartaSeleccionada == carta,
                            onClick = {cambiarEstadoCarta(carta, estado)}
                        )
                    }

                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Companion.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Companion.End)
                ) {

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = nombre,
                            fontFamily = quattrocentoBold,
                            fontSize = 30.sp,
                            color = Color.Companion.White
                        )
                        Row(verticalAlignment = Alignment.Companion.CenterVertically) {
                            Image(
                                painterResource(id = R.drawable.katanas),
                                contentDescription = "Katanas",

                                modifier = Modifier.Companion
                                    .size(30.dp)
                            )
                            Text(
                                katanas.toString(),
                                color = Color.Companion.White,
                                fontSize = 24.sp,
                                fontFamily = quattrocentoBold,
                                modifier = Modifier.Companion.padding(start = 4.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = { },
                        enabled = false,
                        modifier = Modifier.Companion
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Companion.White)
                    ) {

                    }
                }
            }
        }
    }

    fun cambiarEstadoCarta(carta: Carta, estado: EstadoJuego){
        if(estado.cartaSeleccionada == carta) {
            viewModel.desSeleccionarCarta()
        }
        else{
            viewModel.seleccionarCarta(carta)
        }
    }
    @Composable
    fun CartaBoton(carta: Carta, seleccionada: Boolean, onClick: () -> Unit) {

        val ancho = if (seleccionada) 192.dp else 170.dp
        val alto = if (seleccionada) 120.dp else 100.dp
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
                .background(if (seleccionada) Color(0xFFBBDEFB) else Color.LightGray)
                // 4. 👆 INTERACTIVIDAD: Si no pones esto, ¡la carta no hace nada al tocarla!
                .clickable { onClick() }
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
                            .padding(5.dp)
                            .height(65.dp)
                            .width(65.dp)
                    )
                    Text(
                        carta.nombre,
                        fontFamily = FontFamily(Font(R.font.quattrocento_bold)),
                        fontSize = 15.sp,
                        modifier = Modifier
                            .offset(y = (-2).dp)
                            .padding(start = 10.dp)
                    )
                }

                Minigrid(carta.movimientos)
            }
        }
    }

    @Composable
    fun Minigrid(movimientos: List<Movimiento>){
        val tamanoGrid = 7
        val centro = tamanoGrid / 2


        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            for (f in 0 until tamanoGrid) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
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
                                .size(10.dp) // Tamaño de cada puntito del grid
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


    @Composable
    fun GridTablero(estado: EstadoJuego, onCasillaClick: (Posicion) -> Unit){
        val tamanoGrid = 7
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            for (f in 0 until tamanoGrid) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (c in 0 until tamanoGrid) {

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(5))
                                .background(
                                    when {
                                        estado.fichaSeleccionada == Posicion(f, c) -> Color.Yellow
                                        estado.movimientosValidos.contains(Posicion(f, c)) -> Color.LightGray
                                        estado.tablero[f][c].esTrono -> Color.DarkGray
                                        else -> Color.White.copy(alpha = 0.3f)
                                    }
                                )
                                .clickable { onCasillaClick(Posicion(f, c)) }
                        ){
                            if (estado.tablero[f][c].ficha != null) {
                                Image(
                                    painter = when{
                                        estado.tablero[f][c].ficha!!.esRey && estado.tablero[f][c].ficha!!.equipo == EquipoID.ARROJO -> painterResource(id = R.drawable.rey_rojo)
                                        estado.tablero[f][c].ficha!!.esRey && estado.tablero[f][c].ficha!!.equipo == EquipoID.ABAZUL -> painterResource(id = R.drawable.rey_azul)
                                        estado.tablero[f][c].ficha!!.equipo == EquipoID.ARROJO -> painterResource(id = R.drawable.peon_rojo)
                                        else -> painterResource(id = R.drawable.peon_azul)
                                    },
                                    contentDescription = "Ficha ${estado.tablero[f][c].ficha!!.equipo}",
                                    modifier = Modifier.fillMaxSize().padding(1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


