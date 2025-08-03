package com.normflysoftware.pilotcrewrestcalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.normflysoftware.pilotcrewrestcalculator.ui.theme.PilotCrewRestCalculatorTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PilotCrewRestCalculatorTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Pilot Crew Rest Calculator") }
                        )
                    },
                    bottomBar = {
                        BottomAppBar {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = "© 2025 Normfly",
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    ) {
                        CrewRestCalculatorScreen() // <--- this is now imported from CrewRestCalculatorScreen.kt
                    }
                }
            }
        }
    }
}