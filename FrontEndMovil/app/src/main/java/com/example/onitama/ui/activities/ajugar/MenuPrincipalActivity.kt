package com.example.onitama.ui.activities.ajugar

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.onitama.R
import com.example.onitama.AutoLogin
import com.example.onitama.PartidaActiva
import com.example.onitama.ui.activities.cartas.Cartas_activity
import com.example.onitama.ui.activities.partida.PartidaActivity
import com.example.onitama.ui.activities.amigos.Amigos_Activity
import com.example.onitama.api.*
import com.example.onitama.api.ManejadorPartidaAPI
import com.example.onitama.ui.activities.buscarpartida.Buscar_PartidaActivity
import com.example.onitama.ui.activities.perfil.Perfil_Activity
import com.example.onitama.ui.activities.tableros.Tableros_Activity
import com.example.onitama.ui.activities.tienda.Tienda_Activity
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import org.json.JSONObject

class MenuPrincipalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Un contenedor base opcional (útil para temas y colores de fondo por defecto)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainMenuScreen()
            }
        }
    }
}

@Composable
fun MainMenuScreen(
) {
    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))

    val context = LocalContext.current
    val datosUsuario by AutoLogin.sesion.collectAsState()

    val manejadorPartidaAPI = remember { ManejadorPartidaAPI() }
    val amigosAPI = remember { Amigos() }

    var menuPrivadoDesplegado by remember { mutableStateOf(false) }
    var menuEntrenamientoDesplegado by remember  {mutableStateOf(false) }
    var listaAmigosDesplegada by remember { mutableStateOf(false) }
    var listaPartidasPausadaDesplegada by remember { mutableStateOf(false) }

    var esperar by remember { mutableStateOf(false) }
    var oponente by remember { mutableStateOf("") }
    var tiempo by remember { mutableIntStateOf(120) }
    var idNotificacion by remember { mutableIntStateOf(-1) }

    var listaAmigos by remember { mutableStateOf<List<Amigos.Info>>(emptyList()) }
    var listaPartidasPausadas by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var cargaDatos by remember { mutableStateOf(false) }

    var esNuevaPartida by remember { mutableStateOf(true) }
    var amigoSeleccionadoParaReanudar by remember { mutableStateOf<Amigos.Info?>(null) }

    val alphaOtrosBotones by animateFloatAsState(
        targetValue = if (menuPrivadoDesplegado ||
                          menuEntrenamientoDesplegado ||
                          listaAmigosDesplegada ||
                          listaPartidasPausadaDesplegada ||
                          esperar ) {
                            0.3f
                        }
                        else {
                            1f
                        }
    )

    LaunchedEffect(Unit) {
        ManejadorGlobal.mensajesEntrantes.collect { json ->
            val tipo = json.optString("tipo")
            when (tipo) {
                "NOTIFICACION_ENVIADA" -> {
                    idNotificacion = json.optInt("idNotificacion")
                }

                "PARTIDA_PRIVADA_ENCONTRADA" -> {
                    esperar = false

                    val jsonSerializer = Json {
                        ignoreUnknownKeys = true
                        classDiscriminator = "tipo"
                    }
                    val datos = jsonSerializer.decodeFromString<Partida.RespuestaPartidaPrivadaEncontrada>(json.toString())

                    PartidaActiva.datosPartida = datos.toPartidaEncontrada()

                    val intent = Intent(context, PartidaActivity::class.java).apply {
                        putExtra("MODO_JUEGO", "PRIVADA")
                    }
                    context.startActivity(intent)
                }

                "PARTIDAS_PRIVADAS" -> {
                    val array = json.optJSONArray("partidas")
                    val filtrar = mutableListOf<JSONObject>()
                    if (array != null) {
                        for (i in 0 until array.length()) {
                            val indice = array.getJSONObject(i)

                            if (indice.optString("estado") == "PAUSADA") {
                                filtrar.add(indice)
                            }
                        }
                        listaPartidasPausadas = filtrar
                        cargaDatos = false
                    }
                }

                "ERROR_NO_UNIDO", "INVITACION_RECHAZADA", "NOTIFICACION_CANCELADA" -> {
                    esperar = false
                    idNotificacion = -1
                }
            }
        }
    }

    LaunchedEffect(esperar) {
        if (esperar) {
            tiempo = 120

            while (tiempo >= 0 && esperar) {
                delay(1000L)
                tiempo--
            }

            if (tiempo == 0 && esperar) {
                esperar = false
                idNotificacion = -1
            }
        }
    }

    LaunchedEffect(listaAmigosDesplegada) {
        if(listaAmigosDesplegada) {
            cargaDatos = true
            listaAmigos = amigosAPI.obtenerAmigos(datosUsuario?.nombre ?: "")
            cargaDatos = false
        }
    }

    

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
                    .clickable(enabled = !esperar) {
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
                    .clickable(enabled = !esperar)  {
                        menuEntrenamientoDesplegado = false
                    } // Si tocas fuera, se cierra
            )
        }

        if (listaAmigosDesplegada) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = !esperar)  {
                        listaAmigosDesplegada = false
                    } // Si tocas fuera, se cierra
            )
        }

        if (listaPartidasPausadaDesplegada) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = !esperar)  {
                        listaPartidasPausadaDesplegada = false
                    } // Si tocas fuera, se cierra
            )
        }

        if (esperar) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable(enabled = !esperar)  {}
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
                    enabled = !menuPrivadoDesplegado && !menuEntrenamientoDesplegado && !listaAmigosDesplegada && !listaPartidasPausadaDesplegada && !esperar,
                    modifier = Modifier.size(width = 220.dp, height = 100.dp),
                    shape = RoundedCornerShape(16.dp), // Reemplaza @drawable/boton_esquinas_redondas
                    colors = buttonColors(containerColor = Color.White)
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
                        enabled = !menuPrivadoDesplegado && !listaAmigosDesplegada && !listaPartidasPausadaDesplegada && !esperar,
                        modifier = Modifier.size(width = 220.dp, height = 100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = buttonColors(containerColor = if (menuEntrenamientoDesplegado) Color.LightGray else Color.White)
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
                            colors = buttonColors(containerColor = Color.White)
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
                            colors = buttonColors(containerColor = Color.White)
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
                            colors = buttonColors(containerColor = Color.White)
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
                        enabled = !menuEntrenamientoDesplegado && !listaAmigosDesplegada && !listaPartidasPausadaDesplegada && !esperar,
                        modifier = Modifier.size(width = 220.dp, height = 100.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = buttonColors(containerColor = if (menuPrivadoDesplegado) Color.LightGray else Color.White)
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
                            onClick = { /* Acción Continuar Partida Privada */ 
                               esNuevaPartida = false
                               listaAmigosDesplegada = true
                               menuPrivadoDesplegado = false
                            },
                            modifier = Modifier.size(width = 200.dp, height = 60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = buttonColors(containerColor = Color.White)
                        ){
                            Text("CONTINUAR PARTIDA", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { /* Acción Empezar Partida Privada */ 
                                esNuevaPartida = true
                                listaAmigosDesplegada = true
                                menuPrivadoDesplegado = false
                            },
                            modifier = Modifier.size(width = 200.dp, height = 60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = buttonColors(containerColor = Color.White)
                        ){
                            Text("NUEVA PARTIDA", fontFamily = quattrocentoBold, color = colorResource(R.color.azulFondo))
                        }
                    }
                }
            }

        }

        AnimatedVisibility(
            visible = listaAmigosDesplegada,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Evita que toque los bordes de la pantalla
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // CABECERA (Título e icono de cerrar)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (esNuevaPartida) "RETAR A UN AMIGO" else "SELECCIONAR RIVAL",
                        fontFamily = quattrocentoBold,
                        fontSize = 16.sp,
                        color = colorResource(id = R.color.azulFondo)
                    )
                    IconButton(onClick = { listaAmigosDesplegada = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // CONTENIDO VARIABLE (Carga, Vacío o Lista)
                Box(modifier = Modifier.weight(1f, fill = false)) { // Permite que la lista sea scrolleable si es larga
                    if (cargaDatos) {
                        Box(
                            Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("CARGANDO AMIGOS", textAlign = TextAlign.Center, color = Color.Gray)
                        }
                    } else if (listaAmigos.isEmpty()) {
                        Text(
                            "NO TIENES AMIGOS",
                            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(listaAmigos.size) { index ->
                                val amigo = listaAmigos[index]
                                Button(
                                    onClick = {
                                        if (esNuevaPartida) {
                                            oponente = amigo.nombre
                                            esperar = true
                                            listaAmigosDesplegada = false
                                            manejadorPartidaAPI.enviarInvitacion(
                                                remitente = datosUsuario?.nombre ?: "",
                                                destinatario = amigo.nombre
                                            )
                                            val intent = Intent(context, Buscar_PartidaActivity::class.java).apply {
                                                putExtra("MODO_JUEGO", "PRIVADA")
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            amigoSeleccionadoParaReanudar = amigo
                                            manejadorPartidaAPI.obtenerPartidasPausadas(
                                                datosUsuario?.nombre ?: "",
                                                amigo.nombre
                                            )
                                            listaAmigosDesplegada = false
                                            listaPartidasPausadaDesplegada = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = buttonColors(
                                        containerColor = Color.LightGray // Cambiado para que el texto sea legible
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            amigo.nombre,
                                            fontFamily = quattrocentoBold,
                                            color = colorResource(id = R.color.azulFondo)
                                        )
                                        Text(
                                            text = if (esNuevaPartida) "INVITAR" else "REANUDAR",
                                            color = Color.DarkGray,
                                            fontSize = 12.sp,
                                            fontFamily = quattrocentoBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = listaPartidasPausadaDesplegada,
            modifier = Modifier.align(Alignment.Center)
        ) {
            // 1. ENVOLVEMOS TODO EN UN COLUMN para que los elementos se apilen verticalmente
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f) // Un poco menos del ancho total para que no toque los bordes
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // CABECERA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "REANUDAR PARTIDA PRIVADA",
                        fontFamily = quattrocentoBold,
                        fontSize = 16.sp,
                        color = colorResource(id = R.color.azulFondo)
                    )
                    IconButton(
                        onClick = {
                            listaPartidasPausadaDesplegada = false
                            amigoSeleccionadoParaReanudar = null
                        }
                    ) {
                        // Añadimos un icono para que el botón de cerrar sea visible
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val partidasPrivadasAmigo = listaPartidasPausadas.filter {
                    it.optString("oponente") == amigoSeleccionadoParaReanudar?.nombre
                }

                if (cargaDatos) {
                    Box(
                        Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "BUSCANDO PARTIDAS PAUSADAS",
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        // Ajustamos el alto para que no intente ocupar toda la pantalla
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(partidasPrivadasAmigo) { partida ->
                            val idPartida = partida.optInt("partida_id")
                            val fecha = partida.optString("fecha_pausa", "") // Asumiendo que tienes este dato

                            Button(
                                onClick = {
                                    oponente = amigoSeleccionadoParaReanudar?.nombre ?: ""
                                    esperar = true
                                    listaPartidasPausadaDesplegada = false

                                    manejadorPartidaAPI.solicitarReanudar(
                                        remitente = datosUsuario?.nombre ?: "",
                                        destinatario = oponente,
                                        idPartida = idPartida
                                    )

                                    val intent = Intent(context, Buscar_PartidaActivity::class.java).apply {
                                        putExtra("MODO_JUEGO", "PRIVADA")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = buttonColors(containerColor = Color.LightGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                // 2. TEXTO DENTRO DEL BOTÓN (Para que no esté vacío)
                                Text(
                                    text = "Partida ID: $idPartida ${if(fecha.isNotEmpty()) " - $fecha" else ""}",
                                    color = colorResource(id = R.color.azulFondo),
                                    fontFamily = quattrocentoBold
                                )
                            }
                        }
                    }
                }
            }
        }

        /*AnimatedVisibility(
            visible = esperar,
            modifier = Modifier
                .align(Alignment.Center)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "INVITACION ENVIADA. ESPERANDO ...",
                        fontFamily = quattrocentoBold,
                        fontSize = 16.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CircularProgressIndicator(
                        color = colorResource(
                            id = R.color.azulFondo
                        ),
                        modifier = Modifier
                            .size(40.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "ESPERANDO AMIGO...",
                        fontFamily = quattrocentoBold,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    val minutos = tiempo / 60
                    val segundos = tiempo % 60
                    Text(
                        text = String.format("%02d:%02d", minutos, segundos),
                        fontFamily = quattrocentoBold,
                        fontSize = 30.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            manejadorPartidaAPI.cancelarNotificacionEnviada(idNotificacion)
                            esperar = false
                            idNotificacion = -1
                        },
                        colors = buttonColors(
                            containerColor = Color.LightGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "CANCELAR",
                            fontFamily = quattrocentoBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }*/

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
            if(datosUsuario != null) {
                Log.d("DEBUG", "Imagen: ${datosUsuario?.avatar_id}")
                val imageResId = context.resources.getIdentifier(
                    datosUsuario?.avatar_id,
                    "drawable",
                    context.packageName
                )


                //Si la imagen no existe (0), ponemos la inicial del username
                if (imageResId != 0) {
                    Image(
                        painter = painterResource(imageResId),
                        contentDescription = "Imagen de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .clickable(onClick = {
                                val intent = Intent(context, Perfil_Activity::class.java)
                                context.startActivity(intent)
                            })
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.CenterEnd)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable(onClick = {
                                val intent = Intent(context, Perfil_Activity::class.java)
                                context.startActivity(intent)
                            }),
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
                            datosUsuario?.puntos.toString(),
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
                            datosUsuario?.cores.toString(),
                            color = Color.White,
                            fontSize = 24.sp,
                            fontFamily = quattrocentoBold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
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
                    onClick = {
                        val intent = Intent(context, Tableros_Activity::class.java)
                              context.startActivity(intent)
                        (context as? Activity)?.finish()},
                    enabled = !esperar,
                    modifier = Modifier.size(60.dp)
                ){
                    Image(painterResource(R.drawable.tablero),
                        contentDescription = "Skins")
                }
                IconButton(
                    onClick = {
                        val intent = Intent(
                            context, 
                            Cartas_activity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    },
                    enabled = !esperar,
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(painterResource(R.drawable.cards),
                        contentDescription = "Cards")
                }

                Spacer(modifier = Modifier.width(80.dp)) // Hueco para el botón central

                IconButton(
                    onClick = {
                        val intent = Intent(context, Amigos_Activity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    },
                    enabled = !esperar,
                    modifier = Modifier.size(60.dp)
                ){
                    Image(painterResource(R.drawable.amigos),
                        contentDescription = "Amigos")
                }
                IconButton(
                    onClick = {
                        val intent = Intent(
                            context,
                            Tienda_Activity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    },
                     enabled = !esperar,
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