package com.docconverter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.docconverter.app.ui.screens.HomeScreen
import com.docconverter.app.ui.theme.DocConverterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DocConverterTheme {
                HomeScreen()
            }
        }
    }
}
