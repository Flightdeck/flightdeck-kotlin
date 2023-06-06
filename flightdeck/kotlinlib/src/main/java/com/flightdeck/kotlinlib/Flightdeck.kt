package com.flightdeck.kotlinlib

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class Flightdeck private constructor(config: Configuration) {
    private val projectId = config.projectId
    private val projectToken = config.projectToken
    private val context = config.context
    private val addEventMetadata = config.addEventMetadata
    private val trackAutomaticEvents = config.trackAutomaticEvents
    private val trackUniqueEvents = config.trackUniqueEvents

    private val clientType = "AndroidLib"
    private val clientVersion = "1.0.5"
    private val clientConfig: String = "${if (addEventMetadata) 1 else 0}${if (trackAutomaticEvents) 1 else 0}${if (trackUniqueEvents) 1 else 0}"
    private val eventAPIURL = "https://api.flightdeck.cc/v0/events"
    private val automaticEventsPrefix = "(FD) "

    private var staticMetaData: StaticMetaData? = null
    private var superProperties = mutableMapOf<String, Any>()
    private val eventsTrackedThisSession = mutableListOf<String>()
    private var eventsTrackedBefore = mutableMapOf<EventPeriod, EventSet>()
    private var movedToBackgroundTime: Date? = null
    private var previousEvent: String? = null
    private var previousEventDateTimeUTC: String? = null

    data class StaticMetaData(
        val language: String?,
        val appVersion: String,
        val osName: String,
        val deviceModel: String,
        val deviceManufacturer: String,
        val osVersion: String?
    )

    @Serializable
    enum class EventPeriod {
        Hour, Day, Week, Month, Quarter
    }

    @Serializable
    data class EventSet(
        var date: Int,
        var events: MutableSet<String> = mutableSetOf()
    )

    @Serializable
    data class EventSetIndices(
        var date: Int,
        var events: List<Int> = listOf()
    )

    init {
        // Set static metadata if tracked
        if (addEventMetadata) {

            staticMetaData = StaticMetaData(
                language = Locale.getDefault().language,
                appVersion = context.getPackageInfo().versionName,
                osName = "Android",
                deviceModel = Build.MODEL,
                deviceManufacturer = Build.MANUFACTURER,
                osVersion = Build.VERSION.RELEASE.substringBefore(".")
            )
        }

        // Listen for lifecycle updates for actions on app start, close, and background
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        lifecycle.addObserver(AppLifecycleObserver())

        // Retrieve events that have been tracked before from SharedPreferences
        if (trackUniqueEvents) {

            // Initialize eventsTrackedBefore and set empty default values for each period
            val trackedBefore = mutableMapOf<EventPeriod, EventSet>().apply {
                EventPeriod.values().forEach { period ->
                    put(period, EventSet(date = getCurrentDatePeriod(period)))
                }
            }

            // Retrieve eventsTrackedBefore from local storage
            val sharedPreferences = context.getSharedPreferences("FlightdeckPrefs", Context.MODE_PRIVATE)

            // Retrieve list with unique event names
            sharedPreferences.getString("FDUniqueEvents", null)?.let { jsonString ->
                val storedUniqueEventsArray: List<String> = Json.decodeFromString(jsonString)

                // Retrieve events tracked before for every time period and convert EventSetIndices to EventSet
                trackedBefore.forEach { (period, eventSet) ->
                    sharedPreferences.getString("FDEventsTrackedBefore.${period.name}", null)?.let { jsonString ->
                        val storedEventSetIndices: EventSetIndices = Json.decodeFromString(jsonString)
                        if (storedEventSetIndices.date == eventSet.date) {
                            val eventNames = storedEventSetIndices.events.mapNotNull { index ->
                                if (storedUniqueEventsArray.indices.contains(index)) storedUniqueEventsArray[index] else null
                            }
                            trackedBefore[period] = EventSet(date = storedEventSetIndices.date, events = eventNames.toMutableSet())
                        }
                    }
                }
            }

            eventsTrackedBefore = trackedBefore
        }

        // Track session start
        trackAutomaticEvent("Session start")
    }

    companion object {
        private var instance: Flightdeck? = null

        fun getInstance(config: Configuration? = null): Flightdeck {
            if (instance == null) {
                if (config == null) {
                    throw IllegalStateException("Flightdeck must be initialized with a Configuration first.")
                }
                instance = Flightdeck(config)
            }
            return instance!!
        }
    }

    /**
     * Sets properties that are included with each event during the duration of the current initialization.
     * Super properties are reset every time the app is terminated.
     * Make sure to set necessary super properties every time after Flightdeck.initialize() is called.
     *
     * Super properties can be overwritten by similarly named properties that are provided with trackEvent()
     *
     * @param properties Properties dictionary
     */
    fun setSuperProperties(properties: MutableMap<String, Any>) {
        superProperties = properties
    }

    // MARK: - trackEvent

    /**
     * Tracks an event with properties.
     * Properties are optional and can be added only if needed.
     *
     * Properties will allow you to segment your events in your Mixpanel reports.
     * Property keys must be Strings and values must conform to String, Int, Double, or Bool.
     *
     * @param event event name
     * @param properties properties dictionary
     */

    fun trackEvent(event: String, properties: Map<String, Any>? = null) {
        if (event.startsWith(automaticEventsPrefix)) {
            Log.e("Flightdeck","Flightdeck: Event name has forbidden prefix $automaticEventsPrefix")
        } else {
            trackEventCore(event, properties)
        }
    }

    /**
     * Private trackEvent function used for automatic events
     *
     * @param event event name
     * @param properties properties dictionary
     */
    private fun trackAutomaticEvent(event: String, properties: Map<String, Any>? = null) {
        if (trackAutomaticEvents) {
            trackEventCore("$automaticEventsPrefix$event", properties)
        }
    }

    /**
     * Core trackEvent function
     *
     * @param event event name
     * @param properties properties dictionary
     */
    private fun trackEventCore(event: String, properties: Map<String, Any>? = null) {

        val currentDateTime = getCurrentDateTime()

        // Initialize a new Event object with event string and current UTC datetime
        val eventData = Event(
            clientType = clientType,
            clientVersion = clientVersion,
            clientConfig = clientConfig,
            event = event,
            datetimeUTC = currentDateTime.datetimeUTC
        )

        // Set custom properties, merged with super properties, if any
        val props: MutableMap<String, Any> = properties?.toMutableMap() ?: mutableMapOf()
        props.putAll(superProperties)
        val convertedProps: Map<String?, Any?> = props.mapKeys { it.key }
        eventData.properties = JSONObject(convertedProps).toString()

        // Add metadata to event
        if (addEventMetadata) {

            // Set local time and timezone
            eventData.datetimeLocal = currentDateTime.datetimeLocal
            eventData.timezone = currentDateTime.timezone

            // Set static metadata
            staticMetaData?.let { metaData ->
                eventData.language = metaData.language
                eventData.appVersion = metaData.appVersion
                eventData.osName = metaData.osName
                eventData.osVersion = metaData.osVersion
                eventData.deviceModel = metaData.deviceModel
                eventData.deviceManufacturer = metaData.deviceManufacturer
            }
        }

        // Set previous event name and datetime if any
        eventData.previousEvent = previousEvent
        eventData.previousEventDatetimeUTC = previousEventDateTimeUTC

        // Store current event name and datetime for use as future previous event
        previousEvent = eventData.event
        previousEventDateTimeUTC = eventData.datetimeUTC

        // Set event uniqueness of current session
        eventData.firstOfSession = trackFirstOfSession(eventData.event)

        // Set daily and monthly uniqueness of event
        if (trackUniqueEvents) {
            val isFirstOf = trackFirstOfPeriod(eventData.event)
            eventData.firstOfHour = isFirstOf[EventPeriod.Hour]
            eventData.firstOfDay = isFirstOf[EventPeriod.Day]
            eventData.firstOfWeek = isFirstOf[EventPeriod.Week]
            eventData.firstOfMonth = isFirstOf[EventPeriod.Month]
            eventData.firstOfQuarter = isFirstOf[EventPeriod.Quarter]
        }

        // Convert Event object to JSON
        val eventDataJSON = Json.encodeToString(eventData)

        val job = Job()
        val scope = CoroutineScope(job + Dispatchers.IO)
        scope.launch {
            post(eventDataJSON)
        }
    }

    // Helper functions

    /** Send event data **/
    private fun post(payload: String) {

        val url = URL("$eventAPIURL?name=$projectId")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $projectToken")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        try {
            connection.outputStream.use { os ->
                os.write(payload.toByteArray())
                os.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_ACCEPTED) {
                val content = connection.inputStream.bufferedReader().use { it.readText() }
                Log.e(
                    "Flightdeck",
                    "Flightdeck: Failed to send event to server. Response code: $responseCode, Error message: $content"
                )
            }
        } catch (e: Exception) {
            Log.e(
                "Flightdeck",
                "Flightdeck: Failed to send event to server. Error: ${e.localizedMessage}"
            )
            e.printStackTrace()
        } finally {
            connection.disconnect()
        }
    }

    /**
    Get the current UTC datetime, local datetime, and timezone code

    - parameters: none
    - returns: CurrentDateTime object
     */

    data class CurrentDateTime(
        val datetimeUTC: String,
        val datetimeLocal: String,
        val timezone: String
    )

    @SuppressLint("SimpleDateFormat")
    private fun getCurrentDateTime(): CurrentDateTime {
        val dateNow = Date()
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        dateFormatter.timeZone = TimeZone.getDefault()
        val datetimeLocal = dateFormatter.format(dateNow)

        dateFormatter.timeZone = TimeZone.getTimeZone("UTC")
        val datetimeUTC = dateFormatter.format(dateNow)

        return CurrentDateTime(
            datetimeUTC = datetimeUTC,
            datetimeLocal = datetimeLocal,
            timezone = TimeZone.getDefault().id
        )
    }


    /**
    Check if a specified event has been tracked before and set event as tracked if it was the first occurrence.

    - parameter event: Event name
    - returns:         true if event is first of session, false if event has been tracked before
     */
    private fun trackFirstOfSession(event: String): Boolean {
        return if (eventsTrackedThisSession.contains(event)) {
            false
        } else {
            eventsTrackedThisSession.add(event)
            true
        }
    }


    /**
    Check if a specified event has been tracked before during the current period and set event as tracked if it was the first occurrence.

    - parameter event:     Event name
    - parameter period:    Period string ("day", "month")
    - returns:             Dictionary of EventPeriod keys with boolean reflecting
    whether an event has been tracked before during the period
     */
    private fun trackFirstOfPeriod(event: String): Map<EventPeriod, Boolean> {
        return EventPeriod.values().associateWith { period ->
            eventsTrackedBefore.getOrPut(period) {
                EventSet(date = getCurrentDatePeriod(period))
            }.let { eventSet ->
                if (eventSet.date == getCurrentDatePeriod(period) && event !in eventSet.events) {
                    eventsTrackedBefore[period] = eventSet.copy(events = eventSet.events.apply { add(event) })
                    true
                } else {
                    false
                }
            }
        }
    }


    /**
     * Get the current ordinal number representing a date period in this year.
     *
     * @param period hour, day, week, month, or quarter
     * @return Integer representing the period in the year (or other larger timeframe)
     */
    private fun getCurrentDatePeriod(period: EventPeriod): Int {
        val calendar = Calendar.getInstance()

        return when (period) {
            EventPeriod.Hour -> {
                calendar.get(Calendar.HOUR) * calendar.get(Calendar.DAY_OF_YEAR)
            }
            EventPeriod.Day -> {
                calendar.get(Calendar.DAY_OF_YEAR)
            }
            EventPeriod.Week -> {
                calendar.get(Calendar.WEEK_OF_YEAR)
            }
            EventPeriod.Month -> {
                calendar.get(Calendar.MONTH) + 1 // Calendar.MONTH is zero-based
            }
            EventPeriod.Quarter -> {
                (calendar.get(Calendar.MONTH) / 3) + 1
            }
        }
    }


    /**
    Perform actions on app lifecycle state changes

    Session start: Occurs when Flightdeck singleton is initialized or app moves to foreground
    Session end: Occurs when app is terminated or moves to foreground after 60 seconds of being inactive

    Previous event: Event name and datetime of the previous event are removed on session end

    Unique events: Store events fired today and this month in UserDefaults when app terminates
     */
    inner class AppLifecycleObserver : DefaultLifecycleObserver {

        override fun onStop(owner: LifecycleOwner) {
            super.onStop(owner)
            appMovedToBackground()
        }

        override fun onStart(owner: LifecycleOwner) {
            super.onStart(owner)
            appMovedToForeground()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            appTerminated()
        }

        private fun appMovedToBackground() {
            movedToBackgroundTime = Date()
        }

        private fun appMovedToForeground() {
            val movedToBackgroundTime = movedToBackgroundTime

            if (movedToBackgroundTime != null) {
                val timeInterval = Date().time - movedToBackgroundTime.time

                if (timeInterval > 60 * 1000) {
                    eventsTrackedThisSession.clear()
                    previousEvent = null
                    previousEventDateTimeUTC = null
                    trackAutomaticEvent("Session start")
                }
            }
        }

        private fun appTerminated() {
            if (!trackUniqueEvents) return

            // Create an array with unique events from all event periods combined
            val uniqueEvents = eventsTrackedBefore.values.flatMap { it.events }.toSet()
            val uniqueEventsArray = uniqueEvents.toList() // Turn into list for indices

            // Store array with unique events
            val encodedUniqueEvents = Json.encodeToString(uniqueEventsArray)
            val sharedPreferences = context.getSharedPreferences("FDUniqueEvents", Context.MODE_PRIVATE)
            sharedPreferences.edit().putString("FDUniqueEvents", encodedUniqueEvents).apply()

            // Store events tracked before for every time period, using EventSetIndices for storage space optimization
            eventsTrackedBefore.forEach { (eventPeriod, eventSet) ->
                val eventIndices = eventSet.events.mapNotNull { uniqueEventsArray.indexOf(it) }
                val eventSetWithIndices = EventSetIndices(date = eventSet.date, events = eventIndices)
                val encodedData = Json.encodeToString(eventSetWithIndices)
                sharedPreferences.edit().putString("FDEventsTrackedBefore.${eventPeriod.name}", encodedData).apply()
            }
        }
    }

}

@Suppress("DEPRECATION")
fun Context.getPackageInfo(): PackageInfo {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        packageManager.getPackageInfo(packageName, 0)
    }
}