package com.example.onitama

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.onitama.ui.activities.Ini_Ses_Activity
import com.example.onitama.ui.theme.OnitamaTheme
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.onitama.ui.activities.Ini_Reg_Activity
import com.example.onitama.ui.activities.MenuPrincipalActivity
import com.example.onitama.autoLogin

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
            val intent = Intent(this, Ini_Reg_Activity::class.java)

            // Lo iniciamos
            startActivity(intent)
        }

        finish()
    }
}