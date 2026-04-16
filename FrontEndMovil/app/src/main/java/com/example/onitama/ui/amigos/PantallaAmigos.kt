package com.example.onitama.ui.amigos

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onitama.AutoLogin
import com.example.onitama.R
import com.example.onitama.ui.activities.Cartas_activity

/**
 * Pantalla de amigos.
 *
 * Esta función es un Composable que representa la pantalla de
 * amigos. En esta pantalla se muestran los amigos que tiene el
 * usuario. Además, hay un buscador que permite al usuario hacer
 * búsquedas escribiendo el nombre de usuario del jugador que
 * busca.
 */
@Composable
fun PantallaAmigos(viewModel: ViewModelAmigos = viewModel()) {

    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
    val context = LocalContext.current
    val datosUsuario by AutoLogin.sesion.collectAsState()
    
    val query by viewModel.raizBuscada.collectAsState()
    val amigos by viewModel.listaAmigos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ==========================================
        // 1. CABECERA (Contadores y Perfil)
        // ==========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter) // Se ancla arriba del todo
                .background(colorResource(id = R.color.azulFondo))
                .padding(horizontal = 16.dp)
        ) {
            // A) Botón de Perfil
            IconButton(
                onClick = { /* Acción perfil */ },
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 130.dp, bottom = 70.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Buscador
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.busqueda(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Buscar jugadores...") },
                leadingIcon = {
                    Image(
                        painter = painterResource(id = R.drawable.lupa),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                },
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.azulFondo),
                    unfocusedBorderColor = Color.Gray,
                    cursorColor = colorResource(id = R.color.azulFondo)
                ),
                singleLine = true
            )

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorResource(id = R.color.azulFondo))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(amigos) { amigo ->
                        val esAmigo = amigos.any { it.nombre == amigo.nombre }
                        FriendItem(
                            amigo = amigo,
                            fontFamily = quattrocentoBold,
                            esAmigo = esAmigo,
                            onSeguir = { viewModel.seguir(amigo.nombre) },
                            onDejarDeSeguir = { viewModel.dejarDeSeguir(amigo.nombre) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            thickness = 0.5.dp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // ==========================================
        // 3. BARRA INFERIOR DE TAREAS
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
                    modifier = Modifier.size(70.dp)
                ){
                    Image(painterResource(R.drawable.amigos),
                        contentDescription = "Amigos")
                }
                Text(
                    text = "AMIGOS",
                    fontFamily = quattrocentoBold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.offset(y = (-8).dp)
                )
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
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(painterResource(R.drawable.espadas), contentDescription = "Jugar")
                }
            }
        }
    }
}

@Composable
fun FriendItem(
    amigo: com.example.onitama.api.Amigos.Info,
    fontFamily: FontFamily,
    esAmigo: Boolean,
    onSeguir: () -> Unit,
    onDejarDeSeguir: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con inicial
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(colorResource(id = R.color.azulFondo)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = amigo.nombre.take(1).uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = fontFamily
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Nombre y Puntos
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = amigo.nombre,
                fontSize = 18.sp,
                fontFamily = fontFamily,
                color = Color.Black
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.katanas),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "${amigo.puntos} pts",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        // Botón de seguir o dejar de seguir
        if (esAmigo) {
            IconButton(
                onClick = onDejarDeSeguir,
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.no_seguir),
                    contentDescription = "Dejar de Seguir",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            IconButton(
                onClick = onSeguir,
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.seguir),
                    contentDescription = "Seguir",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}