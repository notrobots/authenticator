package dev.notrobots.authenticator.ui.backupimportresult

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import dev.notrobots.authenticator.util.makeLiveData
import javax.inject.Inject

@HiltViewModel
class ImportResultViewModel @Inject constructor(
    val accountDao: AccountDao,
    val accountGroupDao: AccountGroupDao
) : ViewModel() {
    val accounts = makeLiveData<List<Account>> { accountDao.getAccounts() }
    val groups = makeLiveData<List<AccountGroup>> { accountGroupDao.getGroups() }
}