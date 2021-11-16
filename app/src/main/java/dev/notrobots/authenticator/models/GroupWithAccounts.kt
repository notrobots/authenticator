package dev.notrobots.authenticator.models

import androidx.room.Embedded
import androidx.room.Relation

data class GroupWithAccounts(
    @Embedded
    val group: AccountGroup,
    @Relation(parentColumn = "id", entityColumn = "groupId")
    val accounts: MutableList<Account>
) {
    val isEmpty
        get() = accounts.isEmpty()
}