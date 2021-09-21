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

/**
 * Clock input for the time-based OTPs (TOTP).
 *
 *
 * The input is based on the current system time and is adjusted by a persistently stored
 * correction value (offset in minutes).
 */
class TotpClock(context: Context?) : Clock {
    private val mSystemWallClock: Clock = SystemWallClock()
    private val mPreferences: SharedPreferences
    private val mLock = Any()

    /**
     * Cached value of time correction (in minutes) or `null` if not cached. The value is cached
     * because it's read very frequently (once every 100ms) and is modified very infrequently.
     *
     * @GuardedBy [.mLock]
     */
    private var mCachedCorrectionMinutes: Int? = null

    override fun nowMillis(): Long {
        return mSystemWallClock.nowMillis() + timeCorrectionMinutes * Utilities.MINUTE_IN_MILLIS
    }

    /**
     * Gets the currently used time correction value.
     *
     * @return number of minutes by which this device is behind the correct time.
     */// Invalidate the cache to force reading actual settings from time to time
    /**
     * Sets the currently used time correction value.
     *
     * @param minutes number of minutes by which this device is behind the correct time.
     */
    var timeCorrectionMinutes: Int = 0
        get() {
            synchronized(mLock) {
                if (mCachedCorrectionMinutes == null) {
                    mCachedCorrectionMinutes = mPreferences.getInt(PREFERENCE_KEY_OFFSET_MINUTES, 0)
                }
                return mCachedCorrectionMinutes!!
            }
        }

    /**
     * Gets the system "wall" clock on top of this this TOTP clock operates.
     */
    fun getSystemWallClock(): Clock {
        return mSystemWallClock
    }

    companion object {
        val PREFERENCE_KEY_OFFSET_MINUTES = "timeCorrectionMinutes"
    }

    init {
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    }
}