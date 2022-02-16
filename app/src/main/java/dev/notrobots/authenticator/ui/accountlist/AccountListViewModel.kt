package dev.notrobots.authenticator.ui.accountlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.util.makeLiveData
import kotlinx.coroutines.flow.asFlow
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    val accountDao: AccountDao
) : ViewModel() {
    val accounts = accountDao.getAccountsLive()

    /**
     * Inserts the given [account] into the database and takes care of the ordering.
     */
    suspend fun insertAccount(account: Account) {
        val last = accountDao.getLargestOrder()

        account.order = last + 1
        accountDao.insert(account)
        logd("Adding new account")
    }

    /**
     * Updates the given [account].
     */
    suspend fun updateAccount(account: Account) {
        val original = accountDao.getAccount(account.name, account.label, account.issuer)

        if (original != null) {
            account.id = original.id
            accountDao.update(account)
            logd("Updating account")
        } else {
            logd("Cannot update account: Not found")
        }
    }
}