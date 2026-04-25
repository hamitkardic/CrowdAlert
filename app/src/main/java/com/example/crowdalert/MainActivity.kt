package com.example.crowdalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.crowdalert.ui.navigation.CrowdAlertNavHost
import com.example.crowdalert.ui.theme.CrowdAlertTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CrowdAlertTheme {
                CrowdAlertNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}