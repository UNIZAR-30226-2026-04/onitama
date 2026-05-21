package com.example.onitama.ui.perfil

import android.app.Activity
import android.content.Intent
import android.util.Log
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onitama.AutoLogin
import com.example.onitama.AutoLogin.cerrarSesion
import com.example.onitama.R
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.ui.inicial.Ini_Activity
import com.example.onitama.ui.activities.MenuPrincipalActivity
import com.example.onitama.ui.notificaciones.Notificaciones_Activity
import com.example.onitama.ui.activities.cartas.Cartas_activity
import com.example.onitama.ui.amigos.Amigos_Activity
import com.example.onitama.ui.components.BotonPrincipal
import com.example.onitama.ui.components.CampoContrasenya

/**
 * Pantalla que muestra datos del usuario.
 *
 * Esta función es un Composable que representa la pantalla que
 * muestra el perfil del usuario.
 *
 * @param viewModel View Model que gestiona el estado y la lógica.
 */
@Composable
fun PantallaPerfil(
    viewModel: ViewModelEditar
) {

    val datosUsuario by AutoLogin.sesion.collectAsState()
    if(datosUsuario == null) return
    val context = LocalContext.current
    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
    var chooseAvatar by remember { mutableStateOf(false) }
    var changingPass by remember { mutableStateOf(false) }

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
                .align(Alignment.TopCenter)
                .background(colorResource(id = R.color.azulFondo))
                .padding(horizontal = 16.dp)
        ) {
            if (datosUsuario != null) {
                Log.d("DEBUG", "Imagen: ${datosUsuario?.avatar_id}")
                val imageResId = context.resources.getIdentifier(
                    datosUsuario?.avatar_id,
                    "drawable",
                    context.packageName
                )

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
        // 2. DATOS DEL USUARIO
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 120.dp, bottom = 63.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Imagen de perfil
            val imageResId = context.resources.getIdentifier(
                datosUsuario?.avatar_id,
                "drawable",
                context.packageName
            )

            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = "Imagen de Perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable(onClick = {chooseAvatar = true})
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray)
                        .clickable(onClick = {chooseAvatar = true}),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = datosUsuario?.nombre?.take(1)?.uppercase() ?: "",
                        color = colorResource(id = R.color.azulFondo),
                        fontSize = 64.sp,
                        fontFamily = quattrocentoBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Información del jugador
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoRow(
                    label = "Nombre de usuario:",
                    value = datosUsuario?.nombre ?: "Nombre de usuario",
                    fontFamily = quattrocentoBold
                )
                InfoRow(
                    label = "Correo electrónico:",
                    value = datosUsuario?.correo ?: "Correo electrónico",
                    fontFamily = quattrocentoBold
                )
                InfoRow(
                    label = "Partidas Jugadas:",
                    value = datosUsuario?.partidas_jugadas?.toString() ?: "0",
                    fontFamily = quattrocentoBold
                )
                InfoRow(
                    label = "Partidas Ganadas:",
                    value = datosUsuario?.partidas_ganadas?.toString() ?: "0",
                    fontFamily = quattrocentoBold
                )
            }

            // Dentro de tu Composable de Perfil
            val last3Partidas by viewModel.partidasRecientes.collectAsState()

            // Esto para que al cargar la pantalla solicite las 3 últimas públicas al servidor
            LaunchedEffect(Unit) {
                viewModel.getPartidas(datosUsuario?.nombre ?: "")
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (last3Partidas.isEmpty()) {
                    Text("No hay partidas recientes", color = Color.Gray)
                }

                for (partida in last3Partidas) {
                    // Determinamos el color de fondo según el resultado
                    val colorFondo = when {
                        partida.ganador == datosUsuario?.nombre -> Color(0xFFC8E6C9) // Verde clarito
                        partida.ganador == "Empate" || partida.ganador == "NO_HAY" -> Color(0xFFFFF9C4) // Amarillo clarito
                        else -> Color(0xFFFFCDD2) // Rojo clarito
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp)) // Un poco de diseño
                            .background(colorFondo)
                            .padding(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween, // Separa los textos
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "vs ${partida.oponente}",
                                    fontFamily = quattrocentoBold, // Si la tienes definida
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "Estado: ${partida.estado}",
                                    fontSize = 12.sp,
                                    color = Color.DarkGray
                                )
                            }

                            Text(
                                text = if(partida.ganador == datosUsuario?.nombre) "¡VICTORIA!"
                                else if (partida.ganador == "Empate") "EMPATE"
                                else if (partida.ganador == "NO_HAY") "NO HUBO GANADOR"
                                else "DERROTA",
                                fontFamily = quattrocentoBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón 'Cambiar Contraseña'
                Button(
                    onClick = { changingPass = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.azulBarraTareas),
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.editar),
                        contentDescription = "Cambiar Contraseña",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cambiar Contraseña",
                        fontFamily = quattrocentoBold,
                        fontSize = 12.sp
                    )
                }

                // Botón 'Notificaciones'
                Button(
                    onClick = {
                        val intent = Intent(context, Notificaciones_Activity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(id = R.color.azulBarraTareas),
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.notificacion),
                        contentDescription = "Notificaciones",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NOTIFICACIONES",
                        fontFamily = quattrocentoBold,
                        fontSize = 12.sp
                    )
                }

                // Botón 'Cerrar Sesión'
                Button(
                    onClick = {
                        cerrarSesion(context)
                        val intent = Intent(context, Ini_Activity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        ManejadorGlobal.desconectar()
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "CERRAR SESIÓN",
                        fontFamily = quattrocentoBold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        if(chooseAvatar){
            Dialog(onDismissRequest = {chooseAvatar = false}){
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .size(width = 300.dp, height = 600.dp)
                        .clip(RoundedCornerShape(16.dp)) // Cambiado para que no corte tu lista
                        .background(Color.LightGray)
                ) {
                    Column (
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        Text(
                            "Elige Tu avatar",
                            fontSize = 25.sp,
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(12) { index ->
                                val i = index + 1

                                val nombre = "avatar_${i.toString().padStart(2, '0')}"

                                val imageResId = context.resources.getIdentifier(
                                    nombre, "drawable", context.packageName
                                )
                                val idSeguro = if (imageResId != 0) imageResId else R.drawable.onitama_text

                                // Comparamos el nombre de esta imagen con el que tiene el ViewModel
                                val isSelected = (viewModel.avatarState.collectAsState().value == nombre)

                                Image(
                                    painter = painterResource(id = idSeguro),
                                    contentDescription = "Avatar $i",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(80.dp) // Tamaño fijo para que queden uniformes
                                        // Ajusta el tamaño horizontal
                                        .clip(CircleShape) // Los hacemos redondos
                                        // 1. Borde condicional: Solo se dibuja si está seleccionado
                                        .border(width = if (isSelected) 4.dp else 0.dp, color = if (isSelected) colorResource(R.color.azulFondo) else Color.Transparent, shape = CircleShape)
                                        // 2. Efecto de opacidad: Los no seleccionados se ven un poco más apagados
                                        .alpha(if (isSelected) 1f else 0.5f)
                                        // 3. Evento click
                                        .clickable { viewModel.onAvatarChange(nombre) }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                viewModel.cambiarPerfil(context, datosUsuario?.nombre ?: "", viewModel.avatarState.value)
                            },
                        ){
                            Text("Cambiar Avatar")
                        }
                    }
                }
            }

        }

        if(changingPass){
            Dialog(onDismissRequest = {changingPass = false}){
                val context = LocalContext.current
                Box(
                    modifier = Modifier
                        .size(width = 300.dp, height = 600.dp)
                        .clip(RoundedCornerShape(16.dp)) // Cambiado para que no corte tu lista
                        .background(Color.LightGray)
                ) {
                    Column (
                        Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ){
                        CampoContrasenya(
                            entrada = viewModel.oldPassState.collectAsState().value,
                            cambio = { viewModel.onOldPassChange(it) },
                            etiqueta = "Introduce la contraseña actual"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CampoContrasenya(
                            entrada = viewModel.newPass1State.collectAsState().value,
                            cambio = { viewModel.onPass1Change(it) },
                            etiqueta = "Introduce la nueva Contraseña"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        CampoContrasenya(
                            entrada = viewModel.newPass2State.collectAsState().value,
                            cambio = { viewModel.onPass2Change(it) },
                            etiqueta = "Repite la nueva contraseña"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val coincide = ((viewModel.newPass1State.collectAsState().value == viewModel.newPass2State.collectAsState().value) && (viewModel.newPass1State.collectAsState().value != ""))

                        BotonPrincipal(
                            texto = "Cambiar Contraseña",
                            onClick = {
                                val conseguido = viewModel.cambiarPass(context, datosUsuario?.nombre ?: "")
                                if(conseguido){
                                    Log.d("CambioPwd", "La contraseña se ha cambiado exitosamente")
                                    changingPass = false
                                }
                                else{
                                    Log.e("Error de cambio", "No se pudo cambiar o la Contraseña es incorrecta")
                                }
                            },
                            activado = coincide
                        )


                    }
                }
            }
        }
    }
}

/**
 * Fila que muestra una etiqueta y un valor de información del usuario.
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    fontFamily: FontFamily
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontFamily = fontFamily,
            fontSize = 18.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontFamily = fontFamily,
            fontSize = 18.sp,
            color = Color.Black
        )
    }
}