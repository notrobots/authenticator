package dev.notrobots.authenticator.ui.backup

import android.os.Bundle
import android.view.MenuItem
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.ui.backupexport.ExportActivity
import dev.notrobots.authenticator.ui.backupexport.ExportResultActivity
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import kotlinx.android.synthetic.main.activity_backup.*
import javax.inject.Inject

@AndroidEntryPoint
class BackupActivity : ThemedActivity() {
    @Inject
    lateinit var accountGroupDao: AccountGroupDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup)

        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        btn_backup_export.setOnClickListener {
            accountGroupDao.getGroupsWithAccounts().observe(this) {
                val groups = ArrayList(it.map { it.group })
                val accounts = ArrayList(it.flatMap { it.accounts })

                startActivity(ExportActivity::class) {
                    putExtra(ExportActivity.EXTRA_GROUP_LIST, groups)
                    putExtra(ExportActivity.EXTRA_ACCOUNT_LIST, accounts)
                }
            }
        }
        btn_backup_import.setOnClickListener {
            startActivity(ImportActivity::class)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> false
        }
    }
}