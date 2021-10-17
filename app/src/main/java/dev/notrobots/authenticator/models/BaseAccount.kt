package dev.notrobots.authenticator.models

import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

abstract class BaseAccount(
    /**
     * Displayed name for this item
     */
    var name: String,
) : Serializable {
    /**
     * Room Id for this item
     */
    @PrimaryKey(autoGenerate = true)
    var id: Long = DEFAULT_ID

    /**
     * Whether or not this item is selected
     */
    @Ignore
    var isSelected: Boolean = false

    /**
     * Position of this item in the list
     *
     * The default value is -1, which puts the item at the end of the list
     */
    var order: Long = DEFAULT_ORDER

    fun toggleSelected() {
        isSelected = !isSelected
    }

    companion object {
        const val DEFAULT_ORDER = -1L
        const val DEFAULT_ID = 0L
    }
}