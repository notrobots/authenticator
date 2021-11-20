package dev.notrobots.authenticator.models

import androidx.room.Embedded
import androidx.room.Relation
import java.io.Serializable

data class GroupWithAccounts(
    @Embedded
    val group: AccountGroup,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val accounts: MutableList<Account>
) : Serializable {
    val isEmpty
        get() = accounts.isEmpty()
}