package dev.notrobots.authenticator.ui.backup

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.makeSnackBar
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityBackupBinding
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.ui.backupexportconfig.ExportConfigActivity
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BackupActivity : ThemedActivity() {
    private val binding by viewBindings<ActivityBackupBinding>()

    @Inject
    lateinit var accountDao: AccountDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.options.addOption(
            "Export",
            "Export your data to different formats",
            R.drawable.ic_database_export
        ) {
            lifecycleScope.launch {
                val accounts = accountDao.getAccounts()
                val items = ArrayList(accounts)

                if (items.isNotEmpty()) {
                    startActivity(ExportConfigActivity::class) {
                        putExtra(ExportConfigActivity.EXTRA_ITEMS, items)
                    }
                } else {
                    makeSnackBar("Nothing to export", binding.root)
                }
            }
        }
        binding.options.addOption(
            "Import",
            "Import your data from different sources",
            R.drawable.ic_database_import
        ) {
            startActivity(ImportActivity::class)
        }
    }
}