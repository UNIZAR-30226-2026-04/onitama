package com.example.onitama.ui.tableros

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onitama.ui.theme.OnitamaTheme

class Tableros_Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ViewModelTableros = viewModel()

            OnitamaTheme {
                PantallaTableros(viewModel)
            }
        }
    }
}