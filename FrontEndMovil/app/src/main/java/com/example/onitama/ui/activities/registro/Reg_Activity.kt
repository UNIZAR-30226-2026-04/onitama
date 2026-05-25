package com.example.onitama.ui.activities.registro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onitama.ui.activities.ajugar.MenuPrincipalActivity
import com.example.onitama.ui.activities.login.Ini_Ses_Activity
import com.example.onitama.ui.theme.OnitamaTheme

class Reg_Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ViewModelRegistro = viewModel()

            OnitamaTheme {
                PantallaRegistro(
                    viewModel = viewModel,
                    onNavigateToLogin = {
                        val intent = Intent(this, Ini_Ses_Activity::class.java)
                        startActivity(intent)
                        finish()
                    },
                    onNavigateToMain = {
                        val intent = Intent(this, MenuPrincipalActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
        }
    }
}