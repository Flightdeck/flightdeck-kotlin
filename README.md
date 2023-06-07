![maven central](https://maven-badges.herokuapp.com/maven-central/cc.flightdeck/flightdeck-kotlin/badge.png) ![apache2short](https://user-images.githubusercontent.com/3425455/214007387-ced3e898-63e7-4c66-bc08-e113be00e3c3.svg) [![Percentage of issues still open](http://isitmaintained.com/badge/open/Flightdeck/flightdeck-swift.svg)](http://isitmaintained.com/project/Flightdeck/flightdeck-swift "Percentage of issues still open") [![Better Uptime Badge](https://betteruptime.com/status-badges/v1/monitor/lmrx.svg)](https://status.flightdeck.cc/)




# ![flightdeck-logo-flat-brand](https://user-images.githubusercontent.com/3425455/212749718-85e425da-1e17-4c80-8dc0-c7db3b04490c.svg) Flightdeck for Android

## Installation
Add the following line to the dependencies section in **app/build.gradle**:
```
implementation 'cc.flightdeck:flightdeck-kotlin:1.x'
```

Request permission to access internet by adding the following line to your **AndroidManifest.xml**:
```
<uses-permission android:name="android.permission.INTERNET" />
```

## Initialize Flightdeck

Import FlightDeck into your main activity and initialize it in your code by calling Flightdeck.getInstance() with the correct configuration. You can find your **projectId** and **projectToken** in your Flightdeck project settings. Make sure to include your application context exactly as in the example below.

```kotlin
import cc.flightdeck.kotlinlib.Configuration
import cc.flightdeck.kotlinlib.Flightdeck

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Flightdeck.getInstance(
            Configuration(
                projectId = "FLIGHTDECK_PROJECT_ID",
                projectToken = "FLIGHTDECK_PROJECT_TOKEN",
                context = applicationContext
            )
        )
    }
    ...
}
```


### Advanced initialization
`Flightdeck.initialize(...)`

#### Parameters

The **Configuration** object can have the following parameters:

| Parameter              | Required      | Type                         | Description                                                                 |
| ---------------------- | ------------- | ---------------------------- | --------------------------------------------------------------------------- |
| projectId              | Required      | `String`                     | Project ID¹                                                                 |
| ProjectToken           | Required      | `String`                     | Project write API token¹                                                    |
| addEventMetadata       | Optional      | `Bool` default **true**      | Enable device, timezone, and app version metadata to be added to each event |
| trackAutomaticEvents   | Optional      | `Bool` default **true**      | Enable tracking automatic events (e.g. Session start)                       |
| trackUniqueEvents      | Optional      | `Bool` default **false**     | Enable tracking daily and monthly unique events²                            |

¹ Project ID and project token are generated on project creation and can be found in the project settings by team admins and owners.

² To track whether an event has been sent before during the current day or month, a list of previously sent events is stored on the device. The information stored includes event names only. No idenfitying information or metadata is stored. However, legislation in some countries forbids storing information on a user's device without explicit permission, even when this information does not contain personal data. If you want to run Flightdeck without making any use of permanent storage, keep this option disabled. Note that session uniqueness is always tracked, because it doesn't require storing any inromation.

## Track event

### Track an event

```kotlin
Flightdeck.getInstance().trackEvent("New project created", mapOf(
  "Subscription type" to "premium",
  "Active projects" to 12,
))
```

#### Parameters

| Parameter  | Required   | Type                 | Description                                   |
| ---------- | ---------- | -------------------- | --------------------------------------------- |
| event      | Required   | `String`             | Name of the event                             |
| properties | Optional   | `Map<String, Any>`¹  | Any metadata you want to attach to the event  |

¹ The properties map expects keys as `String` and any type that can be converted to string (e.g. String, Int, Double, Bool) as value.


## Set super properties

Set properties that are automatically sent with each event.

```kotlin
Flightdeck.getInstance().setSuperProperties(mapOf(
  "Subscription type" to "premium",
  "Active projects" to 12,
))
```

When you pass a similarly named property with `trackEvent()` as one of the super properties, the super proerty will be overwritten for that specific trackEvent() call.
