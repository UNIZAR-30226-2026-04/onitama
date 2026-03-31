package com.example.onitama.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.onitama.R
import com.example.onitama.ui.components.BotonPrincipal
import com.example.onitama.ui.components.BotonSecundario

/**
 * Pantalla de bienvenida.
 *
 * Esta función es un Composable que representa la pantalla
 * inicial. El usuario elige si iniciar sesión o registrarse.
 *
 * @param onNavigateToLogin Función que lleva a la pantalla de inicio sesión.
 * @param onNavigateToRegistro Función que lleva a la pantalla de registro.
 */
@Composable
fun PantallaBienvenida(
    onNavigateToLogin: () -> Unit,
    onNavigateToRegistro: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Fondo de pantalla
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = "Fondo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo de Onitama
            Image(
                painter = painterResource(id = R.drawable.onitama_text),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(300.dp)
                    .padding(bottom = 64.dp)
            )

            BotonPrincipal(
                texto = "Iniciar sesión",
                onClick = onNavigateToLogin
            )

            Spacer(modifier = Modifier.height(16.dp))

            BotonPrincipal(
                texto = "Registrarse",
                onClick = onNavigateToRegistro
            )
        }
    }
}
