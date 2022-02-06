package dev.notrobots.authenticator.models

import android.os.Handler
import android.os.Looper
import dev.notrobots.androidstuff.util.now
import java.util.concurrent.TimeUnit

class TotpTimer(
    /**
     * Interval of time (seconds) between successive changes of the TOTP pin.
     */
    timeSteps: Long,
    /**
     * Frequency at which [Listener.onTick] is invoked.
     *
     * Smaller values means smoother transitions.
     */
    val updateDelay: Long = DEFAULT_UPDATE_DELAY
) : Runnable {
    private val counter = TotpCounter(timeSteps)
    private val handler = Handler(Looper.getMainLooper())
    private var cachedValue: Long? = -1
    private var isStopped = false
    private var listener: Listener? = null

    override fun run() {
        if (isStopped) {
            return
        }

        val now = now()
//        val counterValueAge = counter.getCounterValueAge(now)
//        val nextInvocation = 150 - counterValueAge % 150
        val counterValue = counter.getValueAtTime(now, TimeUnit.MILLISECONDS)

        if (cachedValue != counterValue) {
            cachedValue = counterValue
            listener?.onValueChanged()
        }

        listener?.onTick(getTimeLeftUntilNextValue(now))
        // handler.postDelayed(this, nextInvocation)
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
        if (!isStopped) {
            run()
        }
    }

    /**
     * Stops the timer
     */
    fun stop() {
        isStopped = true
    }

    /**
     * Gets the time remaining till the counter assumes its next value.
     *
     * @param time time instant (milliseconds since epoch) for which to perform the query.
     *
     * @return time (milliseconds) till next value.
     */
    private fun getTimeLeftUntilNextValue(time: Long): Long {
        val currentValue = counter.getValueAtTime(time, TimeUnit.MILLISECONDS)
        val nextValue = currentValue + 1
        val nextValueStartTime = TimeUnit.SECONDS.toMillis(counter.getValueStartTime(nextValue))

        return nextValueStartTime - time
    }

    companion object {
        const val DEFAULT_UPDATE_DELAY = 100L
    }

    interface Listener {
        /**
         * Invoked every time the countdown indicator should be updated.
         */
        fun onTick(timeLeft: Long)

        /**
         * Invoked when the counter's value has changed.
         */
        fun onValueChanged()
    }
}