package com.example.onitama.ui.activities.tienda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.onitama.ui.theme.OnitamaTheme

class Tienda_Activity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: ViewModelTienda = viewModel()

            OnitamaTheme {
                PantallaTienda(viewModel = viewModel)
            }
        }
    }
}