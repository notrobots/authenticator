package dev.notrobots.authenticator.ui.accountlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.models.Account
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
}