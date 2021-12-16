package dev.notrobots.authenticator.ui.importresult

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import javax.inject.Inject

@HiltViewModel
class ImportResultViewModel @Inject constructor(
    val accountDao: AccountDao,
    val accountGroupDao: AccountGroupDao
) : ViewModel() {
    val accounts = accountDao.getAccounts()
    val groups = accountGroupDao.getGroups()
}