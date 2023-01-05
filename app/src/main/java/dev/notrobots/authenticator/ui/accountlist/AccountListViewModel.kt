package dev.notrobots.authenticator.ui.accountlist

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.androidstuff.util.Logger.Companion.logd
import dev.notrobots.androidstuff.util.Logger.Companion.loge
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountTagCrossRefDao
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.CombinedMediatorLiveData
import dev.notrobots.authenticator.models.SortMode
import dev.notrobots.authenticator.models.Tag
import dev.notrobots.authenticator.util.TextUtil
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    val accountDao: AccountDao,
    val tagDao: TagDao,
    val accountTagCrossRefDao: AccountTagCrossRefDao
) : ViewModel() {
    val sortMode = MutableLiveData(SortMode.Custom)
    val tagIdFilter: MutableLiveData<Long> = MutableLiveData(-1)
    val tags = tagDao.getTagsLive()
    val accounts = CombinedMediatorLiveData(sortMode, tagIdFilter) {
        AccountFilter(it[0] as SortMode, it[1] as Long?)
    }.switchMap {
        when {
            it.tagId != null && it?.tagId != -1L -> accountDao.getAccountsLive(
                it.sortMode.sortingDirection,
                it.sortMode.sortingBy,
                it.tagId
            )
            it.sortMode == SortMode.Custom -> accountDao.getAccountsLive()

            else -> accountDao.getAccountsLive(
                it.sortMode.sortingDirection,
                it.sortMode.sortingBy
            )
        }
    }
    val isFilterActive: MediatorLiveData<Boolean> = FilterActiveMediator(tagIdFilter, tags)

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
        // Since we are updating the account using a dummy account with the new values
        // We might need to fetch the corresponding account using name, label and issuer
        // We then set the id of the account we want to update to our dummy account and pass that
        // to the update function
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
     *
     * @return The id of the inserted account.
     */
    suspend fun insertAccountWithSameName(account: Account): Long {
        val lastSimilarName = accountDao.getSimilarNames("${account.name}%").maxOf { it }
        val match = SIMILAR_NAME_RGX.find(lastSimilarName)
        val name = if (match == null) {
            "$lastSimilarName 1"
        } else {
            val value = match.groupValues[1].toInt()

            lastSimilarName.replace(SIMILAR_NAME_RGX, " ${value + 1}")
        }

        val newAccount = account.clone().apply {
            this.name = name
        }

        return insertAccount(newAccount)
    }

    private class FilterActiveMediator(tagId: LiveData<Long>, tags: LiveData<List<Tag>>) : MediatorLiveData<Boolean>() {
        init {
            addSource(tagId) { value = it != -1L && tags.value?.isNotEmpty() == true }
            addSource(tags) { value = tagId.value != -1L && it.isNotEmpty() }
        }
    }

    private data class AccountFilter(
        val sortMode: SortMode,
        val tagId: Long?
    )

    companion object {
        private val SIMILAR_NAME_RGX = Regex("\\s(\\d+)$")
    }
}