///*
// * Copyright 2019 Google LLC
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package dev.notrobots.authenticator.time
//
///**
// * Constructs a new `TOTPCounter` that starts with the value `0` at the specified
// * time and increments its value with the specified frequency.
// *
// * @param timeStep interval of time (seconds) between successive changes of this counter's value.
// * @param startTime the earliest time instant (seconds since UNIX epoch) at which this counter assumes the value `0`.
// */
//class TOTPCounter(
//    /**
//     * Interval of time (seconds) between successive changes of this counter's value.
//     */
//    private val timeStep: Long,
//    /**
//     * Earliest time instant (seconds since UNIX epoch) at which this counter assumes the value of `0`.
//     */
//    private val startTime: Long
//) {
//    init {
//        require(timeStep >= 1) { "Time step must be positive: $timeStep" }
//    }
//
//    /**
//     * Gets the value of this counter at the specified time.
//     *
//     * @param time time instant (seconds since UNIX epoch) for which to obtain the value.
//     * @return value of the counter at the `time`.
//     */
//    fun getValueAtTime(time: Long): Long {
//        val timeSinceStartTime = time - startTime
//
//        return if (timeSinceStartTime >= 0) {
//            timeSinceStartTime / timeStep
//        } else {
//            (timeSinceStartTime - (timeStep - 1)) / timeStep
//        }
//    }
//
//    /**
//     * Gets the time when the counter assumes the specified value.
//     *
//     * @param value value.
//     * @return earliest time instant (seconds since UNIX epoch) when the counter assumes the value.
//     */
//    fun getValueStartTime(value: Long): Long {
//        return startTime + value * timeStep
//    }
//}