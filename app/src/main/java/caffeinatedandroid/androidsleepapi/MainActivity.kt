package caffeinatedandroid.androidsleepapi

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import caffeinatedandroid.androidsleepapi.data.SleepStore
import caffeinatedandroid.androidsleepapi.receiver.SleepReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.SleepSegmentRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE_ACTIVITY_RECOGNITION = 1000
    }

    private lateinit var sleepPendingIntent: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ensure the files for storing data are created
        SleepStore.init(this)

        // Preparation of intents before registering for sleep updates
        val context = applicationContext
        sleepPendingIntent = PendingIntent.getBroadcast(
            context,
            PERMISSION_REQUEST_CODE_ACTIVITY_RECOGNITION,
            Intent(context, SleepReceiver::class.java),
            PendingIntent.FLAG_CANCEL_CURRENT + PendingIntent.FLAG_IMMUTABLE
        )

        // Prepare handling of permission request response from user
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission granted.
                    Log.d(TAG, "Permission granted: ACTIVITY_RECOGNITION")
                    findViewById<TextView>(R.id.txtStatusPermission).text = "Granted"
                    registerForSleepUpdates(applicationContext)
                    showSleepDataOnUI(context)
                } else {
                    // Permission declined.
                    Log.d(TAG, "Permission declined: ACTIVITY_RECOGNITION")
                    findViewById<TextView>(R.id.txtStatusPermission).text = "Declined"
                    // Inform user of unavailable features.
                    AlertDialog.Builder(this)
                        .setMessage("Sleep tracking is only available if the Activity Recognition permission is granted. You can activate this later.")
                        .setTitle("Permission declined")
                        .create()
                        .show()
                }
            }

        // Check permissions and request them if not granted
        when {
            checkSelfPermission(
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission granted.
                Log.d(TAG, "Permission granted: ACTIVITY_RECOGNITION")
                findViewById<TextView>(R.id.txtStatusPermission).text = "Granted (previously)"
                registerForSleepUpdates(applicationContext)
                showSleepDataOnUI(context)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION) -> {
                // Show rationale for required permission. Must include a no/cancel option.
                Log.d(TAG, "Permission will be requested after informing user of rationale (Activity Recognition).")
                val alertDialog: AlertDialog? = this.let {
                    val builder = AlertDialog.Builder(it)
                    builder.apply {
                        setMessage("Need activity permission please")
                        setTitle("Permissions needed")
                        setPositiveButton("ok"
                        ) { _, _ ->
                            // User clicked OK button - request permissions
                            Log.d(TAG, "Permission will be requested (Activity Recognition).")
                            requestPermissionLauncher.launch(
                                Manifest.permission.ACTIVITY_RECOGNITION)
                        }
                        setNegativeButton("cancel"
                        ) { _, _ ->
                            // User cancelled the dialog
                            Log.d(TAG, "Permission request cancelled by user (Activity Recognition).")
                            findViewById<TextView>(R.id.txtStatusPermission).text = "Request cancelled by user"
                        }
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog?.show()
            }
            else -> {
                // Request permission
                Log.d(TAG, "Permission will be requested, rationale not required (Activity Recognition).")
                requestPermissionLauncher.launch(
                    Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun registerForSleepUpdates(context: Context) {
        // Register for sleep updates
        ActivityRecognition.getClient(context)
            .requestSleepSegmentUpdates(
                sleepPendingIntent,
                SleepSegmentRequest.getDefaultSleepSegmentRequest())
            .addOnSuccessListener {
                Log.d(TAG, "Successfully subscribed to sleep data.")
                findViewById<TextView>(R.id.txtStatusSleepUpdates).text = "Subscribed"
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Exception when subscribing to sleep data: $exception")
                findViewById<TextView>(R.id.txtStatusSleepUpdates).text = "Error"
            }
    }

    private fun showSleepDataOnUI(context: Context) {
        // Show existing sleep data in the UI
        val data = SleepStore().readAllData(context)
        if (data.isEmpty()) {
            findViewById<TextView>(R.id.sleepDataContent).text = "<No data: empty file>"
        } else {
            findViewById<TextView>(R.id.sleepDataContent).text = data
        }
        // Show timestamp of last read
        findViewById<TextView>(R.id.txtStatusSleepData).text = "Read: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }
}