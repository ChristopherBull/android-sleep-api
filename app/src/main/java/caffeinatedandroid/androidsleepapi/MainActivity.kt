package caffeinatedandroid.androidsleepapi

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.content.PackageManagerCompat
import androidx.core.content.UnusedAppRestrictionsConstants.*
import caffeinatedandroid.androidsleepapi.data.SleepStore
import caffeinatedandroid.androidsleepapi.manager.SleepManager
import com.google.common.util.concurrent.ListenableFuture
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
                    findViewById<TextView>(R.id.txtStatusPermission).text = "✔ Granted"
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
                    findViewById<TextView>(R.id.txtStatusPermission).text = "❌ Declined"
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
                findViewById<TextView>(R.id.txtStatusPermission).text = "✔ Granted (previously)"
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
                        setPositiveButton("ok") { _, _ ->
                            // User clicked OK button - request permissions
                            Log.d(TAG, "Permission will be requested (Activity Recognition).")
                            requestPermissionLauncher.launch(
                                Manifest.permission.ACTIVITY_RECOGNITION
                            )
                        }
                        setNegativeButton("cancel") { _, _ ->
                            // User cancelled the dialog
                            Log.d(TAG, "Permission request cancelled by user (Activity Recognition).")
                            findViewById<TextView>(R.id.txtStatusPermission).text =
                                "❌ Request cancelled by user"
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
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
        }
    }

    private fun registerForSleepUpdatesOnSuccess() {
        findViewById<TextView>(R.id.txtStatusSleepUpdates).text = "✔ Subscribed"
    }

    private fun registerForSleepUpdatesOnFailure(exception: Exception) {
        findViewById<TextView>(R.id.txtStatusSleepUpdates).text = "❌ Error"
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
        findViewById<TextView>(R.id.txtStatusSleepData).text =
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
    }

    fun refreshSleepDataViews(view: View) {
        showSleepDataOnUI(view.context)
    }

    override fun onResume() {
        super.onResume()
        showSleepDataOnUI(this)
        updateAutoRevokePermissions(this, checkOnly = true)
    }

    //////////////////////////////////////////////////////////////////////////////////
    // Prevent auto-revoking permissions                                            //
    // ---------------------------------                                            //
    // If the app runs primarily in the background (e.g., passive monitoring,       //
    // possibly offloading to a server), then we can consider asking the user to    //
    // disable auto-revoking of permissions. That is because the user may not open  //
    // the app and the OS will determine that the app is not being used and will    //
    // therefore revoke permissions--possibly breaking the user experience and data //
    // capture of a legitimate application. It is good practice to cache a user's   //
    // response to such a request, so you do not spam them with requests.           //
    // The `androidx.core` technique is cross compatible and works on Android 6.0+  //
    //////////////////////////////////////////////////////////////////////////////////

    /**
     * Click handler for updating/disabling auto-revoking permissions.
     */
    fun clickUpdateAutoRevokePermissions(view: View) {
        updateAutoRevokePermissions(view.context)
    }

    /**
     * Allow user to enable/disable auto-revoking of app permissions.
     */
    private fun updateAutoRevokePermissions(context: Context, checkOnly: Boolean = false) {
        val future: ListenableFuture<Int> =
            PackageManagerCompat.getUnusedAppRestrictionsStatus(context)
        future.addListener(
            { onResult(future.get(), checkOnly) },
            ContextCompat.getMainExecutor(context)
        )
    }

    /**
     * Check result of status request for the `PackageManagerCompat.getUnusedAppRestrictionsStatus()`
     */
    private fun onResult(appRestrictionsStatus: Int, checkOnly: Boolean) {
        when (appRestrictionsStatus) {
            // Status could not be fetched. Check logs for details.
            ERROR -> {
                findViewById<TextView>(R.id.txtStatusPermissionRevoke).text = "❌ Error. See logs."
            }

            // Restrictions do not apply to your app on this device.
            FEATURE_NOT_AVAILABLE -> {
                findViewById<TextView>(R.id.txtStatusPermissionRevoke).text =
                    "✔ Not available on this device"
            }

            // Restrictions have been disabled by the user for your app.
            DISABLED -> {
                findViewById<TextView>(R.id.txtStatusPermissionRevoke).text = "✔ Disabled"
                findViewById<TextView>(R.id.btnDisableAutoRevokePermissions).visibility = View.GONE
            }

            // If the user doesn't start your app for months, its permissions
            // will be revoked and/or it will be hibernated.
            // See the API_* constants for details.
            API_30_BACKPORT, API_30, API_31 -> {
                findViewById<TextView>(R.id.txtStatusPermissionRevoke).text = "⚠ Enabled"
                findViewById<TextView>(R.id.btnDisableAutoRevokePermissions).visibility =
                    View.VISIBLE
                if (!checkOnly) {
                    handleRestrictions(appRestrictionsStatus)
                }
            }
        }
    }

    // An empty Activity Result, as it is used to interact with app Settings page, so treating this
    // like a perform-and-forget operation. This app checks status within the onResume function.
    // Note: Must register for activity before life cycle starts. That is, keep this variable
    // global, not within a local scope of a function after a LifeCycle has started  (e.g., not
    // within or after the onCreate function of an Activity)
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    /**
     * Open the app's information page so the user can update permissions.
     */
    private fun handleRestrictions(appRestrictionsStatus: Int) {
        val intent: Intent =
            IntentCompat.createManageUnusedAppRestrictionsIntent(applicationContext, packageName)
        resultLauncher.launch(intent)
    }
}