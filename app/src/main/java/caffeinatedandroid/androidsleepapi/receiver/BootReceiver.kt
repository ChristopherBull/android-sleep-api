package caffeinatedandroid.androidsleepapi.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import caffeinatedandroid.androidsleepapi.manager.SleepManager

class BootReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive(): ${intent.action}")
        SleepManager.registerForSleepUpdates(context)
    }
}