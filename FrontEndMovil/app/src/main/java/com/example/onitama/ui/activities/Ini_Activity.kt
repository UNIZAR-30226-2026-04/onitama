package com.example.onitama.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.onitama.ui.welcome.PantallaBienvenida
import com.example.onitama.ui.theme.OnitamaTheme

class Ini_Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            OnitamaTheme {
                PantallaBienvenida(
                    onNavigateToLogin = {
                        val intent = Intent(this, Ini_Ses_Activity::class.java)
                        startActivity(intent)
                    },
                    onNavigateToRegistro = {
                        val intent = Intent(this, Reg_Activity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}