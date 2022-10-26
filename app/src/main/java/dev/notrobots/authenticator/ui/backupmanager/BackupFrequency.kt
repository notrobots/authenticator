package dev.notrobots.authenticator.ui.backupmanager

import org.json.JSONObject

data class BackupFrequency(
    val days: Int = 0
) {
    fun toJson(): JSONObject {
        return JSONObject(
            mapOf(
                "days" to days
            )
        )
    }

    companion object {
        fun fromJson(json: String): BackupFrequency {
            val obj = JSONObject(json)

            return BackupFrequency(
                obj.getInt("days")
            )
        }
    }
}