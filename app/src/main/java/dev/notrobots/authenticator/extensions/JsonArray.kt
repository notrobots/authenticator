package dev.notrobots.authenticator.extensions

import org.json.JSONArray
import org.json.JSONObject

fun <T> JSONArray.toList(): List<T> {
    return buildList {
        for (i in 0 until length()) {
            add(get(i))
        }
    }
}