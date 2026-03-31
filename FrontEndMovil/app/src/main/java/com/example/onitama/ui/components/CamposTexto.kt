package com.example.onitama.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.onitama.R

/**
 * Campo de texto para contraseñas con conmutador de visibilidad.
 */
@Composable
fun CampoContrasenya(
    entrada: String,
    cambio: (String) -> Unit,
    etiqueta: String
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = entrada,
        onValueChange = cambio,
        label = {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelMedium
            )
        },
        singleLine = true,
        visualTransformation = if (passwordVisible)
            VisualTransformation.None
        else
            PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(
                onClick = { passwordVisible = !passwordVisible }
            ) {
                Icon(
                    painter = if (passwordVisible)
                        painterResource(id = R.drawable.ic_visibilityoff)
                    else
                        painterResource(id = R.drawable.ic_visibility),
                    contentDescription = "Visibilidad"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}
