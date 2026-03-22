package es.irontaisen.onitama.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import es.irontaisen.onitama.R
import es.irontaisen.onitama.ui.components.BotonPrincipal
import es.irontaisen.onitama.ui.components.EntradaContrasenya

/**
 * Pantalla de registro.
 *
 * Esta función es un Composable que representa la pantalla de
 * registro. El usuario debe introducir el nombre de usuario,
 * el correo electrónico y la contraseña.
 */
@Composable
fun PantallaRegistro(
    onCrearClick: (String, String, String) -> Unit
) {

    var usuario by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasenya by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }

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
                        text = "Registrarse",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Campo para introducir el nombre de usuario
                    OutlinedTextField(
                        value = usuario,
                        onValueChange = { usuario = it },
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

                    EntradaContrasenya(
                        entrada = contrasenya,
                        cambio = { contrasenya = it },
                        etiqueta = "Contraseña"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    EntradaContrasenya(
                        entrada = repetir,
                        cambio = { repetir = it },
                        etiqueta = "Repite la contraseña"
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    val coincide = (contrasenya == repetir)

                    // Botón para crear la cuenta
                    BotonPrincipal(
                        texto = "Crear cuenta",
                        onClick = { onCrearClick(usuario, correo, contrasenya) },
                        activar = coincide
                    )
                }
            }
        }
    }
}