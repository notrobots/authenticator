package dev.notrobots.authenticator.ui.accountlist

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.androidstuff.util.loge
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.dialogs.ReplaceAccountDialog
import dev.notrobots.authenticator.extensions.toUri
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    val accountDao: AccountDao,
    val accountGroupDao: AccountGroupDao
) : ViewModel() {
    val accounts = accountDao.getAll()
    val groups = accountGroupDao.getGroups()
    val groupsWithAccount = accountGroupDao.getGroupsWithAccounts()

    /**
     * Checks if the given [account] already exists and then invokes the given [block] with the result
     *
     * The [block] is invoked inside of a coroutine
     */
    suspend fun checkIfAccountExists(account: Account): Boolean {
        val count = accountDao.getCount(account.name, account.issuer)

        return count > 0
    }

    /**
     * Checks if the given [group] already exists and then invokes the given [block] with the result
     *
     * The [block] is invoked inside of a coroutine
     */
    suspend fun checkIfGroupExists(group: AccountGroup): Boolean {
        return checkIfGroupExists(group.name)
    }

    /**
     * Checks if a group with the given [name] already exists and then invokes the given [block] with the result
     *
     * The [block] is invoked inside of a coroutine
     */
    suspend fun checkIfGroupExists(name: String): Boolean {
        val count = accountGroupDao.getCount(name)

        return count > 0
    }

    /**
     * Inserts the given [account] into the database.
     *
     * In case the account (name & issuer) already exists, the user is prompt with a
     * dialog asking them if they want to overwrite the existing account.
     *
     * This method **will not** throw any exceptions.
     */
    suspend fun addAccount(account: Account) {
        if (checkIfAccountExists(account)) {
            loge("An account with the same name already exists")
            error("An account with the same name already exists")
        } else {
            val last = accountDao.getLastOrder()
            val defaultGroupCreated = accountGroupDao.isNotEmpty() > 0

            if (!defaultGroupCreated) {
                accountGroupDao.insert(AccountGroup.DEFAULT_GROUP)
            }

            account.order = last + 1
            accountDao.insert(account)
            logd("Adding new account")
        }
    }

    /**
     * Inserts the given [group] into the database
     *
     * If the group already exists an exception is thrown
     */
    suspend fun addGroup(group: AccountGroup) {
        if (checkIfGroupExists(group)) {
            loge("A group with the same name already exists")
            error("A group with the same name already exists")
        } else {
            val last = accountGroupDao.getLastOrder()

            group.order = last + 1
            accountGroupDao.insert(group)
            logd("Adding new group")
        }
    }

}