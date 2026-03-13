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
import com.example.onitama.lib.validar
import kotlinx.coroutines.launch

class Reg_Activity: AppCompatActivity() {

    private val authClient = Auth()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.regis)
        val nameEditText = findViewById<EditText>(R.id.RegUsr)
        val emailEditText = findViewById<EditText>(R.id.RegEmail)
        val passwordEditText = findViewById<EditText>(R.id.RegPwd)
        val regButton = findViewById<Button>(R.id.btn_regis)
        val IniInstead = findViewById<TextView>(R.id.yesAccount)

        IniInstead.setOnClickListener {
            val intent: Intent = Intent(this, Ini_Ses_Activity::class.java)
            startActivity(intent)
        }

        regButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if(name.isEmpty() || password.isEmpty() || email.isEmpty()){
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if(!validar(password)){
                Toast.makeText(this, "La contraseña debe contener al menos 8 caracteres, de los cuales al menos 1 ha de ser un número", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            regButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Llamamos al servidor (o al mock)
                    authClient.registrarUsuario(email, name, password )

                    Toast.makeText(this@Reg_Activity, "Registro completado con éxito. A continuación inica sesión", Toast.LENGTH_LONG).show()
                    
                    val intent = Intent(this@Reg_Activity, Ini_Ses_Activity::class.java)
                    
                    startActivity(intent)
                    
                    // Usamos finish() para que no puedan volver al login usando el botón "Atrás"
                    finish()

                } catch (e: Exception) {
                    regButton.isEnabled = true
                    // Algo falló (contraseña mal, usuario no existe, sin internet...)
                    Toast.makeText(this@Reg_Activity, e.message ?: "Error al registrarse", Toast.LENGTH_LONG).show()
                }
            }

        }

    }
}