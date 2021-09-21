package dev.notrobots.authenticator.data

import dev.notrobots.authenticator.R

val KnownIssuers = mapOf(
    Regex(
        "(https://)?(www.)?github(.com)?",
        RegexOption.IGNORE_CASE
    ) to R.drawable.ic_launcher_background,
    Regex(
        "(https://)?(www.)?twitter(.com)?",
        RegexOption.IGNORE_CASE
    ) to R.drawable.ic_launcher_background
)