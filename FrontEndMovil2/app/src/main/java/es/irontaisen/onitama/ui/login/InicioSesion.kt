package es.irontaisen.onitama.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import es.irontaisen.onitama.R
import es.irontaisen.onitama.ui.components.*


/**
 * Pantalla de inicio de sesión.
 *
 * Esta función es un Composable que representa la pantalla de
 * inicio de sesión. El usuario debe introducir el correo
 * electrónico y la contraseña.
 * Si no tiene cuenta, puede acceder a la pantalla de registro.
 */
@Composable
fun PantallaInicioSesion(
    onEntrarClick: (String, String) -> Unit,
    onRegistrarseClick: () -> Unit
) {

    var correo by remember { mutableStateOf("") }
    var contrasenya by remember { mutableStateOf("") }

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
            painter = painterResource(id = R.drawable.onitama_logo),
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

                    Text(
                        text = "Iniciar sesión",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Campo para introducir el correo electrónico
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
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

                    // Campo para introducir la contraseña
                    EntradaContrasenya(
                        entrada = contrasenya,
                        cambio = { contrasenya = it },
                        etiqueta = "Contraseña"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón para acceder con el correo y la contraseña especificados
                    BotonPrincipal(
                        texto = "Entrar",
                        onClick = { onEntrarClick(correo, contrasenya) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "¿Aún no tienes una cuenta?",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Botón para crear una cuenta
                    BotonSecundario(
                        texto = "Regístrate",
                        onClick = { onRegistrarseClick() }
                    )
                }
            }
        }
    }
}