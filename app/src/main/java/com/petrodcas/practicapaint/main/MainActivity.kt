package com.petrodcas.practicapaint.main

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.petrodcas.practicapaint.R

class MainActivity : AppCompatActivity() {

    /**
     * Flags usadas para esconder la barra de navegación y la de estado
     */
    private val flags: Int = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)

        //se añade un listener para que, si la visibilidad actual no se corresponde con la de pantalla completa
        //vuelva a activar las flags para esconder las barras
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    hideStatusAndNavigationBars()
                }
            }
        }
    }


    /**
     * Aplica las flags necesarias para esconder la barra de navegación y estado
     */
    private fun hideStatusAndNavigationBars () {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.decorView.systemUiVisibility = flags
        }
    }


    /**
     * Si se cambia el focus de la ventana, entonces vuelve a esconder las barras de navegación y estado
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hideStatusAndNavigationBars()
    }

}