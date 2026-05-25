package com.example.onitama.ui.activities.notificaciones

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onitama.ui.theme.OnitamaTheme

class Notificaciones_Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ViewModelNotificaciones = viewModel()

            OnitamaTheme {
                PantallaNotificaciones(viewModel = viewModel)
            }
        }
    }
}
