package com.example.daysmark.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.example.daysmark.android.designsystem.DaysmarkTheme
import com.example.daysmark.android.navigation.LaunchGate
import com.example.daysmark.android.navigation.LaunchRequest

class MainActivity : ComponentActivity() {
    /** What a tapped reminder or widget asked to open; the navigation host consumes it. */
    private val request = mutableStateOf<LaunchRequest?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) request.value = LaunchRequest.from(intent)
        val graph = (application as DaysmarkApplication).graph
        setContent {
            DaysmarkTheme {
                LaunchGate(
                    graph,
                    request.value,
                    onRequestHandled = { request.value = null },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // The device may have slept through a midnight while the app was in the background.
        (application as DaysmarkApplication).graph.services.onClockChanged()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        LaunchRequest.from(intent)?.let { request.value = it }
    }
}
