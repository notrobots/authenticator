package dev.notrobots.authenticator.ui.backupexport

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
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
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import dev.notrobots.authenticator.ui.backupexportconfig.ExportConfigActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ExportActivity : AppCompatActivity() {
    private val binding by viewBindings<ActivityExportBinding>()
    private val viewModel by viewModels<AccountListViewModel>()
    private val adapter = ExportAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel.accounts.observe(this, object : Observer<List<Account>> {
            override fun onChanged(t: List<Account>) {
                adapter.setItems(t)
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
                viewModel.accounts.removeObserver(this)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_export_activity, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.menu_export_select_all -> {
                adapter.selectAll()
            }

            else -> return false
        }

        return true
    }
}