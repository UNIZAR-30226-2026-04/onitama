package com.example.onitama.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.onitama.R

@Composable
fun WaitingMatchScreen() {
    val quattrocentoBold = FontFamily(Font(R.font.quattrocento_bold))
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

        Image(
            painter = painterResource(id = R.drawable.fading_white),
            contentDescription = "Gradiente",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )




        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.TopCenter) // Se ancla arriba del todo
                .background(colorResource(id = R.color.azulFondo))
                .padding(horizontal = 16.dp)
        ) {
            // A) Botón de Perfil
            IconButton(
                onClick = { /* Acción perfil */ },
                modifier = Modifier
                    .size(60.dp)
                    .align(Alignment.CenterStart)
            ) {
                // Aquí puedes poner la imagen de tu botón redondeado
            }

            // B) Título del juego
            Image(
                painter = painterResource(id = R.drawable.onitama_text),
                contentDescription = "Titulo",
                modifier = Modifier
                    .height(60.dp)
                    .align(Alignment.Center)
            )

            // C) Contadores (Katanas y Core)
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.katanas), contentDescription = "Katanas", modifier = Modifier.size(30.dp))
                    Text("123", color = Color.White, fontSize = 24.sp, fontFamily = quattrocentoBold, modifier = Modifier.padding(start = 4.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(id = R.drawable.core), contentDescription = "Core", modifier = Modifier.height(30.dp))
                    Text("123", color = Color.White, fontSize = 24.sp, fontFamily = quattrocentoBold, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }


}