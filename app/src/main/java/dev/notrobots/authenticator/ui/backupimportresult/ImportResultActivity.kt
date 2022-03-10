package dev.notrobots.authenticator.ui.backupimportresult

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.showChoice
import dev.notrobots.androidstuff.extensions.showInfo
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.util.viewBindings
import dev.notrobots.authenticator.databinding.ActivityImportResultBinding
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.util.AccountExporter
import dev.notrobots.authenticator.ui.accountlist.AccountListActivity
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImportResultActivity : AppCompatActivity() {
    private val binding by viewBindings<ActivityImportResultBinding>(this)
    private val viewModel by viewModels<AccountListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val data = intent.getSerializableExtra(EXTRA_DATA) as AccountExporter.ImportedData
        val importResults = mutableListOf<ImportResult>()
        val adapter = ImportResultAdapter()

        binding.list.layoutManager = LinearLayoutManager(this@ImportResultActivity)
        binding.list.adapter = adapter

        lifecycleScope.launch {
            for (account in data.accounts) {
                val result = ImportResult(
                    account,
                    viewModel.accountDao.exists(account)
                )

                importResults.add(result)
            }

            adapter.setItems(importResults)
        }

        binding.skipAll.setOnClickListener {
            for (result in importResults) {
                if (result.isDuplicate) {
                    result.importStrategy = ImportStrategy.Skip
                }
            }
            adapter.notifyDataSetChanged()
        }
        binding.replaceAll.setOnClickListener {
            for (result in importResults) {
                if (result.isDuplicate) {
                    result.importStrategy = ImportStrategy.Replace
                }
            }
            adapter.notifyDataSetChanged()
        }
        binding.done.setOnClickListener {
            val isNotResolved = { r: ImportResult ->
                r.isDuplicate && r.importStrategy == ImportStrategy.Default
            }

            if (importResults.any(isNotResolved)) {
                showChoice(
                    "Import conflicts",
                    "There are still some conflicts left, are you sure you want to proceed?\n\nNOTE: By default conflicting items will be skipped",
                    "Proceed",
                    positiveCallback = {
                        importItems(importResults)
                    },
                    "Cancel"
                )
            } else {
                importItems(importResults)
            }
        }
    }

    private fun importItems(importResults: List<ImportResult>) {
        var skipped = 0
        var added = 0
        var replaced = 0

        lifecycleScope.launch {
            for (importResult in importResults) {
                if (importResult.isDuplicate) {
                    when (importResult.importStrategy) {
                        ImportStrategy.Replace -> {
                            when (val i = importResult.item) {
                                is Account -> viewModel.updateAccount(i)
                            }
                            replaced++
                        }
//                        ImportStrategy.KeepBoth -> {  //TODO: Add this later
//                            val name = importResult.item.getNextName()
//
//                            when (val i = importResult.item) {
//                                is AccountGroup -> {
//                                    val group = AccountGroup(name).apply {
//                                        isExpanded = i.isExpanded
//                                        order = i.order
//                                    }
//                                    viewModel.addGroup(group)
//                                }
//                                is Account -> {
//                                    val account = i.clone().apply {
//                                        this.name = name
//                                    }
//                                    viewModel.accountDao.insert(account)
//                                }
//                            }
//                            added++
//                        }
                        ImportStrategy.Default,
                        ImportStrategy.Skip -> skipped++
                    }
                } else {
                    when (val i = importResult.item) {
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
    }
}