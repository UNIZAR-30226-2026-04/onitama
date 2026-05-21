package com.example.onitama.ui.perfil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onitama.ui.theme.OnitamaTheme

class Perfil_Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            OnitamaTheme {
                PantallaPerfil(
                    viewModel = viewModel()
                )
            }
        }
    }
}