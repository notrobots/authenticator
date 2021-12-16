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
     * Inserts the given [account] into the database and takes care of the group ordering and groupId
     */
    suspend fun addAccount(account: Account, forceReplace: Boolean = false) {
        if (checkIfAccountExists(account) && !forceReplace) {   //TODO: Check should be done outside of here and this should overwrite by default
            loge("An account with the same name already exists")
            throw Exception("An account with the same name already exists")
        } else {
            val last = accountDao.getLargestOrder(account.groupId)

            if (accountGroupDao.getGroup(account.groupId) == null) {
                account.groupId = Account.DEFAULT_GROUP_ID
            }

            account.order = last + 1
            accountDao.insert(account)
            logd("Adding new account")
        }
    }

    suspend fun updateAccount(account: Account) {
        val original = accountDao.getAccount(account.name, account.label, account.issuer)

        if (original != null) {
            if (accountGroupDao.getGroup(account.groupId) == null) {
                account.groupId = Account.DEFAULT_GROUP_ID
            }

            if (original.groupId != account.groupId) {
                val last = accountDao.getLargestOrder(account.groupId)

                account.order = last + 1
            }

            account.id = original.id

            accountDao.update(account)
            logd("Updating account")
        } else {
            logd("Cannot update account: Not found")
        }
    }

    /**
     * Checks if the given [account] exists and throws an exception if an account with the same
     * name, label and issuer is found.
     *
     * If [overwrite] is true it will update the existing account
     */
    suspend fun updateAccount(account: Account, overwrite: Boolean) {
        val exists = accountDao.getCount(account.name, account.label, account.issuer) > 0   //FIXME: Do not check for existence here

        if (exists && !overwrite) {
            throw Exception("An account with the same name and issuer already exists")
        } else {
            val original = accountDao.getAccount(account.id)

            if (original.groupId != account.groupId) {
                val last = accountDao.getLargestOrder(account.groupId)

                account.order = last + 1
            }

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
    suspend fun addGroup(group: AccountGroup, forceReplace: Boolean = false) {
        if (checkIfGroupExists(group) && !forceReplace) {
            loge("A group with the same name already exists")
            throw Exception("A group with the same name already exists")
        } else {
            val last = accountGroupDao.getLastOrder()

            group.order = last + 1
            accountGroupDao.insert(group)
            logd("Adding new group")
        }
    }

    suspend fun updateGroup(group: AccountGroup) {
        val original = accountGroupDao.getGroup(group.name)

        if (original != null) {
            group.id = original.id
            accountGroupDao.update(group)
            logd("Updating group")
        } else {
            logd("Cannot update group: Not found")
        }
    }
}