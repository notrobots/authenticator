package dev.notrobots.authenticator.data

import dev.notrobots.authenticator.R

val KnownIssuers = mapOf(
    "(https://)?(www.)?github(.com)?" to R.drawable.ic_palette,
    "(https://)?(www.)?twitter(.com)?" to R.drawable.ic_add,
    "(https://)?(www.)?twitter(.com)?" to R.drawable.ic_delete,
    ".*" to R.drawable.ic_save
)