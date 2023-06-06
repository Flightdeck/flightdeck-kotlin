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
import cc.flightdeck.kotlinlib.Configuration
import cc.flightdeck.kotlinlib.Flightdeck
import com.example.flightdeckkotlin.ui.theme.FlightdeckKotlinTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Flightdeck.getInstance(
            Configuration(
                projectId = "flightdeck_demo",
                projectToken = "p.eyJ1IjogIjVhZjBlOWZlLTA3MTEtNDNiMi1hZmNkLTY3MzZhYjBhM2Q5MiIsICJpZCI6ICIzMzc0MDg4OC02OWUyLTQxZGItOWIwOC1iM2E3YzI0NTUzYmIifQ._TMjeCrtematM2ex9d105n9mYmZ-dIWhkazGPrjc8RY",
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
    var tapCount = 0

    Flightdeck.getInstance().setSuperProperties(mapOf(
        "Subscription type" to "premium",
        "Purchase count" to 12
    ))

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
            Flightdeck.getInstance().trackEvent("button-tap", mapOf("tap-count" to tapCount))
            tapCount++
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