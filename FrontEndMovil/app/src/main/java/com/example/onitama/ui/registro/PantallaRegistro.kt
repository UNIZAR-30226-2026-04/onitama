package com.example.onitama.ui.registro

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.onitama.R
import com.example.onitama.ui.components.*

/**
 * Pantalla de registro.
 *
 * Esta función es un Composable que representa la pantalla de
 * registro. El usuario debe introducir el nombre de usuario,
 * el correo electrónico y la contraseña (dos veces).
 *
 * @param viewModel View Model que gestiona el estado y la lógica.
 * @param onNavigateToLogin Función que lleva a la pantalla
 * de inicio de sesión.
 */
@Composable
fun PantallaRegistro(
    viewModel: ViewModelRegistro,
    onNavigateToLogin: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado.creada) {
        if (estado.creada) {
            onNavigateToLogin()
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
                .fillMaxHeight(0.75f)
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

                    Text(
                        text = "Registrarse",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Campo para introducir el nombre de usuario
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

                    // Campo para introducir el correo electrónico
                    OutlinedTextField(
                        value = estado.correo,
                        onValueChange = { viewModel.onCorreoChange(it) },
                        label = {
                            Text(
                                text = "Correo electrónico",
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CampoContrasenya(
                        entrada = estado.contrasenya,
                        cambio = { viewModel.onContrasenyaChange(it) },
                        etiqueta = "Contraseña"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CampoContrasenya(
                        entrada = estado.contrasenyaR,
                        cambio = { viewModel.onContrasenyaRChange(it) },
                        etiqueta = "Repite la contraseña"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val coincide = (estado.contrasenya == estado.contrasenyaR)

                    // Botón 'Crear cuenta' para crear la cuenta de usuario
                    BotonPrincipal(
                        texto = "Crear cuenta",
                        onClick = { viewModel.onCrearClick() },
                        activado = coincide
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¿Ya tienes una cuenta?",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón 'Iniciar Sesión' para acceder a la pantalla de inicio de sesión
                    BotonSecundario(
                        texto = "Inicia sesión",
                        onClick = onNavigateToLogin
                    )
                }
            }
        }
    }
}
