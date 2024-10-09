package com.example.rrhe

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.LinkedList
import java.util.Queue
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.WorkManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.system.exitProcess

private var currentToast: Toast? = null
private val toastQueue: Queue<String> = LinkedList()
private val handler = Handler(Looper.getMainLooper())
private const val TOAST_DELAY = 2000L // Delay in milliseconds between toasts

fun showToast(context: Context, message: String) {
    // Add the message to the queue only if it doesn't already exist to prevent duplicates
    if (!toastQueue.contains(message)) {
        toastQueue.add(message)
    }
    // If no toast is currently being displayed, show the next one in the queue
    if (currentToast == null) {
        showNextToast(context)
    }
}

private fun showNextToast(context: Context) {
    // Retrieve the next message from the queue
    val message = toastQueue.poll()
    if (message != null) {
        // Cancel any currently displayed toast
        currentToast?.cancel()
        // Create and show the new toast
        currentToast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        currentToast?.show()

        // Schedule the next toast after the current one finishes
        handler.postDelayed({
            currentToast = null
            showNextToast(context)
        }, TOAST_DELAY)
    }
}

// Function to decode Unicode sequences
fun decodeUnicode(unicode: String): String {
    val unicodeStr = unicode.replace("\\\\u", "\\u")
    val regex = Regex("\\\\u([0-9A-Fa-f]{4})")
    return regex.replace(unicodeStr) {
        val code = it.groupValues[1].toInt(16)
        String(charArrayOf(code.toChar()))
    }
}

object TranslationHelper {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun translateToThai(text: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=th&dt=t&q=${text}"
                val request = Request.Builder().url(url).build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()

                    // Ensure the responseBody is not null
                    if (responseBody != null) {
                        // Parse the response
                        val result = gson.fromJson(responseBody, List::class.java)

                        // Ensure result is not null and is a list
                        if (result != null && result.isNotEmpty()) {
                            val translatedText = (result[0] as? List<*>)?.getOrNull(0) as? List<*>

                            // Ensure translatedText is not null and get the first element
                            if (translatedText != null && translatedText.isNotEmpty()) {
                                return@withContext translatedText[0].toString()
                            }
                        }
                    }
                }
                // Return original text if translation fails
                text
            } catch (e: Exception) {
                e.printStackTrace()
                // Return original text in case of an exception
                text
            }
        }
    }
}

object Utils {
    fun terminateApp(context: Context) {
        Log.d("Utils", "Closing open sockets and stopping background tasks")
        ApiClient.closeAllConnections()
        SyncManager.stopSyncing()

        // Cancel all WorkManager tasks
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWork()

        Log.d("Utils", "Terminating the app")
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.appTasks.forEach { task ->
            task.finishAndRemoveTask()
        }

        exitProcess(0)
    }

    /*
    class ScreenLockReceiver(private val inactivityDetector: InactivityDetector) : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                Log.d("ScreenLockReceiver", "Screen turned off")
                inactivityDetector.startTerminationProcess()
            }
        }
    }
     */
}
