package dev.notrobots.authenticator.models

import androidx.room.Entity
import java.io.Serializable

@Entity
class AccountGroup(
    name: String
) : BaseAccount(name), Serializable {
    /**
     * Expanded state of this group
     */
    var isExpanded: Boolean = true

    /**
     * Whether or not this is a default group
     *
     * A default group is not shown like a regular group and there must be
     * only one default group which is also automatically created
     */
    var isDefault: Boolean = false

    /**
     * Accounts belonging to this group
     */
//    var accounts: MutableList<Account> = mutableListOf()

//    operator fun get(position: Int): Account {
//        return accounts[position]
//    }

    companion object {
        val DEFAULT_GROUP = AccountGroup("").apply {
            id = Account.DEFAULT_GROUP_ID
            order = DEFAULT_ORDER
            isDefault = true
        }
    }
}