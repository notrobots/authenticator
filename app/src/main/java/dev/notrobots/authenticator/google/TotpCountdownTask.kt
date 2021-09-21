/*
* Copyright 2019 Google LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package dev.notrobots.authenticator.google

import android.os.Handler
import java.util.concurrent.TimeUnit

/**
 * Task that periodically notifies its listener about the time remaining until the value of a TOTP
 * counter changes.
 */
class TotpCountdownTask(
    counter: TotpCounter,
    clock: TotpClock,
    remainingTimeNotificationPeriod: Long
) : Runnable {
    private val mCounter: TotpCounter = counter
    private val mClock: Clock = clock
    private val mRemainingTimeNotificationPeriod: Long = remainingTimeNotificationPeriod
    private val mHandler = Handler()
    private var mLastSeenCounterValue = Long.MIN_VALUE
    private var mShouldStop = false
    private var mListener: Listener? = null

    /**
     * Listener notified of changes to the time remaining until the counter value changes.
     */
    interface Listener {
        /**
         * Invoked when the time remaining till the TOTP counter changes its value.
         *
         * @param millisRemaining time (milliseconds) remaining.
         */
        fun onTotpCountdown(millisRemaining: Long)

        /** Invoked when the TOTP counter changes its value.  */
        fun onTotpCounterValueChanged()
    }

    /**
     * Sets the listener that this task will periodically notify about the state of the TOTP counter.
     *
     * @param listener listener or `null` for no listener.
     */
    fun setListener(listener: TotpCountdownTask.Listener?) {
        mListener = listener
    }

    /**
     * Starts this task and immediately notifies the listener that the counter value has changed.
     *
     *
     * The immediate notification during startup ensures that the listener does not miss any
     * updates.
     *
     * @throws IllegalStateException if the task has already been stopped.
     */
    fun startAndNotifyListener() {
        check(!mShouldStop) { "Task already stopped and cannot be restarted." }
        run()
    }

    /**
     * Stops this task. This task will never notify the listener after the task has been stopped.
     */
    fun stop() {
        mShouldStop = true
    }

    override fun run() {
        if (mShouldStop) {
            return
        }
        val now: Long = mClock.nowMillis()
        val counterValue = getCounterValue(now)
        if (mLastSeenCounterValue != counterValue) {
            mLastSeenCounterValue = counterValue
            fireTotpCounterValueChanged()
        }
        fireTotpCountdown(getTimeTillNextCounterValue(now))
        scheduleNextInvocation()
    }

    private fun scheduleNextInvocation() {
        val now: Long = mClock.nowMillis()
        val counterValueAge = getCounterValueAge(now)
        val timeTillNextInvocation =
            mRemainingTimeNotificationPeriod - counterValueAge % mRemainingTimeNotificationPeriod
        mHandler.postDelayed(this, timeTillNextInvocation)
    }

    private fun fireTotpCountdown(timeRemaining: Long) {
        if (!mShouldStop) {
            mListener?.onTotpCountdown(timeRemaining)
        }
    }

    private fun fireTotpCounterValueChanged() {
        if (!mShouldStop) {
            mListener?.onTotpCounterValueChanged()
        }
    }

    /**
     * Gets the value of the counter at the specified time instant.
     *
     * @param time time instant (milliseconds since epoch).
     */
    private fun getCounterValue(time: Long): Long {
        return mCounter.getValueAtTime(TimeUnit.MILLISECONDS.toSeconds(time))
    }

    /**
     * Gets the time remaining till the counter assumes its next value.
     *
     * @param time time instant (milliseconds since epoch) for which to perform the query.
     *
     * @return time (milliseconds) till next value.
     */
    private fun getTimeTillNextCounterValue(time: Long): Long {
        val currentValue = getCounterValue(time)
        val nextValue = currentValue + 1
        val nextValueStartTime: Long = TimeUnit.SECONDS.toMillis(mCounter.getValueStartTime(nextValue))

        return nextValueStartTime - time
    }

    /**
     * Gets the age of the counter value at the specified time instant.
     *
     * @param time time instant (milliseconds since epoch).
     *
     * @return age (milliseconds).
     */
    private fun getCounterValueAge(time: Long): Long {
        return time - TimeUnit.SECONDS.toMillis(mCounter.getValueStartTime(getCounterValue(time)))
    }
}