package com.example.onitama.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.onitama.R
import com.example.onitama.autoLogin
import com.example.onitama.ui.pages.MainMenuScreen

class MenuPrincipalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nombreUsuario = autoLogin.obtenerNombre(this) ?: "Jugador"
        val valorCores = autoLogin.obtenerCores(this)
        val valorKatanas = autoLogin.obtenerKatanas(this)
        setContent {
            // Un contenedor base opcional (útil para temas y colores de fondo por defecto)
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainMenuScreen(
                    nombre = nombreUsuario,
                    cores = valorCores,
                    katanas = valorKatanas
                )
            }
        }
    }
}