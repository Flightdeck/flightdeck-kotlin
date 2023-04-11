package com.example.flightdeckkotlin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flightdeckkotlin.ui.theme.FlightdeckKotlinTheme
import com.flightdeck.kotlinlib.Configuration
import com.flightdeck.kotlinlib.Flightdeck

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Flightdeck.getInstance(
            Configuration(
                projectId = "flightdeck_demo",
                projectToken = "p.eyJ1IjogIjVhZjBlOWZlLTA3MTEtNDNiMi1hZmNkLTY3MzZhYjBhM2Q5MiIsICJpZCI6ICI1NGZiYzYwNi1mMGRmLTQ1MjctOTYwZi1lMmRlYWQ3ZjRhZTkifQ.cYVMYgDu3mGU-5Utka95VWXFSx6wuPKNYeacfSUtW8Y",
                context = applicationContext,
                addEventMetadata = true,
                trackAutomaticEvents = true,
                trackUniqueEvents = true

            )
        )

        setContent {
            FlightdeckKotlinTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    DemoSection()
                }
            }
        }
    }
}

@Composable
fun DemoSection() {
    val flightdeck = Flightdeck.getInstance()

    Column(
        modifier = Modifier.width(100.dp).height(100.dp).background(
            brush = Brush.verticalGradient(
                0.0f to Color(88,86,214,255),
                0.5f to Color(175,82,222,255),
                1.0f to Color(255,149,0,255)
            )
        ).wrapContentSize(align = Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Flightdeck", fontWeight = FontWeight.Bold, fontSize = 40.sp, color = Color.White,
            modifier = Modifier.padding(bottom = 20.dp))
        Button(onClick = {
            flightdeck.debug()
        }) {
            Text("Track this button")
        }
    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlightdeckKotlinTheme {
        DemoSection()
    }
}