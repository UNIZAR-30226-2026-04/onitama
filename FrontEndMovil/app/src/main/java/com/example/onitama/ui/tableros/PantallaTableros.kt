package com.example.onitama.ui.tableros

import android.app.Activity
import android.content.Intent
import android.util.Log
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onitama.AutoLogin
import com.example.onitama.R
import com.example.onitama.ui.activities.MenuPrincipalActivity
import com.example.onitama.ui.activities.cartas.Cartas_activity
import com.example.onitama.ui.amigos.Amigos_Activity
import com.example.onitama.ui.perfil.Perfil_Activity
import com.example.onitama.ui.tienda.Tienda_Activity
import com.example.onitama.api.Skin as SkinAPI

/**
 * Pantalla que muestra datos del usuario.
 *
 * Esta función es un Composable que representa la pantalla que
 * muestra los tableros que tiene el usuario.
 *
 * @param viewModel View Model que gestiona el estado y la lógica.
 */
@Composable
fun PantallaTableros(
    viewModel: ViewModelTableros
) {

    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
    val context = LocalContext.current
    val datosUsuario by AutoLogin.sesion.collectAsState()
    val skins by viewModel.skins.collectAsState()
    val skinActivaId by viewModel.skinActivaId.collectAsState()

    LaunchedEffect(datosUsuario?.nombre) {
        datosUsuario?.nombre?.let {
            viewModel.obtenerSkins(it)
        }
    }

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
        // 2. CUERPO (Lista de Tableros Propios)
        // ==========================================
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 130.dp, bottom = 80.dp)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(skins) { skin ->
                SkinCard(
                    skin = skin,
                    isActiva = skin.skin_id == skinActivaId,
                    onActivateClick = {
                        datosUsuario?.nombre?.let { usuario ->
                            viewModel.activarSkin(usuario, skin.skin_id)
                        }
                    }
                )
            }
        }

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
                Spacer(modifier = Modifier.width(80.dp))
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

                IconButton(
                    onClick = {
                        val intent = Intent(context, MenuPrincipalActivity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier.size(60.dp)
                ){
                    Image(painterResource(R.drawable.espadas),
                        contentDescription = "Jugar")
                }
                IconButton(
                    onClick = {
                        val intent = Intent(
                            context,
                            Amigos_Activity::class.java)
                        context.startActivity(intent)
                        (context as? Activity)?.finish()
                    },
                    modifier = Modifier.size(60.dp)
                ) {
                    Image(
                        painterResource(R.drawable.amigos),
                        contentDescription = "AMIGOS")
                }
                IconButton(
                    onClick = {
                            val intent = Intent(context, Tienda_Activity::class.java)
                            context.startActivity(intent)
                            (context as? Activity)?.finish()},
                    modifier = Modifier.size(60.dp)
                ){
                    Image(painterResource(R.drawable.carrito),
                        contentDescription = "Tienda")
                }

            }

            // Botón central "Carrito" sobresaliendo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp, start = 15.dp),
                horizontalAlignment = Alignment.Start
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(70.dp)
                ) {
                    Image(painterResource(R.drawable.tablero), contentDescription = "Amigos")
                }
                Text(
                    text = "MIS SKINS",
                    fontFamily = quattrocentoBold,
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.offset(y = (-5).dp, x = (-5).dp)
                )
            }
        }
    }
}

@Composable
fun SkinCard(
    skin: SkinAPI.Skin,
    isActiva: Boolean,
    onActivateClick: () -> Unit
) {
    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
    val skinName = getSkinName(skin.skin_id)

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Fila superior: Nombre y Botón
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = skinName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = quattrocentoBold,
                        color = Color.Black
                    )
                }

                if (isActiva) {
                    Button(
                        onClick = { },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color(0xFFE8F5E9),
                            disabledContentColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = false
                    ) {
                        Text(text = "Activa")
                    }
                } else {
                    Button(
                        onClick = onActivateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE67E22)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Usar", color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila inferior: Previsualizaciones de equipos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TeamPreview(
                    teamName = "EQUIPO ROJO",
                    skinId = skin.skin_id,
                    isRed = true,
                    modifier = Modifier.weight(1f)
                )
                TeamPreview(
                    teamName = "EQUIPO AZUL",
                    skinId = skin.skin_id,
                    isRed = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun TeamPreview(
    teamName: String,
    skinId: String,
    isRed: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Obtener los IDs de recursos dinámicamente
    val suffix = skinId.lowercase()
    val colorPart = if (isRed) "rojo" else "azul"

    val peonResId = context.resources.getIdentifier("peon${colorPart}$suffix", "drawable", context.packageName)
    val reyResId = context.resources.getIdentifier("rey${colorPart}$suffix", "drawable", context.packageName)
    val temploResId = context.resources.getIdentifier("templo${colorPart}$suffix", "drawable", context.packageName)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF8F9FA))
            .padding(8.dp)
    ) {
        Column {
            Text(
                text = teamName,
                fontSize = 10.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (peonResId != 0) Image(painterResource(peonResId), null, modifier = Modifier.size(40.dp))
                if (reyResId != 0) Image(painterResource(reyResId), null, modifier = Modifier.size(50.dp))
                if (temploResId != 0) {
                    Image(
                        painter = painterResource(temploResId),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        alpha = 0.5f
                    )
                }
            }
        }
    }
}

fun getSkinName(skinId: String): String {
    return when (skinId.lowercase()) {
        "skin0" -> "Onitama"
        "skin1" -> "Ajedrez"
        "skin2" -> "El Clásico"
        "skin3" -> "Medieval"
        "skin4" -> "Minimalista"
        "skin5" -> "Pradera Solar"
        "skin6" -> "Hechizo de Calabaza"
        else -> skinId
    }
}