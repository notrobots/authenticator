package dev.notrobots.authenticator.ui.accountlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.models.Account
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountListViewModel @Inject constructor(
    val accountDao: AccountDao
) : ViewModel() {
    val accounts = accountDao.getAll()
}