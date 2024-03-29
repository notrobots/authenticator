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
package dev.notrobots.authenticator.models

import dev.notrobots.androidstuff.util.now
import java.util.concurrent.TimeUnit

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
class TotpCounter(
    /**
     * Interval of time (seconds) between successive changes of this counter's value.
     */
    val timeStep: Long
) {
    init {
        require(timeStep >= 1) { "Time step must be positive: $timeStep" }
    }

    /**
     * Gets the counter at the specified time.
     *
     * @param time time instant for which to obtain the value.
     * @param unit time unit of [time] ([TimeUnit.MILLISECONDS] by default).
     *
     * @return counter at `time`.
     */
    fun getCounterAtTime(time: Long, unit: TimeUnit = TimeUnit.MILLISECONDS): Long {
        val time = unit.toSeconds(time)

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
        return if (time >= 0) {
            time / timeStep
        } else {
            (time - (timeStep - 1)) / timeStep
        }
    }

    /**
     * Gets the start time for the specified counter value.
     *
     * @param counter value.
     *
     * @return earliest time instant (seconds since UNIX epoch) for the given counter value.
     */
    fun getCounterStartTime(counter: Long): Long {
        return counter * timeStep
    }

    /**
     * Gets the time (seconds since UNIX epoch) until the next counter value.
     *
     * @param time time instant  for which to perform the query.
     * @param inputUnit time unit of [time].
     *
     * @return time left (in milliseconds since epoch) until the next counter value .
     */
    fun getTimeUntilNextCounter(
        time: Long = now(),
        inputUnit: TimeUnit = TimeUnit.MILLISECONDS
    ): Long {
        val current = getCounterAtTime(time, inputUnit)
        val next = current + 1
        val nextStart = TimeUnit.SECONDS.toMillis(getCounterStartTime(next))

        return nextStart - inputUnit.toMillis(time)
    }

    companion object {
        private fun assertValidTime(time: Long) {
            require(time >= 0) { "Negative time: $time" }
        }
    }
}