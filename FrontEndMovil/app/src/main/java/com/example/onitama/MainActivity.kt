package com.example.onitama

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.onitama.ui.activities.Ini_Activity
import com.example.onitama.ui.activities.MenuPrincipalActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (autoLogin.yaHaIniciadoSesion(this)){
            // Creación del Intent
            val intent = Intent(this, MenuPrincipalActivity::class.java)

            // Lo iniciamos
            startActivity(intent)
        }
        else {
            // Creación del Intent
            val intent = Intent(this, Ini_Activity::class.java)

            // Lo iniciamos
            startActivity(intent)
        }

        finish()
    }
}