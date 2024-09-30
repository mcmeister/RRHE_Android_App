package com.example.rrhe

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class InactivityDetector(private val context: Context, private val inactivityTimeout: Long = 120000, private val terminationTimeout: Long = 480000) {

    private val handler: Handler = Handler(Looper.getMainLooper())
    private val inactivityRunnable: Runnable = Runnable {
        Log.d("InactivityDetector", "Inactivity detected, starting termination process")
        startTerminationProcess()
    }
    private val terminationRunnable: Runnable = Runnable {
        Log.d("InactivityDetector", "Termination process triggered, app will terminate")
        Toast.makeText(context, "User inactive for 10 minutes!", Toast.LENGTH_SHORT).show()
        Utils.terminateApp(context)
    }

    private var isTimerRunning: Boolean = false
    private var isTerminationRunning: Boolean = false

    // Start the inactivity timer
    private fun startInactivityTimer() {
        if (!isTimerRunning) {
            Log.d("InactivityDetector", "Inactivity timer started, waiting for $inactivityTimeout milliseconds of inactivity")
            handler.postDelayed(inactivityRunnable, inactivityTimeout)
            isTimerRunning = true
        }
    }

    // Start the termination process if inactivity is detected
    fun startTerminationProcess() {
        if (!isTerminationRunning) {
            Log.d("InactivityDetector", "Starting termination process, app will terminate in $terminationTimeout milliseconds")
            handler.postDelayed(terminationRunnable, terminationTimeout)
            isTerminationRunning = true
        }
    }

    // Stop the inactivity and termination timers
    fun stop() {
        if (isTimerRunning || isTerminationRunning) {
            Log.d("InactivityDetector", "Inactivity and/or termination timer stopped")
            handler.removeCallbacks(inactivityRunnable)
            handler.removeCallbacks(terminationRunnable)
            isTimerRunning = false
            isTerminationRunning = false
        }
    }

    // Reset the inactivity timer by stopping and starting it
    fun reset() {
        Log.d("InactivityDetector", "Inactivity timer reset")
        stop()  // Stop any existing timers
        startInactivityTimer() // Start a new inactivity timer
    }

    // Implementing MyWorkerFactory within InactivityDetector
    class MyWorkerFactory : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters
        ): ListenableWorker? {
            Log.d("MyWorkerFactory", "Attempting to create worker: $workerClassName")
            return try {
                val workerClass = Class.forName(workerClassName).asSubclass(ListenableWorker::class.java)
                val constructor = workerClass.getConstructor(Context::class.java, WorkerParameters::class.java)
                constructor.newInstance(appContext, workerParameters)
            } catch (e: Exception) {
                Log.e("MyWorkerFactory", "Error creating worker: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}
