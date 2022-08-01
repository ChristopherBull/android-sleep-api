# Android Sleep API Sample
Sample implementation of the Android Sleep API for detecting when someone is asleep or awake using an Android phone.

## Configuring a project

Set the minimum and compile SDK versions to version `29` or higher in your module's Gradle build file (e.g., `app/build.gradle`)

```groovy
android {
    compileSdk 29

    defaultConfig {
        minSdk 29
    }
}
```

Add the Google Play services (location) dependency, also to your module's Gradle build file (e.g., `app/build.gradle`). Sleep API functions used in this sample require v18.0.0+ of the `play-services-location` for functions such as: `ActivityRecognitionClient#requestSleepSegmentUpdates()`.

```groovy
dependencies {
    // Google Play Services (Sleep API)
    implementation 'com.google.android.gms:play-services-location:20.0.0'
    // ...
}
```

You also need the `ACTIVITY_RECOGNITION` permission in your manifest file.

```xml
<manifest>
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />
</manifest>
```

## Check for permissions

Follow the latest guidance on [best practice for requesting permissions](https://developer.android.com/training/permissions/requesting#request-permission). An example of checking for `Manifest.permission.ACTIVITY_RECOGNITION` can be found in [MainActivity.kt](app/src/main/java/caffeinatedandroid/androidsleepapi/MainActivity.kt).

## Register for Sleep Updates

Create a [BroadcastReceiver](https://developer.android.com/reference/android/content/BroadcastReceiver) to handle the Sleep Events and override the `BroadcastReceiver#onReceive()`, see an example in [SleepReceiver.kt](app/src/main/java/caffeinatedandroid/androidsleepapi/receiver/SleepReceiver.kt). When receiving a Sleep update, you need to handle `SleepSegmentEvent` and `SleepClassifyEvent`:

```kotlin
if (SleepSegmentEvent.hasEvents(intent)) {
    val sleepSegmentEvents: List<SleepSegmentEvent> =
        SleepSegmentEvent.extractEvents(intent)
    Log.d(TAG, "SleepSegmentEvent List: $sleepSegmentEvents")
    // TODO handle this SleepSegmentEvent
} else if (SleepClassifyEvent.hasEvents(intent)) {
    val sleepClassifyEvents: List<SleepClassifyEvent> =
        SleepClassifyEvent.extractEvents(intent)
    Log.d(TAG, "SleepClassifyEvent List: $sleepClassifyEvents")
    // TODO handle this SleepClassifyEvent
}
```

You should update your Manifest with the name and location of you `BroadcastReceiver`. Update the name field to the correct package and class name of your Sleep Boradcast Receiver:

```xml
<manifest>
    <application>
        <receiver
            android:name=".receiver.SleepReceiver"
            android:enabled="true"
            android:exported="true"
            android:permission="android.permission.ACTIVITY_RECOGNITION" />
    </application>
</manifest>
```

Prepare an intent prior to registering for sleep updates using the above BroadcastReceiver:

```kotlin
val context = applicationContext
var sleepPendingIntent = PendingIntent.getBroadcast(
    context,
    PERMISSION_REQUEST_CODE_ACTIVITY_RECOGNITION,
    Intent(context, SleepReceiver::class.java),
    PendingIntent.FLAG_CANCEL_CURRENT + PendingIntent.FLAG_IMMUTABLE
)
```

Finally, register for sleep updates with the Sleep API:

```kotlin
ActivityRecognition.getClient(context)
    .requestSleepSegmentUpdates(
        sleepPendingIntent,
        SleepSegmentRequest.getDefaultSleepSegmentRequest())
    .addOnSuccessListener {
        Log.d(TAG, "Successfully subscribed to sleep data.")
    }
    .addOnFailureListener { exception ->
        Log.d(TAG, "Exception when subscribing to sleep data: $exception")
    }
```
