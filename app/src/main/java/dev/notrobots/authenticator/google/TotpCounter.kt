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

/**
 * Counter whose value is a deterministic function of time as described in RFC 6238
 * "TOTP: Time-Based One-Time Password Algorithm".
 *
 *
 * The 64-bit counter assumes the value `0` at a predefined point in time and periodically
 * increments its value by one periodically.
 *
 *
 * The value `V` of the counter at time instant `T` is:
 * <pre>
 * V = (T - T0) / TimeStep
</pre> *
 * where `T0` is the earliest time instant at which the counter assumes the value `0`,
 * and `TimeStep` is the duration of time for which the values of the counter remain constant.
 *
 *
 * *Note: All time instants are in seconds since UNIX epoch, and all time durations are
 * in seconds.*
 *
 *
 * *Note: All time instants must be non-negative.*
 *
 *
 * Thread-safety: Instances of this class are immutable and are thus thread-safe.
 */
class TotpCounter @JvmOverloads constructor(timeStep: Long, startTime: Long = 0) {
    /**
     * Gets the frequency with which the value of this counter changes.
     *
     * @return interval of time (seconds) between successive changes of this counter's value.
     */
    /** Interval of time (seconds) between successive changes of this counter's value.  */
    val timeStep: Long
    /**
     * Gets the earliest time instant at which this counter assumes the value `0`.
     *
     * @return time (seconds since UNIX epoch).
     */
    /**
     * Earliest time instant (seconds since UNIX epoch) at which this counter assumes the value of
     * `0`.
     */
    val startTime: Long

    /**
     * Gets the value of this counter at the specified time.
     *
     * @param time time instant (seconds since UNIX epoch) for which to obtain the value.
     *
     * @return value of the counter at the `time`.
     */
    fun getValueAtTime(time: Long): Long {
        assertValidTime(time)

        // According to the RFC:
        // T = (Current Unix time - T0) / X, where the default floor function is used.
        //   T  - counter value,
        //   T0 - start time.
        //   X  - time step.

        // It's important to use a floor function instead of simple integer division. For example,
        // assuming a time step of 3:
        // Time since start time: -6 -5 -4 -3 -2 -1  0  1  2  3  4  5  6
        // Correct value:         -2 -2 -2 -1 -1 -1  0  0  0  1  1  1  2
        // Simple division / 3:   -2 -1 -1 -1  0  0  0  0  0  1  1  1  2
        //
        // To avoid using Math.floor which requires imprecise floating-point arithmetic, we
        // we compute the value using integer division, but using a different equation for
        // negative and non-negative time since start time.
        val timeSinceStartTime = time - startTime
        return if (timeSinceStartTime >= 0) {
            timeSinceStartTime / timeStep
        } else {
            (timeSinceStartTime - (timeStep - 1)) / timeStep
        }
    }

    /**
     * Gets the time when the counter assumes the specified value.
     *
     * @param value value.
     *
     * @return earliest time instant (seconds since UNIX epoch) when the counter assumes the value.
     */
    fun getValueStartTime(value: Long): Long {
        return startTime + value * timeStep
    }

    companion object {
        private fun assertValidTime(time: Long) {
            require(time >= 0) { "Negative time: $time" }
        }
    }
    /**
     * Constructs a new `TotpCounter` that starts with the value `0` at the specified
     * time and increments its value with the specified frequency.
     *
     * @param timeStep interval of time (seconds) between successive changes of this counter's value.
     * @param startTime the earliest time instant (seconds since UNIX epoch) at which this counter
     * assumes the value `0`.
     */
    /**
     * Constructs a new `TotpCounter` that starts with the value `0` at time instant
     * `0` (seconds since UNIX epoch) and increments its value with the specified frequency.
     *
     * @param timeStep interval of time (seconds) between successive changes of this counter's value.
     */
    init {
        require(timeStep >= 1) { "Time step must be positive: $timeStep" }
        assertValidTime(startTime)
        this.timeStep = timeStep
        this.startTime = startTime
    }
}