package caffeinatedandroid.androidsleepapi

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import caffeinatedandroid.androidsleepapi.data.SleepStore
import caffeinatedandroid.androidsleepapi.manager.SleepManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Ensure storage is initialised.
        SleepStore.init(this)

        // Prepare handling of permission request response from user
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission granted.
                    Log.d(TAG, "Permission granted: ACTIVITY_RECOGNITION")
                    findViewById<TextView>(R.id.txtStatusPermission).text = "Granted"
                    SleepManager.registerForSleepUpdates(
                        applicationContext,
                        // Callback functions
                        ::registerForSleepUpdatesOnSuccess,
                        ::registerForSleepUpdatesOnFailure
                    )
                    showSleepDataOnUI(applicationContext)
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
                SleepManager.registerForSleepUpdates(
                    applicationContext,
                    // Callback functions
                    ::registerForSleepUpdatesOnSuccess,
                    ::registerForSleepUpdatesOnFailure
                )
                showSleepDataOnUI(applicationContext)
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

    private fun registerForSleepUpdatesOnSuccess() {
        findViewById<TextView>(R.id.txtStatusSleepUpdates).text = "Subscribed"
    }

    private fun registerForSleepUpdatesOnFailure(exception: Exception) {
        findViewById<TextView>(R.id.txtStatusSleepUpdates).text = "Error"
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
        findViewById<TextView>(R.id.txtStatusSleepData).text = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    fun refreshSleepDataViews(view: View) {
        showSleepDataOnUI(view.context)
    }

    override fun onResume() {
        super.onResume()
        showSleepDataOnUI(this)
    }
}