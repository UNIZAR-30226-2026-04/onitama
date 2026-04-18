package com.example.onitama.ui.registro

import android.R.attr.onClick
import android.annotation.SuppressLint
import android.util.Log
import android.widget.ImageButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.onitama.R
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.ui.components.*
import java.util.concurrent.ThreadLocalRandom.current

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
@SuppressLint("LocalContextResourcesRead")
@Composable
fun PantallaRegistro(
    viewModel: ViewModelRegistro,
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    var chooseAvatar by remember { mutableStateOf(false) }

    LaunchedEffect(estado.creada) {
        if (estado.creada) {
            onNavigateToMain()
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
                        onClick = {
                            val conseguido = viewModel.onCrearClick()
                            if(conseguido){
                                Log.d("Registro", "Registro exitoso con ${estado.nombre}, ${estado.correo}")
                                viewModel.onAvatarChange("None")
                                chooseAvatar = true
                            }
                            else{
                                Log.e("Error de registro", estado.error?: "Error desconocido")
                            }
                        },
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

                    Button(
                        onClick = {
                            viewModel.onAvatarChange("None")
                        },
                    ){
                        Text("Ninguno")
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(12) { index ->
                            val i = index + 1

                            // Un truco de Kotlin para añadir el 0 delante automáticamente (reemplaza tu if)
                            val nombre = "avatar_${i.toString().padStart(2, '0')}"

                            val imageResId = context.resources.getIdentifier(
                                nombre, "drawable", context.packageName
                            )
                            val idSeguro = if (imageResId != 0) imageResId else R.drawable.onitama_text

                            // Comparamos el nombre de esta imagen con el que tiene el ViewModel
                            val isSelected = (estado.avatar == nombre)

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
                            viewModel.registrarDelTodo(context)
                        },
                    ){
                        Text("Siguiente")
                    }


                }
            }
        }

    }
}
