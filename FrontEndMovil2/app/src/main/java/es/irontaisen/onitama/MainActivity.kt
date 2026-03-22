package es.irontaisen.onitama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import es.irontaisen.onitama.ui.navigation.NavegacionInicial
import es.irontaisen.onitama.ui.theme.OnitamaTheme

/**
 * Actividad principal de la aplicación.
 *
 * Esta clase es el punto de entrada de la aplicación Android e
 * inicializa la interfaz de usuario utilizando Jetpack Compose.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OnitamaTheme {
                NavegacionInicial()
            }
        }
    }
}