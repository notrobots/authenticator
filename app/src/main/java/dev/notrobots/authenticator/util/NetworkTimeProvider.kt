package dev.notrobots.authenticator.util

import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.data.MINUTE_IN_MILLIS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.math.round

object NetworkTimeProvider {
    private val logger = Logger(this)
    private val sources = mapOf<String, (conn: HttpURLConnection) -> Long>(
        "https://www.google.com" to {
            it.getHeaderFieldDate("Date", 0)
        },

        // If Google is down it either means there are network issues or the world is over.
        // All the backup Urls are here just because.
        "https://time.is/" to {
            it.getHeaderFieldDate("Date", 0)
        },
        "https://www.wikipedia.com" to {
            it.getHeaderFieldDate("date", 0)
        }
    )

    /**
     * Fetches the time from one of the specified sources and invokes [onSuccess] if one of the sources URL was available and
     * returned a valid time value or [onFailure] if none of the sources were available.
     */
    fun getTimeCorrection(
        onSuccess: (timeCorrection: Int) -> Unit,
        onFailure: () -> Unit
    ) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        val uiScope = CoroutineScope(Dispatchers.Main)

        coroutineScope.launch {
            for ((url, getTime) in sources) {
                try {
                    with(URL(url).openConnection() as HttpURLConnection) {
                        requestMethod = "HEAD"

                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            val time = getTime(this).toDouble()
                            val correction = round((time - System.currentTimeMillis()) / MINUTE_IN_MILLIS).toInt()

                            if (time > 0) {
                                uiScope.launch {
                                    onSuccess(correction)
                                }
                                return@launch
                            } else {
                                logger.logw("$url: Cannot get date header")
                            }
                        } else {
                            logger.logw("$url: $responseCode")
                        }
                    }
                } catch (e: Exception) {
                    logger.logw("Cannot fetch time from $url", e)
                }
            }

            logger.loge("Cannot fetch any of the provided URLs")

            uiScope.launch {
                onFailure()
            }
        }
    }
}