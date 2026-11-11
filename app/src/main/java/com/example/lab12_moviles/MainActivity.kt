package com.example.lab12_moviles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.lab12_moviles.ui.theme.Lab12MovilesTheme
import com.google.android.gms.maps.MapsInitializer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Inicializar Google Maps SDK antes de renderizar la UI
        MapsInitializer.initialize(applicationContext)
        setContent {
            Lab12MovilesTheme {
                MapScreen()
            }
        }
    }
}
