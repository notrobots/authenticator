package dev.notrobots.authenticator.models

import androidx.room.Entity
import androidx.room.PrimaryKey
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
}