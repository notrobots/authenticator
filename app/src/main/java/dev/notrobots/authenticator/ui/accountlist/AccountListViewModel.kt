package dev.notrobots.authenticator.ui.accountlist

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.SortMode
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    val accountDao: AccountDao
) : ViewModel() {
    val sortMode = MutableLiveData(SortMode.Custom)
    val accounts = sortMode.switchMap {
        when (it) {
            SortMode.Custom -> accountDao.getAccountsLive()
            SortMode.NameAscending -> accountDao.getAccountsOrderedByName(0)
            SortMode.NameDescending -> accountDao.getAccountsOrderedByName(1)
            SortMode.LabelAscending -> accountDao.getAccountsOrderedByLabel(0)
            SortMode.LabelDescending -> accountDao.getAccountsOrderedByLabel(1)
            SortMode.IssuerAscending -> accountDao.getAccountsOrderedByIssuer(0)
            SortMode.IssuerDescending -> accountDao.getAccountsOrderedByIssuer(1)
            SortMode.TagAscending -> TODO()
            SortMode.TagDescending -> TODO()
        }
    }

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
            account.order = original.order
            accountDao.update(account)
            logd("Updating account")
        } else {
            logd("Cannot update account: Not found")
        }
    }
}