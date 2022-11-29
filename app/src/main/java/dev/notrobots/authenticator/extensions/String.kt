package dev.notrobots.authenticator.extensions

import com.google.protobuf.ByteString

internal fun String.toByteString(): ByteString {
    return ByteString.copyFrom(toByteArray())
}