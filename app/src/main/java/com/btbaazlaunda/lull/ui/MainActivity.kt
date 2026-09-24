package com.btbaazlaunda.lull.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.btbaazlaunda.lull.ui.home.HomeRoute
import com.btbaazlaunda.lull.ui.theme.LullTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LullTheme {
                HomeRoute()
            }
        }
    }
}
