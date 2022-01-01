package dev.notrobots.authenticator.ui.backup

import android.os.Bundle
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.ui.backupexport.ExportActivity
import dev.notrobots.authenticator.ui.backupexportconfig.ExportConfigActivity
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import kotlinx.android.synthetic.main.activity_backup.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BackupActivity : ThemedActivity() {
    @Inject
    lateinit var accountDao: AccountDao

    @Inject
    lateinit var accountGroupDao: AccountGroupDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btn_backup_export.setOnClickListener {
            // With selection
            // startActivity(ExportActivity::class)

            // Without selection
            lifecycleScope.launch {
                val groups = accountGroupDao.getGroups()
                val accounts = accountDao.getAccounts()
                val items = ArrayList(groups + accounts)

                startActivity(ExportConfigActivity::class) {
                    putExtra(ExportConfigActivity.EXTRA_ITEMS, items)
                }
            }
        }
        btn_backup_import.setOnClickListener {
            startActivity(ImportActivity::class)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return false
    }
}