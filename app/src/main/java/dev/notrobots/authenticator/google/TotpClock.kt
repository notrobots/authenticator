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

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import java.util.concurrent.TimeUnit

/**
 * Clock input for the time-based OTPs (TOTP).
 *
 * The input is based on the current system time and is adjusted by a persistently stored
 * correction value (offset in minutes).
 */
class TotpClock(context: Context?) : Clock {
    private val mSystemWallClock: Clock = SystemWallClock()
    private val mPreferences: SharedPreferences
    private val mLock = Any()

    init {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }

    /**
     * Number of minutes by which this device is behind the correct time.
     *
     * Invalidate the cache to force reading actual settings from time to time.
     */
    var timeCorrectionMinutes: Long? = null
        set(value) {
            synchronized (mLock) {
                mPreferences.edit().putLong(PREFERENCE_OFFSET_MINUTES, value ?: 0).commit()
                field = null
            }
        }
        get() {
            synchronized(mLock) {
                if (field == null) {
                    field = mPreferences.getLong(PREFERENCE_OFFSET_MINUTES, 0)
                }
                return field!!
            }
        }

    override fun nowMillis(): Long {
        return mSystemWallClock.nowMillis() + TimeUnit.MINUTES.toMillis(timeCorrectionMinutes!!)
    }

    /**
     * Gets the system "wall" clock on top of this this TOTP clock operates.
     */
    fun getSystemWallClock(): Clock {
        return mSystemWallClock
    }

    companion object {
        const val PREFERENCE_OFFSET_MINUTES = "time_correction_minutes"
    }
}