package dev.notrobots.authenticator.models

import org.json.JSONObject

interface JsonSerializable<T> {
    fun toJson(value: T): JSONObject

    fun fromJson(json: JSONObject): T

    fun fromJson(json: String): T {
        return fromJson(JSONObject(json))
    }
}