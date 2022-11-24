package dev.notrobots.authenticator.data

import dev.notrobots.authenticator.R

object KnownIssuers {
    val list = mapOf(
        "(https://)?(www.)?github(.com)?" to R.drawable.ic_github,
        "(https://)?steamcommunity(.com)?" to R.drawable.ic_steam,
        "(https://)?store.steampowered(.com)?" to R.drawable.ic_steam,
        "(https://)?(www.)?discord(.com)?" to R.drawable.ic_discord,
        "(https://)?((www|mail|myaccount).)?google(.com)?" to R.drawable.ic_google
    ).mapKeys {
        it.key.toRegex(RegexOption.IGNORE_CASE)
    }

    /**
     * Returns the icon resources if any of the known issuers matches the given [input] or 0 if no match is found.
     */
    fun lookup(input: CharSequence, default: Int = R.drawable.ic_account): Int {
        return list.entries.find {
            it.key.matches(input)
        }?.value ?: default
    }
}