package com.jetcar.vidrox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.jetcar.vidrox.ui.screens.YoutubeWV
import com.jetcar.vidrox.ui.theme.NoTubeTVTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoTubeTVTheme {
                Box(modifier = Modifier.fillMaxSize()) { YoutubeWV() }
            }
        }
    }
}
