package com.m7md7sn.labra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.m7md7sn.labra.navigation.NavGraph
import com.m7md7sn.labra.ui.theme.LabraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LabraTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
