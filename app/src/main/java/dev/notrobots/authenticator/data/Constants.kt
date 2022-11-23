package dev.notrobots.authenticator.data

const val EMOJI_RGX = "(\\u00a9|\\u00ae|[\\u2000-\\u3300]|\\ud83c[\\ud000-\\udfff]|\\ud83d[\\ud000-\\udfff]|\\ud83e[\\ud000-\\udfff])"

const val GOOGLE_AUTHENTICATOR_PROTO_VERSION = 1

const val TAG_NAME_MAX_LENGTH = 40
val TAG_NAME_INVALID_CHARACTERS = Regex("['\".,:/\\\\]")