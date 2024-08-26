package com.example.rrhe

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

class AppLifecycleObserver(
    private val onAppBackgrounded: () -> Unit,
    private val onAppForegrounded: () -> Unit
) : DefaultLifecycleObserver {

    private var isInactivityTimerStarted = false

    override fun onStop(owner: LifecycleOwner) {
        if (owner == ProcessLifecycleOwner.get()) {
            if (!isInactivityTimerStarted) {
                isInactivityTimerStarted = true
                Log.d("AppLifecycleObserver", "App moved to background")
                onAppBackgrounded()
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (owner == ProcessLifecycleOwner.get()) {
            if (isInactivityTimerStarted) {
                isInactivityTimerStarted = false
                Log.d("AppLifecycleObserver", "App moved to foreground")
                onAppForegrounded()
            }
        }
    }
}
