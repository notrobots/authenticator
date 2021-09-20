package dev.notrobots.authenticator.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.models.Account
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    val accountDao: AccountDao
) : ViewModel() {
    val accounts = accountDao.getAll().asLiveData()

    fun getAccount(index: Int): Account? {
        return accounts.value?.get(index)
    }
}