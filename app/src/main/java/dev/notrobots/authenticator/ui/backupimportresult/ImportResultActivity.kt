package dev.notrobots.authenticator.ui.backupimportresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.showChoice
import dev.notrobots.androidstuff.extensions.showInfo
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityImportResultBinding
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.util.AccountExporter
import dev.notrobots.authenticator.ui.accountlist.AccountListActivity
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import dev.notrobots.authenticator.util.TextUtil
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImportResultActivity : AuthenticatorActivity() {
    private val binding by viewBindings<ActivityImportResultBinding>()
    private val viewModel by viewModels<AccountListViewModel>()
    private val importResults = mutableMapOf<Any, ImportResult>()
    private val importedData = mutableListOf<AccountExporter.ImportedData>()
    private val adapter = ImportResultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)

        title = "Imported backup"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        importedData.addAll(intent.getSerializableExtra(EXTRA_DATA) as List<AccountExporter.ImportedData>)
        binding.toolbarLayout.toolbar.setNavigationOnClickListener { finish() }
        binding.list.layoutManager = LinearLayoutManager(this@ImportResultActivity)
        binding.list.adapter = adapter
        binding.done.setOnClickListener {
            val isNotResolved = { r: ImportResult ->
                r.isDuplicate && r.importStrategy == ImportStrategy.Default
            }

            if (importResults.values.any(isNotResolved)) {
                showChoice(
                    "Import conflicts",
                    "There are still some conflicts left, are you sure you want to proceed?\n\nNOTE: By default conflicting items will be skipped",
                    "Proceed",
                    positiveCallback = {
                        importItems()
                    },
                    "Cancel"
                )
            } else {
                importItems()
            }
        }

        lifecycleScope.launch {
            for (account in importedData.flatMap { it.accounts }) {
                importResults[account] = ImportResult(
                    account.displayName,
                    R.drawable.ic_account,
                    viewModel.accountDao.exists(account)
                )
            }

            adapter.setItems(importResults.values.toList())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_import_result, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_import_skip_all -> {
                for (result in importResults.values) {
                    if (result.isDuplicate) {
                        result.importStrategy = ImportStrategy.Skip
                    }
                }
                adapter.notifyDataSetChanged()
            }
            R.id.menu_import_replace_all -> {
                for (result in importResults.values) {
                    if (result.isDuplicate) {
                        result.importStrategy = ImportStrategy.Replace
                    }
                }
                adapter.notifyDataSetChanged()
            }
            R.id.menu_import_keep_all -> {
                for (result in importResults.values) {
                    if (result.isDuplicate) {
                        result.importStrategy = ImportStrategy.KeepBoth
                    }
                }
                adapter.notifyDataSetChanged()
            }

            else -> return false
        }

        return true
    }

    private fun importItems() {
        var skipped = 0
        var added = 0
        var replaced = 0

        lifecycleScope.launch {
            for (entry in importResults) {
                if (entry.value.isDuplicate) {
                    when (entry.value.importStrategy) {
                        ImportStrategy.Replace -> {
                            when (val i = entry.key) {
                                is Account -> viewModel.updateAccount(i)
                            }
                            replaced++
                        }
                        ImportStrategy.KeepBoth -> {
                            when (val i = entry.key) {  //TODO: ImportResult should only be used for accounts, tags should just be imported automatically
                                is Account -> {
                                    viewModel.insertAccountWithSameName(i)
                                }
                            }
                            added++
                        }
                        ImportStrategy.Default,
                        ImportStrategy.Skip -> skipped++
                    }
                } else {
                    when (val i = entry.key) {
                        is Account -> viewModel.insertAccount(i)
                    }
                    added++
                }
            }

            showInfo(
                "Import succeed",
                "$added new items\n$replaced replaced items\n$skipped skipped items"
            ) {
                startActivity(AccountListActivity::class) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
        }
    }

    companion object {
        const val EXTRA_DATA = "ImportResultActivity.DATA"

        fun showResults(context: Context, importedData: List<AccountExporter.ImportedData>) {
            context.startActivity(ImportResultActivity::class) {
                putExtra(EXTRA_DATA, ArrayList(importedData)) //FIXME: Start on top?
            }
        }

        fun showResults(context: Context, importedData: AccountExporter.ImportedData) {
            showResults(context, listOf(importedData))
        }
    }
}