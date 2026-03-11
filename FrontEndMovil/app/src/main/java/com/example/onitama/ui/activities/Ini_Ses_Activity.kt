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

class Ini_Ses_Activity: AppCompatActivity()  {
    private val authClient = Auth("ws://TU_IP_O_DOMINIO:PUERTO")
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



        loginButton.setOnClickListener {
            val nameMail = nameMailEditText.text.toString()
            val password = passwordEditText.text.toString()
            if(nameMail.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // 3. Feedback visual: deshabilitamos el botón para evitar doble clic
            loginButton.isEnabled = false
            loginButton.text = "CARGANDO..."

            // 4. Lanzamos la corrutina
            lifecycleScope.launch {
                try {
                    // Llamamos al servidor (o al mock)
                    val datosSesion = authClient.iniciarSesion(nameMail, password)
                    
                    
                    val intent = Intent(this@Ini_Ses_Activity, MenuPrincipalActivity::class.java)
                    startActivity(intent)
                    finish() // Usamos finish() para que no puedan volver al login usando el botón "Atrás"
                    

                } catch (e: Exception) {
                    // Algo falló (contraseña mal, usuario no existe, sin internet...)
                    Toast.makeText(this@Ini_Ses_Activity, e.message ?: "Error al iniciar sesión", Toast.LENGTH_LONG).show()
                } finally {
                    // 5. Restauramos el botón falle o acierte
                    loginButton.isEnabled = true
                    loginButton.text = "CONTINUAR"
                }
            }


        }



    }
}