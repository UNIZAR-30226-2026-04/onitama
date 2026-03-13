package com.example.onitama.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.onitama.R
import com.example.onitama.api.Auth
import kotlinx.coroutines.launch
import com.example.onitama.lib.validar
import com.example.onitama.autoLogin

class Ini_Ses_Activity: AppCompatActivity()  {

    private val authClient = Auth()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.init_sesion)
        val nameMailEditText = findViewById<EditText>(R.id.IniSesUsr)
        val passwordEditText = findViewById<EditText>(R.id.IniSesPwd)
        val loginButton = findViewById<Button>(R.id.btn_login)
        val registerInstead = findViewById<TextView>(R.id.notAccount)

        registerInstead.setOnClickListener {
            val intent: Intent = Intent(this, Reg_Activity::class.java)
            startActivity(intent)
        }

        nameMailEditText.setOnClickListener {
            nameMailEditText.text.clear()
         }

        loginButton.setOnClickListener {
            val nameMail = nameMailEditText.text.toString()
            val password = passwordEditText.text.toString()
            if(nameMail.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Llamamos al servidor (o al mock)
                    val datosSesion = authClient.iniciarSesion(nameMail, password)
                    
                    autoLogin.inicioSesion(
                        this@Ini_Ses_Activity,
                        datosSesion.nombre,
                        datosSesion.puntos,
                        datosSesion.cores
                    )

                    val intent = Intent(this@Ini_Ses_Activity, MenuPrincipalActivity::class.java)
                    
                    startActivity(intent)
                    
                    // Usamos finish() para que no puedan volver al login usando el botón "Atrás"
                    finish() 

                } catch (e: Exception) {
                    loginButton.isEnabled = true
                    
                    // Algo falló (contraseña mal, usuario no existe, sin internet...)
                    Toast.makeText(this@Ini_Ses_Activity, e.message ?: "Error al iniciar sesión", Toast.LENGTH_LONG).show()
                }
            }

        }

    }
}