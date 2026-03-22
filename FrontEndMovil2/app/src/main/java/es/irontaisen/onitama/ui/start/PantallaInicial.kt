package es.irontaisen.onitama.ui.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import es.irontaisen.onitama.R

/**
 * Pantalla al abrir la aplicación.
 *
 * Esta función es un Composable que representa la pantalla que
 * se muestra al abrir la aplicación durante 5 segundos.
 */
@Composable
fun PantallaInicial(onTimeout: () -> Unit) {

    // Ejecuta esto una vez al crear el Composable
    LaunchedEffect(Unit) {
        // Espera 5 segundos
        kotlinx.coroutines.delay(5000)
        // Llama a la función para mostrar la siguiente pantalla
        onTimeout()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // La imagen del fondo de pantalla
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = "Fondo pantalla",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // El logo que se muestra se pone en un contenedor
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 350.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // La imagen del logo de Onitama
            Image(
                painter = painterResource(id = R.drawable.onitama_logo),
                contentDescription = "Logo Onitama",
                modifier = Modifier.fillMaxWidth(0.8f),
                contentScale = ContentScale.FillWidth

            )

            Spacer(modifier = Modifier.height(12.dp))

            // La imagen de By Iron Taisen
            Image(
                painter = painterResource(id = R.drawable.by_iron_taysen),
                contentDescription = "By Iron Taysen",
                modifier = Modifier.fillMaxWidth(0.5f),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}