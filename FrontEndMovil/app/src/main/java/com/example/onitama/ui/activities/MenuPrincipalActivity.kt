package com.example.onitama.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.onitama.R
import com.example.onitama.autoLogin

class MenuPrincipalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Información de la parte superior de la pantalla
        val botonPerfil = findViewById<ImageButton>(R.id.btn_perfil)
        val cores = findViewById<TextView>(R.id.core_count)
        val katanas = findViewById<TextView>(R.id.katanas_count)

        val nombre = autoLogin.obtenerNombre(this)
        val valorCores = autoLogin.obtenerCores(this)
        val valorKatanas = autoLogin.obtenerKatanas(this)

        cores.text = valorCores.toString()
        katanas.text = valorKatanas.toString()

        // Botones de los tipos de partidss disponibles
        val botonPartidaOnline = findViewById<Button>(R.id.btn_publicmatch)
        val botonPartidaEntrenamiento = findViewById<Button>(R.id.btn_botmatch)
        val botonPartidaPrivada = findViewById<Button>(R.id.btn_privatematch)

        // Otros botones en la parte inferior para cambiar a diferentes menus
        val botonTablerosSkins = findViewById<ImageButton>(R.id.btn_myskins)
        val botonCartas = findViewById<ImageButton>(R.id.btn_mycards)
        val botonJugarPartidas = findViewById<ImageButton>(R.id.btn_AjUGAR)
        val botonAmigos = findViewById<ImageButton>(R.id.btn_friends)
        val botonTienda = findViewById<ImageButton>(R.id.btn_shop)

        // Configuramos los botonse, INCOMPLETO
        botonPerfil.setOnClickListener {

        }

        botonPartidaOnline.setOnClickListener {

        }

        botonPartidaEntrenamiento.setOnClickListener {

        }

        botonPartidaPrivada.setOnClickListener {

        }

        botonTablerosSkins.setOnClickListener {

        }

        botonCartas.setOnClickListener {

        }

        botonJugarPartidas.setOnClickListener {

        }

        botonAmigos.setOnClickListener {

        }

        botonTienda.setOnClickListener {

        }
    }
}