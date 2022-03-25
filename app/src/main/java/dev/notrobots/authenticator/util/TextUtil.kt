package dev.notrobots.authenticator.util

object TextUtil {
    fun getNextName(name: String): String {
        val rgx = Regex("\\s\\d+\$")
        val match = rgx.find(name)

        if (match != null) {
            val value = match.value.trim().toLong()

            return name.replace(rgx, " ${value + 1}")
        } else {
            return "$name 1"
        }
    }
}