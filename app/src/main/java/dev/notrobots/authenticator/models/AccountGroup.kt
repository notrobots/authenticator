package dev.notrobots.authenticator.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.notrobots.authenticator.ui.accountlist.AccountListItem

//@Entity
class AccountGroup(
    /**
     * Name for this group
     */
    var name: String
) : AccountListItem {
    /**
     * Id for this group
     */
//    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

//    /**
//     * Position of this account in the account list
//     */
//    var order: Long = -1

    /**
     * Expanded state of this group
     */
    var isExpanded: Boolean = true

    /**
     * Accounts belonging to this group
     */
    var accounts: MutableList<Account> = mutableListOf()

    operator fun get(position: Int): Account {
        return accounts[position]
    }
}