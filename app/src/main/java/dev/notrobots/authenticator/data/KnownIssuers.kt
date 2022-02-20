package dev.notrobots.authenticator.data

import dev.notrobots.authenticator.R

val KnownIssuers = mapOf(
    "(https://)?(www.)?github(.com)?" to R.drawable.ic_github,
    "(https://)?steamcommunity(.com)?" to R.drawable.ic_steam,
    "(https://)?store.steampowered(.com)?" to R.drawable.ic_steam,
    "(https://)?(www.)?discord(.com)?" to R.drawable.ic_discord,
    "(https://)?((www|mail|myaccount).)?google(.com)?" to R.drawable.ic_google,
    ".*" to R.drawable.ic_account
)