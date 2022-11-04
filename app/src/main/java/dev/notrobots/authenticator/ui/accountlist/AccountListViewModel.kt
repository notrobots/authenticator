package dev.notrobots.authenticator.ui.accountlist

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.androidstuff.util.loge
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.SortMode
import dev.notrobots.authenticator.models.Tag
import dev.notrobots.authenticator.util.TextUtil
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    val accountDao: AccountDao,
    val tagDao: TagDao
) : ViewModel() {
    val sortMode = MutableLiveData(SortMode.Custom)
    val tagIdFilter: MutableLiveData<Long> = MutableLiveData(-1)
    val tags = tagDao.getTagsLive()
    val accounts = accountDao.getAccountsLive()
    val filteredAccounts = Transformations.switchMap(AccountFilterMediator(sortMode, tagIdFilter)) {
        if (it?.second != -1L && it.first != null) {
            accountDao.getAccountsWithTagsLive(
                it.first!!.sortingDirection,
                it.first!!.sortingBy,
                it.second!!
            )
        } else {
            accountDao.getAccountsWithTagsLive(
                it.first!!.sortingDirection,
                it.first!!.sortingBy
            )
        }
    }

    /**
     * Inserts the given [account] into the database and takes care of the ordering.
     */
    suspend fun insertAccount(account: Account): Long { //TODO: Incorporate these methods in the DAO if possible
        val last = accountDao.getLargestOrder()

        account.order = last + 1
        logd("Adding new account")
        return accountDao.insert(account)
    }

    /**
     * Updates the given [account].
     */
    suspend fun updateAccount(account: Account) {
        val stored = accountDao.getAccount(account.name, account.label, account.issuer)

        if (account.accountId == Account.DEFAULT_ID) {
            if (stored != null) {
                account.accountId = stored.accountId
            } else {
                loge("Cannot update id: Account not found")
                return
            }
        }

        if (account.order == Account.DEFAULT_ORDER) {
            if (stored != null) {
                account.order = stored.order
            } else {
                loge("Cannot update order: Account not found")
                return
            }
        }

        accountDao.update(account)
        logd("Updating account")
    }

    /**
     * Inserts the given [account] and changes its name so that it doesn't collide with
     * another existing account.
     */
    suspend fun insertAccountWithSameName(account: Account) {
        var name = TextUtil.getNextName(account.name)

        //FIXME: This is not optimized
        //TODO: Handle max value too
        do {
            val oldAccount = accountDao.getAccount(name, account.label, account.issuer)

            if (oldAccount == null) {
                val newAccount = account.clone().apply {  //FIXME: If the account is imported it doesn't need to be copied
                    this.name = name
                }
                insertAccount(newAccount)
                break
            }

            name = TextUtil.getNextName(oldAccount.name)
        } while (true)
    }

    private class AccountFilterMediator(sortMode: LiveData<SortMode>, tagId: LiveData<Long?>) : MediatorLiveData<Pair<SortMode?, Long?>>() {
        init {
            addSource(sortMode) { value = it to tagId.value }
            addSource(tagId) { value = sortMode.value to it }
        }
    }
}