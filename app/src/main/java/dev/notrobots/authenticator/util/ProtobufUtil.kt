package dev.notrobots.authenticator.util

import android.net.Uri
import com.google.protobuf.ByteString
import com.google.protobuf.Internal
import com.google.protobuf.MessageLite
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType

import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Base64

object ProtobufUtil {
    /**
     * Serializes the given message into a base64 string.
     */
    fun serializeMessage(message: MessageLite): String {
        val base64 = Base64().encode(message.toByteArray())
        val string = base64.toString(Charsets.UTF_8)

        return Uri.encode(string)
    }

    /**
     * Deserializes the given base64 string into a ByteArray.
     *
     * The returned value can be used to create a {MessageLite} object.
     */
    fun deserializeMessage(string: String): ByteArray {
        val base64 = Uri.decode(string).toByteArray(Charsets.UTF_8)

        return Base64().decode(base64)
    }

    fun serializeSecret(secret: String): ByteString {
        val bytes = Base32().decode(secret)

        return ByteString.copyFrom(bytes)
    }

    fun deserializeSecret(byteString: ByteString): String {
        val bytes = Base32().encode(byteString.toByteArray())

        return bytes.toString(Charsets.UTF_8)
    }

    /**
     * Returns the length of the given [message].
     */
    fun getMessageLength(message: MessageLite): Int {
        return 4 * (message.serializedSize / 3)
    }

    /**
     * Returns the length of the given [messages].
     */
    fun getMessageLength(messages: Iterable<MessageLite>): Int {
        return messages.sumOf { getMessageLength(it) }
    }
}