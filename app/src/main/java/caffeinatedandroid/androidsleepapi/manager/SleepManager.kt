package caffeinatedandroid.androidsleepapi.manager

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import caffeinatedandroid.androidsleepapi.data.SleepStore
import caffeinatedandroid.androidsleepapi.receiver.SleepReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest

object SleepManager {

    private const val TAG = "SleepManager"

    private fun createSleepReceiverPendingIntent(context: Context): PendingIntent {
        // Prepare intent flags
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_CANCEL_CURRENT
        }
        // Create the PendingIntent
        return PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SleepReceiver::class.java),
            flags
        )
    }

    /**
     * Register for sleep updates. Has optional success/failure callbacks.
     */
    @SuppressLint("MissingPermission")
    fun registerForSleepUpdates(
        context: Context,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((exception: Exception) -> Unit)? = null
    ) {
        // Ensure storage is initialised
        // NB. Keep storage init here, as Activity#onCreate does not run for BOOT_COMPLETED event.
        SleepStore.init(context)
        // Register for sleep updates
        ActivityRecognition.getClient(context)
            .requestSleepSegmentUpdates(
                createSleepReceiverPendingIntent(context),
                SleepSegmentRequest.getDefaultSleepSegmentRequest()
            )
            .addOnSuccessListener {
                Log.d(TAG, "Successfully subscribed to sleep data.")
                // onSuccess callback
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Exception when subscribing to sleep data: $exception")
                // onFailure callback
                onFailure?.invoke(exception)
            }
    }
}