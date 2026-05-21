package com.example.onitama

import android.os.Bundle
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.onitama.api.Auth
import com.example.onitama.api.ManejadorGlobal
import com.example.onitama.ui.inicial.Ini_Activity
import com.example.onitama.ui.activities.MenuPrincipalActivity
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (AutoLogin.haySesionActiva(this)){
            // Creación del Intent
            val credenciales = AutoLogin.datosIni(this)
            val authClient: Auth = Auth()
            val prevcontext = this
            runBlocking {
                try {
                    //con esto se inicia sesión
                    val conectado = ManejadorGlobal.conectarYMantener()
                    if(conectado){
                        authClient.iniciarSesion(
                            credenciales!!.nombre, credenciales.contrasenya
                        )
                        //con esto otro se actualiza el perfil (iniciarsesión no tiene partidas ganadas o jugadas)
                        val datos = authClient.obtenerPerfil(credenciales.nombre)

                        // Guardamos la sesión en el Singleton 'AutoLogin'
                        AutoLogin.inicioSesion(
                            prevcontext,
                            datos!!.nombre,
                            datos.puntos,
                            datos.cores,
                            datos.avatar_id,
                            datos.skin_activa,
                        )
                        AutoLogin.actualizar(prevcontext, datos as DatosPerfil?)
                    }
                    else{
                        Log.e("ERROR", "No se pudo conectar al servidor")
                        ManejadorGlobal.desconectar()
                    }
                } catch (e: Exception) {
                    ManejadorGlobal.desconectar()
                }
            }
            val intent = Intent(this, MenuPrincipalActivity::class.java)

            // Lo iniciamos
            startActivity(intent)
            finish()
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