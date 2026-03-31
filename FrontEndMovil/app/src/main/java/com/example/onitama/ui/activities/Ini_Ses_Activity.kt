package com.example.onitama.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onitama.ui.login.PantallaInicioSesion
import com.example.onitama.ui.login.ViewModelInicioSesion
import com.example.onitama.ui.theme.OnitamaTheme

class Ini_Ses_Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val viewModel: ViewModelInicioSesion = viewModel()

            OnitamaTheme {
                PantallaInicioSesion(
                    viewModel = viewModel,
                    onNavigateToRegistro = {
                        val intent = Intent(this, Reg_Activity::class.java)
                        startActivity(intent)
                    },
                    onLoginSuccess = {
                        val intent = Intent(this, MenuPrincipalActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}