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
