package dev.notrobots.authenticator.ui.accountlist

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.androidstuff.util.loge
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    val accountDao: AccountDao,
    val accountGroupDao: AccountGroupDao
) : ViewModel() {
    val groupsWithAccount = accountGroupDao.getGroupsWithAccounts()
    val accounts = accountDao.getAccounts()
    val groups = accountGroupDao.getGroups()

    /**
     * Checks if the given [account] already exists.
     */
    suspend fun checkIfAccountExists(account: Account): Boolean {
        val count = accountDao.getCount(account.name, account.label, account.issuer)

        return count > 0
    }

    /**
     * Checks if the given [group] already exists.
     */
    suspend fun checkIfGroupExists(group: AccountGroup): Boolean {
        return checkIfGroupExists(group.name)
    }

    /**
     * Checks if a group with the given [name] already exists.
     */
    suspend fun checkIfGroupExists(name: String): Boolean {
        val count = accountGroupDao.getCount(name)

        return count > 0
    }

    /**
     * Inserts the given [account] into the database and takes care of the group ordering.
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
            val last = accountDao.getLargestOrder(account.groupId)

            account.order = last + 1
            accountDao.insert(account)
            logd("Adding new account")
        }
    }

    /**
     * Checks if the given [account] exists and throws an exception if an account with the same
     * name, label and issuer is found.
     *
     * If [overwrite] is true it will update the existing account
     */
    suspend fun updateAccount(account: Account, overwrite: Boolean) {
        val exists = accountDao.getCount(account.name, account.label, account.issuer) > 0

        if (exists && !overwrite) {
            error("An account with the same name and issuer already exists")
        } else {
            val last = accountDao.getLargestOrder(account.groupId)

            account.order = last + 1
            accountDao.update(account)
            logd("Updating account")
        }
    }

    /**
     * Inserts the given [group] into the database and takes care of the group ordering.
     *
     * To bypass the group ordering, call [AccountGroupDao.insert] directly.
     *
     * If the group already exists an exception is thrown.
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