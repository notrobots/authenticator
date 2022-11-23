package dev.notrobots.authenticator.data

const val EMOJI_RGX = "(\\u00a9|\\u00ae|[\\u2000-\\u3300]|\\ud83c[\\ud000-\\udfff]|\\ud83d[\\ud000-\\udfff]|\\ud83e[\\ud000-\\udfff])"

const val GOOGLE_AUTHENTICATOR_PROTO_VERSION = 1

// Fields max lengths
const val TAG_NAME_MAX_LENGTH = 40
const val ACCOUNT_NAME_MAX_LENGTH = 60
const val ACCOUNT_LABEL_MAX_LENGTH = 60
const val ACCOUNT_ISSUER_MAX_LENGTH = 60
