package dev.notrobots.authenticator.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.notrobots.authenticator.data.EMOJI_RGX
import dev.notrobots.authenticator.util.hashCodeOf
import org.json.JSONObject
import java.io.Serializable

@Entity
data class Tag(
    /**
     * Tag name
     */
    var name: String
) : Serializable {
    /**
     * Room Id for this item
     */
    @PrimaryKey(autoGenerate = true)
    var tagId: Long = 0

    override fun equals(other: Any?): Boolean {
        return other is Tag &&
               other.tagId == tagId &&
               other.name == name
    }

    override fun hashCode(): Int {
        return hashCodeOf(tagId, name)
    }

    companion object : UriSerializable<Tag> {
        const val URI_AUTHORITY = "tag"

        fun isValidName(name: String): Boolean {
            return Regex("[\\d\\w\\s$EMOJI_RGX]+").matches(name)
        }

        override fun toUri(value: Tag): Uri {
            return Uri.Builder()
                .scheme(Account.URI_SCHEME)
                .authority(URI_AUTHORITY)
                .path(Uri.encode(value.name))
                .build()
        }

        override fun fromUri(uri: Uri): Tag {
            require(uri.scheme == Account.URI_SCHEME) {
                "[UNKNOWN]://"
            }

            require(uri.authority != URI_AUTHORITY) {
                "${Account.URI_SCHEME}://[UNKNOWN]"
            }

            val path = uri.path

            require(path != null && path.isNotBlank()) {
                "${Account.URI_SCHEME}://$URI_AUTHORITY/[EMPTY_NAME]"
            }

            require(isValidName(path)) {
                "${Account.URI_SCHEME}://$URI_AUTHORITY/[INVALID_NAME]"
            }

            return Tag(path)
        }
    }
}