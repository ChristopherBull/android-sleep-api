package caffeinatedandroid.androidsleepapi.data

import android.content.Context
import android.util.Log
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent
import java.io.FileNotFoundException

/**
 * A simple data storage class for demonstrating the Sleep API.
 */
class SleepStore {
    companion object {
        const val TAG = "SleepStore"
        const val fileName: String = "sleepData.dat"
    }

    fun addSleepSegments(context: Context, sleepSegmentEvents: List<SleepSegmentEvent>) {
        val data: String = sleepSegmentEvents.joinToString()
        appendData(context, data)
    }

    fun addSleepClassifications(context: Context, sleepClassifyEvents: List<SleepClassifyEvent>) {
        val data: String = sleepClassifyEvents.joinToString()
        appendData(context, data)
    }

    private fun appendData(context: Context, data: String) {
        context.openFileOutput(fileName, Context.MODE_APPEND).use {
            it.write(data.toByteArray())
        }
    }

    fun readAllData(context: Context): String {
        var result: String
        try {
            context.openFileInput(fileName).use { fileInputStream ->
                fileInputStream.bufferedReader().use { bufferedReader ->
                    result = bufferedReader.readText()
                }
            }
        } catch (ex: FileNotFoundException) {
            result = ""
            ex.message?.let { Log.e(TAG, it) } ?: Log.e(TAG, ex.toString())
        }
        return result
    }
}