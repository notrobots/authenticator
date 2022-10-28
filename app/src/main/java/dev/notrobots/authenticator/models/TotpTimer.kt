package dev.notrobots.authenticator.models

import android.os.Handler
import android.os.Looper
import dev.notrobots.androidstuff.util.now

class TotpTimer(
    /**
     * Frequency at which [Listener.onTick] is invoked.
     *
     * Smaller values means smoother transitions.
     */
    val updateDelay: Long
) : Runnable {
    private val handler = Handler(Looper.getMainLooper())
    private var isStopped = true
    private var listener: Listener? = null

    override fun run() {
        if (isStopped) {
            return
        }

        listener?.onTick(now())
        handler.postDelayed(this, updateDelay)
    }

    /**
     * Sets the timer's listener
     */
    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    /**
     * Starts the timer
     */
    fun start() {
        if (isStopped) {
            isStopped = false
            run()
        }
    }

    /**
     * Stops the timer
     */
    fun stop() {
        isStopped = true
    }

    interface Listener {
        /**
         * Invoked every time the countdown indicator should be updated.
         */
        fun onTick(currentTime: Long)
    }
}