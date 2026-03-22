package es.irontaisen.onitama.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import es.irontaisen.onitama.ui.start.PantallaInicial
import es.irontaisen.onitama.ui.login.PantallaInicioSesion

/**
 * Composable encargado de gestionar la navegación inicial.
 *
 * Esta función muestra la pantalla inicial durante un periodo
 * de tiempo, y después muestra la pantalla de login.
 */
@Composable
fun NavegacionInicial() {

    val controlador = rememberNavController()

    // Inicializa los valores necesarios para mostrar
    // la pantalla inicial hasta que se agote el timeout
    NavHost(
        navController = controlador,
        startDestination = "home"
    ) {

        // Pantalla inicial al abrir la aplicación
        composable("home") {
            PantallaInicial(
                onTimeout = {
                    controlador.navigate("login")
                }
            )
        }

        // Pantalla de login después de la anterior
        composable("login") {
            PantallaInicioSesion()
        }

    }
}