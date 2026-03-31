package com.example.onitama.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.onitama.R
import com.example.onitama.ui.components.*

/**
 * Pantalla de inicio de sesión.
 *
 * Esta función es un Composable que representa la pantalla de
 * inicio de sesión. El usuario debe introducir el nombre de
 * usuario y la contraseña.
 *
 * @param viewModel View Model que gestiona el estado y la lógica.
 * @param onNavigateToRegistro Función que lleva a la pantalla de registro.
 * @param onLoginSuccess Función que lleva a la pantalla principal.
 */
@Composable
fun PantallaInicioSesion(
    viewModel: ViewModelInicioSesion,
    onNavigateToRegistro: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(estado.iniciada) {
        if (estado.iniciada) {
            onLoginSuccess()
        }
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

        // La imagen del logo de Onitama
        Image(
            painter = painterResource(id = R.drawable.onitama_text),
            contentDescription = "Logo Onitama",
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(20.dp)
                .size(250.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(
                topStart = 40.dp,
                topEnd = 40.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {

            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {

                    // Título 'Iniciar sesión'
                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Campo para introducir el correo electrónico
                    OutlinedTextField(
                        value = estado.nombre,
                        onValueChange = { viewModel.onNombreChange(it) },
                        label = {
                            Text(
                                text = "Nombre de usuario",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Campo para introducir la contraseña
                    CampoContrasenya(
                        entrada = estado.contrasenya,
                        cambio = { viewModel.onContrasenyaChange(it) },
                        etiqueta = "Contraseña"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón 'Entrar' para acceder con el correo y la contraseña
                    BotonPrincipal(
                        texto = "Entrar",
                        onClick = { viewModel.onEntrarClick(context) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¿Aún no tienes una cuenta?",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón 'Regístrate' para acceder a la pantalla de registro
                    BotonSecundario(
                        texto = "Regístrate",
                        onClick = onNavigateToRegistro
                    )
                }
            }
        }
    }
}
