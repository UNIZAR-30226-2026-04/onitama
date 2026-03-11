package com.example.onitama.ui.activities


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.onitama.R


class Ini_Reg_Activity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_init_regist)


        val loginButton = findViewById<Button>(R.id.boton_ini_sesion)
        val registerButton = findViewById<Button>(R.id.boton_registrarse)

        registerButton.setOnClickListener {
            val intent: Intent = Intent(this, Ini_Ses_Activity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {

        }
    }
}