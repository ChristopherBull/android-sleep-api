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

You should update your Manifest with the name and location of you `BroadcastReceiver`. Update the name field to the correct package and class name of your Sleep Broadcast Receiver:

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

## Re-register for Sleep Updates after System Boot

When a device is rebooted, the `BroadcastReceiver` needs to be re-registered. For passive apps, such as sleep monitoring, it is best to do this at device boot rather than waiting for a user to open the app. You will need to update the `AndroidManifest.xml`:

```xml
<manifest>
    
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    
    <application>
        
        <receiver
            android:name=".receiver.BootReceiver"
            android:enabled="true"
            android:exported="true"
            android:permission="android.permission.RECEIVE_BOOT_COMPLETED">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED"/>
                <action android:name="android.intent.action.QUICKBOOT_POWERON"/>
            </intent-filter>
        </receiver>
        
    </application>
</manifest>
```

You will also need to create a `BroadcastReciever` to receive the Boot action. An example can be seen in [`.receiver.BootReceiver`](app/src/main/java/caffeinatedandroid/androidsleepapi/receiver/BootReceiver.kt).

## Disable Auto-Revoking Permissions

Newer versions of Android [automatically remove permissions previously granted to apps](https://developer.android.com/about/versions/11/privacy/permissions#auto-reset) if the app is not used for a few months. This is typically desirable in most apps, but if your app is primarily used for passive monitoring where the user won't open the app directly (e.g., using the Sleep API and sending the data to a server), then Android will determine that the app is not being used and remove permissions. In these situations, this automatic removal of permissions is undesirable.

You can detect if your app is set to have permissions automatically removed. A user has to manually disable this feature in the app's permissions page, however, we can help the user by opening the app's general settings page for them, to more easily guide a user to disable this feature.

```kotlin
private fun updateAutoRevokePermissions(context: Context, checkOnly: Boolean = false) {
    val future: ListenableFuture<Int> = PackageManagerCompat.getUnusedAppRestrictionsStatus(context)
    future.addListener(
        { onResult(future.get(), checkOnly) },
        ContextCompat.getMainExecutor(context)
    )
}

private fun onResult(appRestrictionsStatus: Int, checkOnly: Boolean) {
    when (appRestrictionsStatus) {
        ERROR -> {
            // Status could not be fetched. Check logs for details.
        }
        FEATURE_NOT_AVAILABLE -> {
            // Restrictions do not apply to your app on this device.
        }
        DISABLED -> {
            // Restrictions have been disabled by the user for your app.
        }
        API_30_BACKPORT, API_30, API_31 -> {
            // Auto-removal of permissions is enabled
            handleRestrictions(appRestrictionsStatus)
        }
    }
}

private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

private fun handleRestrictions(appRestrictionsStatus: Int) {
    // Open the settings page
    val intent: Intent = IntentCompat.createManageUnusedAppRestrictionsIntent(applicationContext, packageName)
    resultLauncher.launch(intent)
}
```
