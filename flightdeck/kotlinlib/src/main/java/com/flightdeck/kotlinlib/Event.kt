package cc.flightdeck.kotlinlib

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    @SerialName("client_type")
    val clientType: String,

    @SerialName("client_version")
    val clientVersion: String,

    @SerialName("client_config")
    val clientConfig: String,

    val event: String,

    @SerialName("datetime_utc")
    val datetimeUTC: String,

    @SerialName("datetime_local")
    var datetimeLocal: String? = null,

    var timezone: String? = null,
    var language: String? = null,

    @SerialName("properties")
    var properties: String? = null,

    @SerialName("app_version")
    var appVersion: String? = null,

    @SerialName("os_name")
    var osName: String? = null,

    @SerialName("os_version")
    var osVersion: String? = null,

    @SerialName("debug")
    var debug: Boolean? = null,

    @SerialName("device_manufacturer")
    var deviceManufacturer: String? = null,

    @SerialName("device_model")
    var deviceModel: String? = null,

    @SerialName("first_of_session")
    var firstOfSession: Boolean? = null,

    @SerialName("first_of_hour")
    var firstOfHour: Boolean? = null,

    @SerialName("first_of_day")
    var firstOfDay: Boolean? = null,

    @SerialName("first_of_week")
    var firstOfWeek: Boolean? = null,

    @SerialName("first_of_month")
    var firstOfMonth: Boolean? = null,

    @SerialName("first_of_quarter")
    var firstOfQuarter: Boolean? = null,

    @SerialName("previous_event")
    var previousEvent: String? = null,

    @SerialName("previous_event_datetime_utc")
    var previousEventDatetimeUTC: String? = null
)