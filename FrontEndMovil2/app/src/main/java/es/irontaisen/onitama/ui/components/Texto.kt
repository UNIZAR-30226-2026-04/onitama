package es.irontaisen.onitama.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import es.irontaisen.onitama.R

@Composable
fun EntradaContrasenya(
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
                        painterResource(id = R.drawable.ic_visibility)
                    else
                        painterResource(id = R.drawable.ic_visibilityoff),
                    contentDescription = "Visibilidad"
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}