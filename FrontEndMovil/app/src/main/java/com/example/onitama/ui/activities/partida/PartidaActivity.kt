package com.example.onitama.ui.activities.partida

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.example.onitama.PartidaActiva
import com.example.onitama.R
import com.example.onitama.api.Auth
import com.example.onitama.AutoLogin
import com.example.onitama.DatosPerfil
import com.example.onitama.api.Partida
import com.example.onitama.lib.Carta
import com.example.onitama.lib.Cartas
import com.example.onitama.lib.Dificultad
import com.example.onitama.lib.EquipoID
import com.example.onitama.lib.EstadoJuego
import com.example.onitama.lib.FasePartida
import com.example.onitama.lib.ModoJuego
import com.example.onitama.lib.Movimiento
import com.example.onitama.lib.Posicion
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class PartidaActivity : AppCompatActivity() {
    private val viewModel: PartidaViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val modoString = intent.getStringExtra("MODO_JUEGO") ?: ModoJuego.BOT.name
        val nivelDificultadString = intent.getStringExtra("DIFICULTAD") ?: "FACIL"

        val modoJuego = try {
            ModoJuego.valueOf(modoString)
        } catch (e: Exception) {
            ModoJuego.BOT
        }

        val nivelDificultad = try {
            Dificultad.valueOf(nivelDificultadString)
        } catch (e: Exception) {
            Dificultad.FACIL
        }

        if (savedInstanceState == null) {
            viewModel.iniciarPartida(modoJuego, nivelDificultad)
        }

        setContent {
            // Observamos el estado del ViewModel. Cuando cambie, la UI se repintará sola.
            val estadoJuego = viewModel.estado.collectAsState().value

            val pausa = viewModel.notificacionPausa.collectAsState().value
            val datosUsuario by AutoLogin.sesion.collectAsState()
            val skinActiva = datosUsuario?.skin_activa ?: "Skin0"

            Surface(modifier = Modifier.fillMaxSize()) {
                MatchScreen(
                    estado = estadoJuego, // Pasamos el estado a la UI
                    modo = modoJuego,
                    avisoPausa = pausa,
                    skinActiva = skinActiva
                )
            }
        }
    }


    @Composable
    fun MatchScreen(
        estado: EstadoJuego,
        modo: ModoJuego,
        avisoPausa: Partida.RespuestaSolicitudPausa?,
        skinActiva: String
    ) {
        val datosUsuario by AutoLogin.sesion.collectAsState()
        val authClient: Auth = Auth()
        val context = LocalContext.current
        val activity = context as? Activity
        val partida = Partida()
        var vermazo by remember { mutableStateOf(false) }
        var verAcciones by remember { mutableStateOf(false) }
        val infoCartasAccion by viewModel.mensajeCartaAccion.collectAsState()
        val mostrarPopPausa by remember { derivedStateOf { viewModel.mostrarPopPausa } }
        val mostrarPopCancel by remember { derivedStateOf { viewModel.mostrarPopCancel } }
        val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))


        if (mostrarPopCancel) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { /* Opcional: no permitir cerrar fuera */ },
                confirmButton = {
                    Button(
                        onClick = {

                            activity?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.azulFondo))
                    ) {
                        Text("Aceptar", color = Color.White)
                    }
                },
                title = {
                    Text(
                        "Partida Cancelada",
                        fontFamily = quattrocentoBold,
                        color = colorResource(id = R.color.azulFondo)
                    )
                },
                text = {
                    Text(
                        "Se ha agotado el tiempo límite, la partida se cancela sin ganador",
                        fontFamily = quattrocentoBold
                    )
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
        }



        if (mostrarPopPausa) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { /* Opcional: no permitir cerrar fuera */ },
                confirmButton = {
                    Button(
                        onClick = {
                            // Al pulsar, cerramos la actividad
                            activity?.finish()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = R.color.azulFondo))
                    ) {
                        Text("Aceptar", color = Color.White)
                    }
                },
                title = {
                    Text(
                        "Partida Pausada",
                        fontFamily = quattrocentoBold,
                        color = colorResource(id = R.color.azulFondo)
                    )
                },
                text = {
                    Text(
                        "La partida se ha guardado correctamente. Podrás reanudarla más tarde desde el menú de amigos.",
                        fontFamily = quattrocentoBold
                    )
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
        }



        LaunchedEffect(estado.modoAccion, estado.cartaAccionYaUsada) {
            if (estado.modoAccion != null) {
                verAcciones = false
            }

            if (estado.cartaAccionYaUsada){
                verAcciones = true
            }
        }



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
                                datosUsuario?.puntos.toString(),
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
                                datosUsuario?.cores.toString(),
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
                        painter = painterResource(id = if (modo == ModoJuego.BOT) R.drawable.ironbot else R.drawable.publicmatch), //pendiente cambiarlo cuando se tengan las públicas por la imagen de perfil del oponente
                        contentDescription = "Imagen del rival",

                        )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = if (modo == ModoJuego.BOT) "Iron" else PartidaActiva.datosPartida?.oponente
                                ?: "Desconocido",
                            fontFamily = quattrocentoBold,
                            fontSize = 30.sp,
                            color = Color.Companion.White
                        )
                        if (modo != ModoJuego.BOT) {
                            Row {
                                Image(
                                    painterResource(id = R.drawable.katanas),
                                    contentDescription = "Katanas",
                                    modifier = Modifier.Companion.size(30.dp)
                                )
                                Text(
                                    PartidaActiva.datosPartida?.oponentePt.toString(),
                                    color = Color.Companion.White,
                                    fontSize = 24.sp,
                                )
                            }
                        }

                    }

                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        if (modo == ModoJuego.PRIVADA) {
                            Button(
                                onClick = {
                                    viewModel.activarPausa()
                                },
                                modifier = Modifier.height(40.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Companion.Gray)
                            ) {
                                Text(
                                    "PAUSAR",
                                    fontFamily = quattrocentoBold,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.botonAbandonar()
                                val datos = runBlocking {
                                    delay(1000)
                                    authClient.obtenerPerfil(datosUsuario!!.nombre)
                                }
                                AutoLogin.actualizar(context, datos)
                                partida.desconectarPartida()
                                finish()
                            },
                            modifier = Modifier.Companion
                                .size(width = 220.dp, height = 55.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Companion.LightGray)
                        ) {
                            Text(
                                "ABANDONAR",
                                fontFamily = quattrocentoBold,
                                fontSize = 12.sp,
                                color = colorResource(R.color.azulFondo)
                            )
                        }
                    }
                }
                LazyRow(
                    verticalAlignment = Alignment.Companion.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    estado.cartasOponente.forEach { carta ->
                        if(estado.equipoCiego == null || estado.equipoCiego != PartidaActiva.datosPartida!!.obtenerEquipoID()) {
                            item{
                                CartaBoton(
                                    carta = carta,
                                    seleccionada = false,
                                    onClick = {
                                        if (estado.modoAccion == "ROBAR" && viewModel.cartaAccionEnUso != null) {
                                            viewModel.ejecucionCartaAccion(
                                                nombreCarta = "Atrapasueños",
                                                cartaAccion = "ROBAR",
                                                cartaARobar = carta.nombre // Pasamos el nombre de la carta rival elegida
                                            )
                                        }
                                    },
                                    isEnemy = true,
                                    skinActiva = skinActiva
                                )
                            }
                        }
                        else{
                            item{
                                Oculto()
                            }
                        }
                    }

                }

                Box(
                    modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho disponible
                    contentAlignment = Alignment.Center // Centra lo que tenga dentro
                ) {
                    GridTablero(
                        estado = estado,
                        onCasillaClick = { pos -> viewModel.tocarCelda(pos) },
                        skinActiva = skinActiva
                    )
                }
                LazyRow(
                    verticalAlignment = Alignment.Companion.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    estado.cartasJugador.forEach { carta ->
                        item{
                            CartaBoton(
                                carta = carta,
                                seleccionada = estado.cartaSeleccionada == carta,
                                onClick = {
                                    cambiarEstadoCarta(carta, estado)
                                },
                                isEnemy = false,
                                skinActiva = skinActiva
                            )
                        }
                    }

                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Companion.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Companion.End)
                ) {


                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = datosUsuario!!.nombre,
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
                                datosUsuario?.puntos.toString(),
                                color = Color.Companion.White,
                                fontSize = 24.sp,
                                fontFamily = quattrocentoBold,
                                modifier = Modifier.Companion.padding(start = 4.dp)
                            )
                        }
                    }
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
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
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
                }


                if (estado.cartasAccionPropia.isNotEmpty() && estado.fasePartida == FasePartida.JUGANDO) {
                    val seleccion = estado.modoAccion != null

                    Column(
                        modifier = Modifier.Companion
                            .fillMaxWidth()
                            .padding(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (seleccion) {
                            Button(
                                onClick = {
                                    viewModel.desSeleccionarCarta()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "CANCELAR PODER",
                                    fontFamily = quattrocentoBold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            estado.cartasAccionPropia.forEach { nombreCarta ->
                                val esMiTurno = estado.turnoActual == viewModel.equipoPropio

                                Box(
                                    modifier = Modifier
                                        .clickable(enabled = esMiTurno) {
                                            viewModel.activarCartaAccion(
                                                nombreCarta
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = nombreCarta.uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = quattrocentoBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentAlignment = Alignment.BottomStart,
            ) {
                Column {
                    Button(
                        onClick = { 
                            vermazo = !vermazo
                            if (vermazo) verAcciones = false
                        },
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                        modifier = Modifier.size(50.dp)
                    ) {
                        Text(
                            text = if (vermazo) "v" else "^", // Un pequeño truco para que la flecha cambie
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )
                    }

                    AnimatedVisibility(
                        visible = vermazo,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 300.dp, height = 500.dp)
                                .clip(RoundedCornerShape(16.dp)) // Cambiado para que no corte tu lista
                                .background(Color.DarkGray)
                        ) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "CARTAS SIGUIENTES",
                                    fontSize = 25.sp,
                                    fontFamily = quattrocentoBold,
                                    textAlign = TextAlign.Center,
                                    color = Color.White

                                )
                                estado.cartasSiguientes.forEach { carta ->
                                    CartaBoton(
                                        carta = carta,
                                        seleccionada = estado.cartaSeleccionada == carta,
                                        onClick = { Unit },
                                        isEnemy = false,
                                        skinActiva = skinActiva
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (estado.fasePartida == FasePartida.JUGANDO && estado.cartasAccionPropia.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(start = 80.dp, bottom = 10.dp)
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Button(
                            onClick = { 
                                verAcciones = !verAcciones 
                                if (verAcciones) vermazo = false
                            },
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                            modifier = Modifier.size(50.dp)
                        ) {
                            Text(
                                text = if (verAcciones) "v" else "^",
                                fontSize = 25.sp,
                                textAlign = TextAlign.Center,
                                color = Color.Black
                            )
                        }

                        AnimatedVisibility(
                            visible = verAcciones,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(
                                        width = 300.dp, 
                                        height = 500.dp
                                    )
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.DarkGray)
                                    .border(
                                        2.dp,
                                        Color.White,
                                        RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "CARTAS DE ACCION",
                                        fontSize = 20.sp,
                                        fontFamily = quattrocentoBold,
                                        textAlign = TextAlign.Center,
                                        color = Color.White
                                    )

                                    estado.cartasAccionPropia.take(2).forEach { nombreCarta ->
                                        SeleccionarCartaAccion(
                                            nombre = nombreCarta,
                                            esSeleccionable = estado.cartaAccionYaUsada,
                                            yaUsada = estado.cartaAccionYaUsada,
                                            onClick = { 
                                                if (!estado.cartaAccionYaUsada) {
                                                    viewModel.activarCartaAccion(nombreCarta)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }                    
                }
            }

            AnimatedVisibility(
                visible = infoCartasAccion != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 130.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(
                        modifier = Modifier
                        
                    ) {
                        Text(
                            text = infoCartasAccion ?: "",
                            color = Color.White,
                            fontFamily = quattrocentoBold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (estado.fasePartida == FasePartida.COLOCAR_TRAMPA) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 5.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val yaPuesta = estado.posicionTrampa != null
                        val texto = when {
                            yaPuesta -> "ESPERANDO A QUE EL RIVAL COLOQUE SU TRAMPA....."
                            else -> "COLOCA TU CASILLA TRAMPA"
                        }
                        
                        
                        val colorFondo = Color(0xFFFF5252)  
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    colorFondo,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    1.dp,
                                    color = Color.Red,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                                
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "!",
                                    fontSize = 20.sp,
                                    color = Color.Black
                                )
                                
                                Spacer(Modifier.width(12.dp))

                                Text(
                                    text = texto,
                                    color = Color.White,
                                    fontFamily = quattrocentoBold,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = estado.mensajeErrorTrampa != null
                        ) {
                            estado.mensajeErrorTrampa?.let { error ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            Color(0xFF2C2C2C)
                                        )
                                        .border(
                                            1.dp,
                                            Color.Gray
                                        )
                                        .padding (
                                            horizontal = 16.dp,
                                            vertical = 6.dp
                                        )
                                ) {
                                    Text(
                                        text = error,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (estado.fasePartida == FasePartida.ELEGIR_CARTA_ACCION) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)), 
                    contentAlignment = Alignment.Center
                ) {
                    val yaElegida = estado.cartaAccionInicialElegida != null
                    
                    Column(
                        modifier = Modifier
                            .then(
                                if (!yaElegida) {
                                    Modifier
                                        .fillMaxWidth(0.85f)
                                        .fillMaxHeight(0.70f)
                                } else {
                                    Modifier
                                        .fillMaxWidth(0.85f)
                                        .fillMaxHeight(0.45f)
                                }
                            )
                            .background(
                                colorResource(id = R.color.azulFondo),  
                                shape = RoundedCornerShape(16.dp)
                            )
                            .border(
                                3.dp, 
                                Color.White, 
                                RoundedCornerShape(16.dp)
                            )
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "SELECCIONA TU CARTA DE ACCION",
                                color = Color.White,
                                fontFamily = quattrocentoBold,
                                fontSize = 30.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(Modifier.height(10.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color.Gray)
                            )
                            
                            Spacer(Modifier.height(20.dp))
                            
                            if (!yaElegida) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    estado.cartasAccionPropia.take(2).forEach { nombre ->
                                        SeleccionarCartaAccion(
                                            nombre = nombre,
                                            esSeleccionable = true,
                                            yaUsada = false,
                                            onClick = { viewModel.elegirCartaAccionInicial(nombre) },
                                            
                                        )
                                    } 
                                }
                            } 
                            else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "esperando el rival...",
                                        color = Color.White,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            avisoPausa?.let { notificacion ->
                AlertDialog(
                    onDismissRequest = {},
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.core),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(30.dp)
                            )

                            Text(
                                text = "SOLICITUD DE PAUSA",
                                fontFamily = quattrocentoBold,
                                fontSize = 20.sp,
                                color = Color.Yellow
                            )
                        }
                    },

                    text = {
                        Text(
                            text = "¿Aceptas la pausa?",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    },

                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.enviarAceptarPausa(
                                    notificacion.idNotificacion,
                                    datosUsuario?.nombre ?: ""
                                )
                            },
                        ) {
                            Text(
                                "ACEPTAR",
                                color = Color.White,
                                fontFamily = quattrocentoBold
                            )
                        }
                    },

                    dismissButton = {
                        Button(
                            onClick = {
                                viewModel.enviarRechazarPausa(
                                    notificacion.idNotificacion,
                                    datosUsuario?.nombre ?: ""
                                )
                            },
                        ) {
                            Text(
                                "RECHAZAR",
                                color = Color.Red,
                                fontFamily = quattrocentoBold
                            )
                        }
                    },
                    shape = RoundedCornerShape(15.dp),
                )
            }
        

            if (estado.ganador != null) {
                val motivo = viewModel.razon
                val equipo = viewModel.equipoPropio
                val winner = estado.ganador
                val victoria = winner == equipo

                AlertDialog(
                    // Evita que el jugador cierre el popup pulsando fuera de él
                    onDismissRequest = { },
                    title = {
                        Text(
                            text = if (victoria) "VICTORIA" else "DERROTA",
                            fontFamily = quattrocentoBold,
                            fontSize = 24.sp
                        )
                    },

                    /*image = {
                        Image(
                            painter = painterResource(id = R.drawable.emote_derrota),
                            contentDescription = "Imagen de resultado",
                            modifier = Modifier.size(100.dp)
                        )
                    },*/

                    text = {
                        Text(
                            text = when (motivo) {
                                "TRONO" -> if (victoria) "Colocaste tu rey en el trono del rival" else "Tu rival llevó su rey hasta tu trono"
                                "REY CAPTURADO" -> if (victoria) "Capturaste el rey de tu rival" else "Tu rival ha capturado tu rey"
                                "ABANDONO" -> if (victoria) "Tu rival abandonó la partida" else "Has abandonado la partida"
                                "SIN MOVIMIENTOS" -> if (victoria) "El rival no tiene movimientos disponibles" else "Te has quedado sin mvimientos disponibles"
                                else -> if (victoria) "El Rey del rival ha caído en tu trampa" else "Tu rey ha caido en una trampa. Esta vez tu rival te ha vencido, más suerte a la próxima"

                            },
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (modo == ModoJuego.PUBLICA) {
                                    val datos = runBlocking {
                                        authClient.obtenerPerfil(datosUsuario!!.nombre)
                                    }
                                    AutoLogin.actualizar(context, datos)
                                }
                                finish()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.azulFondo))
                        ) {
                            Text("Volver al Menú", color = Color.White)
                        }
                    }
                )
            }
        }
    }
       


    fun cambiarEstadoCarta(carta: Carta, estado: EstadoJuego) {
        if (estado.modoAccion != null) {
            return
        }

        if (estado.cartaSeleccionada == carta) {
            viewModel.desSeleccionarCarta()
        } else {
            viewModel.seleccionarCarta(carta)
        }
    }

    @Composable
    fun CartaBoton(carta: Carta, seleccionada: Boolean, onClick: () -> Unit, isEnemy: Boolean, skinActiva: String) {

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
                .clickable { onClick() }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically // Centra el minigrid y la imagen
            ) {
                Column {
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
                Minigrid(carta.movimientos, isEnemy, skinActiva)
            }
        }
    }


    @Composable
    fun Oculto() {

        val ancho = 170.dp
        val alto = 100.dp
        val context = LocalContext.current

        Box(
            modifier = Modifier
                .padding(start = 15.dp)
                .height(alto)
                .width(ancho)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .background(Color.DarkGray)
        ) {
            Image(
                painterResource(id = R.drawable.hidden),
                contentDescription = "bloqueado",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align (Alignment.Center)
                    .height(65.dp)
                    .width(65.dp)
            )
        }
    }

    @Composable
    fun SeleccionarCartaAccion(
        nombre: String,
        esSeleccionable: Boolean,
        yaUsada: Boolean,
        onClick: () -> Unit
    ) {
        val context = LocalContext.current
        
        var nombreSeguro = nombre.replace(" ", "_").lowercase()
        if(nombre == "Atrapasueños"){
            nombreSeguro = "atrapasuenos" //caso especial: contiene una ñ
        }
        val imageResId = context.resources.getIdentifier(
            nombreSeguro,
            "drawable",
            context.packageName
        )
        val idSeguro = if (imageResId != 0) imageResId else R.drawable.onitama_text

        val descripcion = when (nombre) {
            "Pensatorium" -> "Invierte en espejo los movimientos de todas las cartas del tablero. Dura hasta que el rival realice un movimiento."
            "Atrapasueños" -> "Elige una carta de movimiento del oponente y añádela a tu mano."
            "Requiem" -> "Selecciona un peón tuyo y un peón rival; ambos mueren."
            "Santo Grial" -> "Añade un peón extra a una casilla vacía de tu mitad del campo."
            "La Dama del Mar" -> "Solo se pueden hacer movimientos para adelante."
            "Finisterra" -> "Solo se pueden hacer movimientos para atrás."
            "Brujeria" -> "Tu rival no verá qué cartas tienes."
            "Illusia" -> "Mueve a tu Rey a una casilla vacía."
            else -> "Carta de acción"
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    3.dp,
                    if (yaUsada) Color.Red else Color.White,
                    RoundedCornerShape(16.dp)
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = idSeguro),
                contentDescription = nombre,
                modifier = Modifier
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            ) 

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (yaUsada) 0.8f else 0.4f))
            ) 

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = nombre.uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))
                    
                Text(
                    text = descripcion,
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }

            if (yaUsada) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Color.Red.copy(alpha = 0.8f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            3.dp,
                            Color(0xFF8B0000),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "YA USADA",
                        color = Color.White,
                        fontFamily = FontFamily(Font(R.font.quattrocento_bold)),
                        fontSize = 20.sp
                    )
                }
            }
        }
    }


    @Composable
    fun Minigrid(movimientos: List<Movimiento>, isEnemy: Boolean, skinActiva: String) {
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
                        val dfRelativo = if (isEnemy) (centro - (6 - f)) else centro - f
                        val dcRelativo = if (isEnemy) (6 - c - centro) else c - centro

                        // Verificamos si este punto coincide con algún movimiento de la carta
                        val esMovimiento =
                            movimientos.any { it.df == dfRelativo && it.dc == dcRelativo }
                        val esCentro = f == centro && c == centro

                        Box(
                            modifier = Modifier
                                .size(10.dp) // Tamaño de cada puntito del grid
                                .clip(RoundedCornerShape(16))
                                .border(1.dp, Color.Black)
                                .background(
                                    when {
                                        esCentro -> Color.Black
                                        esMovimiento -> {
                                            when (skinActiva.lowercase()) {
                                                "skin1" -> if (isEnemy) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                                "skin2" -> if (isEnemy) Color(0xFFF8FAFC) else Color(0xFF1E3A8A)
                                                "skin5" -> if (isEnemy) Color(0xFFFACC15) else Color(0xFF10B981)
                                                "skin6" -> if (isEnemy) Color(0xFFF97316) else Color(0xFFA855F7)
                                                else -> if (isEnemy) Color.Red else Color(0xFF2196F3)
                                            }
                                        }
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
    fun GridTablero(estado: EstadoJuego, onCasillaClick: (Posicion) -> Unit, skinActiva: String) {
        val context = LocalContext.current
        val tamanoGrid = 7

        // 1. ¿Quién es el jugador que tiene el móvil en la mano?
        // Si juegas local (contra el bot), asumimos que el humano es el Azul.
        val equipoLocal = remember {
            PartidaActiva.datosPartida?.obtenerEquipoID() ?: EquipoID.AZUL
        }

        // 2. ¿Debemos girar la pantalla 180 grados?
        val invertirPantalla = (equipoLocal == EquipoID.ROJO)

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            for (f in 0 until tamanoGrid) {
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (c in 0 until tamanoGrid) {


                        val logicaF = if (invertirPantalla) (6 - f) else f
                        val logicaC = if (invertirPantalla) (6 - c) else c

                        val posLogica = Posicion(logicaF, logicaC)
                        val celda = estado.tablero[logicaF][logicaC]
                        val esTrampaSeleccionada = estado.posicionTrampa == posLogica

                        val boardStyle = when (skinActiva.lowercase()) {
                            "skin1" -> "ajedrez"
                            "skin2" -> "clasico-futbol"
                            else -> "default"
                        }
                        val esBlanca = (f + c) % 2 == 0
                        val colorBase = when (boardStyle) {
                            "ajedrez" -> if (esBlanca) Color(0xFF3A3A3A) else Color(0xFFC8C5C1)
                            "clasico-futbol" -> Color(0xFF2E7D32).copy(alpha = 0.9f)
                            else -> Color.White.copy(alpha = 0.3f)
                        }


                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(5))
                                .background(
                                    when {
                                        estado.fichaSeleccionada == posLogica -> Color.Yellow
                                        estado.movimientosValidos.contains(posLogica) -> Color.Green
                                        celda.esTrono -> if (boardStyle == "default") Color.DarkGray else colorBase
                                        else -> colorBase
                                    }
                                )
                                .border(
                                    width = when {
                                        celda.esTrampaEquipo == -1 -> 2.dp
                                        esTrampaSeleccionada -> 3.dp
                                        else -> 1.dp
                                    },
                                    color = when {
                                        celda.esTrampaEquipo == -1 -> Color.DarkGray
                                        esTrampaSeleccionada -> Color.Red
                                        else -> Color.Black
                                    }
                                )
                                // Mandamos SIEMPRE la posición lógica al ViewModel
                                .clickable { onCasillaClick(posLogica) }
                        ) {
                            // Guardamos la ficha en una variable segura (Adiós a los !!)
                            val ficha = celda.ficha

                            if (ficha != null) {
                                val colorSuffix = skinActiva.lowercase()
                                val equipoColor = if (ficha.equipo == EquipoID.ROJO) "rojo" else "azul"
                                val tipoPieza = if (ficha.esRey) "rey" else "peon"
                                
                                val resId = context.resources.getIdentifier(
                                    "${tipoPieza}${equipoColor}${colorSuffix}",
                                    "drawable",
                                    context.packageName
                                )
                                Log.d("SKINS", "ficha: $ficha, resId: $resId")
                                
                                val finalResId = if (resId != 0) resId else {
                                    Log.d("SKINS", "No existe la imagen de la skin actual")
                                    // Fallback a skin0 si no existe la imagen de la skin actual
                                    context.resources.getIdentifier(
                                        "${tipoPieza}${equipoColor}",
                                        "drawable",
                                        context.packageName
                                    )
                                }

                                Image(
                                    painter = painterResource(id = finalResId),
                                    contentDescription = "Ficha ${ficha.equipo}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(1.dp)
                                )
                            } else if (celda.esTrono) {
                                val equipoTrono = if (logicaF == 0) "rojo" else "azul"
                                val colorSuffix = skinActiva.lowercase()
                                val resId = context.resources.getIdentifier(
                                    "templo${equipoTrono}${colorSuffix}",
                                    "drawable",
                                    context.packageName
                                )
                                if (resId != 0) {
                                    Image(
                                        painter = painterResource(id = resId),
                                        contentDescription = "Trono",
                                        modifier = Modifier.fillMaxSize(),
                                        alpha = 0.4f
                                    )
                                }
                            } else if (celda.esTrampaEquipo == -1) {
                                Image(
                                    painter = painterResource(
                                        id = R.drawable.lapida
                                    ),
                                    contentDescription = "Esta casilla ha quedado inactiva",
                                    modifier = Modifier.Companion
                                        .fillMaxSize()
                                )
                            }
                            else if (celda.esTrampaEquipo == equipoLocal.id || esTrampaSeleccionada) {
                                Image(
                                    painter = painterResource(
                                        id = R.drawable.casilla_trampa
                                    ),
                                    contentDescription = "Trampa",
                                    modifier = Modifier.Companion
                                        .fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

