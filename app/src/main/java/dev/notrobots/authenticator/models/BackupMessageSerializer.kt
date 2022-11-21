package dev.notrobots.authenticator.models

import com.google.protobuf.MessageLite
import dev.notrobots.authenticator.util.BackupData
import dev.notrobots.authenticator.util.ProtobufUtil

/**
 * Base class for serializing backups using protocol buffers.
 */
abstract class BackupMessageSerializer {
    /**
     * Serializes the given data into a list of base64 strings using protocol buffers.
     *
     * This method needs to take care of parsing the data into a [MessageLite] object,
     * the message object will then be encoded to base64 and finally URL encoded,
     * these two operation can be done by using [ProtobufUtil.serializeMessage].
     *
     * This method will return at least one item.
     * The amount of items depends on the value of [maxBytes] and how big each message is.
     */
    abstract fun serialize(
        accounts: List<Account>,
        accountsWithTags: List<AccountWithTags>,
        tags: List<Tag>,
        maxBytes: Int = 0
    ): List<String>

    /**
     * Deserializes the given base64 string into a BackupData object.
     *
     * This method needs to take care of parsing the [MessageLite] into the desired output.
     *
     * The [MessageLite] object is obtained by URL decoding and then base64 decoding the input,
     * these operations can be done by using [ProtobufUtil.deserializeMessage].
     */
    abstract fun deserialize(data: String): BackupData
}