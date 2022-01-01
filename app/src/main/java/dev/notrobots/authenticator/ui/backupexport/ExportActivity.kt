package dev.notrobots.authenticator.ui.backupexport

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.makeSnackBar
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityExportBinding
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.ui.backupexportconfig.ExportConfigActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExportActivity : AppCompatActivity() {
    private val binding by viewBindings<ActivityExportBinding>()

    @Inject
    lateinit var accountDao: AccountDao

    @Inject
    lateinit var accountGroupDao: AccountGroupDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        title = "Export"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        lifecycleScope.launch {
            val groups = accountGroupDao.getGroups()
            val accounts = accountDao.getAccounts()
            val adapter = ExportAdapter(groups + accounts)

            binding.list.layoutManager = LinearLayoutManager(this@ExportActivity)
            binding.list.adapter = adapter
            binding.next.setOnClickListener {
                val checked = adapter.checkedItems

                if (checked.isNotEmpty()) {
                    startActivity(ExportConfigActivity::class) {
                        putExtra(ExportConfigActivity.EXTRA_ITEMS, ArrayList(checked))
                    }
                } else {
                    val sb = makeSnackBar("No items selected", binding.root, Snackbar.LENGTH_SHORT)
                    sb.anchorView = binding.next
                }
            }
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