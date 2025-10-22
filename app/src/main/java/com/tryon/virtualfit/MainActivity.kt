package com.tryon.virtualfit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.tryon.virtualfit.ui.screens.HomeScreen
import com.tryon.virtualfit.ui.theme.VirtualTryOnTheme
import com.tryon.virtualfit.viewmodel.TryOnViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: TryOnViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[TryOnViewModel::class.java]

        setContent {
            VirtualTryOnTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(viewModel = viewModel)
                }
            }
        }
    }
}
