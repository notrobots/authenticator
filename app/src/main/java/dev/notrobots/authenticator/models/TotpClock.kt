package dev.notrobots.authenticator.models

import android.content.SharedPreferences
import dev.notrobots.androidstuff.util.now
import dev.notrobots.preferences2.getTimeCorrection
import dev.notrobots.preferences2.putTimeCorrection

class TotpClock(
    private val preferences: SharedPreferences
) {
    private var cachedTimeCorrection: Int? = null
    private val lock = Any()

    fun nowMillis(): Long {
        return now() + (getTimeCorrection() * 1000 * 60)
    }

    fun getTimeCorrection(): Int {
        synchronized(lock) {
            if (cachedTimeCorrection == null) {
                cachedTimeCorrection = preferences.getTimeCorrection()
            }

            return cachedTimeCorrection!!
        }
    }

    fun setTimeCorrection(timeCorrection: Int) {
        synchronized(lock) {
            preferences.putTimeCorrection(timeCorrection)
            cachedTimeCorrection = null
        }
    }
}