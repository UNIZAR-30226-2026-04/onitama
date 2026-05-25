package com.example.onitama.ui.activities.inicial

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.onitama.ui.activities.login.Ini_Ses_Activity
import com.example.onitama.ui.activities.registro.Reg_Activity
import com.example.onitama.ui.theme.OnitamaTheme
import com.example.onitama.ui.welcome.PantallaBienvenida

class Ini_Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OnitamaTheme {
                PantallaBienvenida(
                    onNavigateToLogin = {
                        val intent = Intent(this, Ini_Ses_Activity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onNavigateToRegistro = {
                        val intent = Intent(this, Reg_Activity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}