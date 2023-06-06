package cc.flightdeck.kotlinlib

import android.content.Context

data class Configuration(
    val projectId: String,
    val projectToken: String,
    val context: Context,
    val addEventMetadata: Boolean = true,
    val trackAutomaticEvents: Boolean = true,
    val trackUniqueEvents: Boolean = true,
)