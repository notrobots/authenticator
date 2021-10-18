package dev.notrobots.authenticator.ui.account

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    val accountDao: AccountDao,
    val accountGroupDao: AccountGroupDao
) : ViewModel() {
    val groups = accountGroupDao.getGroups()

    suspend fun updateAccount(account: Account) {
        val exists = accountDao.getCount(account.name, account.issuer) > 0

        if (exists) {
            error("An account with the same name and issuer already exists")
        } else {
            accountDao.update(account)
            logd("Updating account")
        }
    }

    suspend fun addAccount(account: Account) {
        val exists = accountDao.getCount(account.name, account.issuer) > 0

        if (exists) {
            error("An account with the same name and issuer already exists")
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
}