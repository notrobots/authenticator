package dev.notrobots.authenticator.ui.backupimportresult

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.showChoice
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.Logger.Companion.logd
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityImportResultBinding
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.ui.accountlist.AccountListActivity
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import dev.notrobots.authenticator.util.AccountsWithTags
import dev.notrobots.preferences2.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImportResultActivity : AuthenticatorActivity() {
    private val binding by viewBindings<ActivityImportResultBinding>()
    private val viewModel by viewModels<AccountListViewModel>()
    private lateinit var importData: ImportData
    private val adapter = ImportResultAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        finishOnBackPressEnabled = true

        require(intent.hasExtra(EXTRA_IMPORT_DATA)) {
            "Extra $EXTRA_IMPORT_DATA must be defined"
        }

        importData = intent.getSerializableExtra(EXTRA_IMPORT_DATA) as ImportData

        lifecycleScope.launch {
            if (importData.hasDuplicates) {
                val adapterValues = mutableListOf<ImportResult>().apply {
                    addAll(importData.accounts.values)
                    addAll(importData.tags.values)
                }

                setContentView(binding.root)
                adapter.setItems(adapterValues)
                binding.list.layoutManager = LinearLayoutManager(this@ImportResultActivity)
                binding.done.setOnClickListener {
                    val isNotResolved = { r: ImportResult ->
                        r.isDuplicate && r.action == ImportAction.Default
                    }

                    if (adapterValues.any(isNotResolved)) {
                        showChoice(
                            "Import conflicts",
                            "There are still some conflicts left, are you sure you want to proceed?\n\nNOTE: By default conflicting items will be skipped",
                            "Proceed",
                            positiveCallback = {
                                importBackup()
                            },
                            "Cancel"
                        )
                    } else {
                        importBackup()
                    }
                }
            } else {
                importBackup()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        binding.list.adapter = adapter
    }

    override fun onPause() {
        super.onPause()

        binding.list.adapter = null
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_import_result, menu)
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_import_skip_all -> {
                for ((_, result) in importData.accounts) {
                    if (result.isDuplicate) {
                        result.action = ImportAction.Skip
                    }
                }
                adapter.notifyDataSetChanged()
            }
            R.id.menu_import_replace_all -> {
                for ((_, result) in importData.accounts) {
                    if (result.isDuplicate) {
                        result.action = ImportAction.Replace
                    }
                }
                adapter.notifyDataSetChanged()
            }
            R.id.menu_import_keep_all -> {
                for ((_, result) in importData.accounts) {
                    if (result.isDuplicate) {
                        result.action = ImportAction.KeepBoth
                    }
                }
                adapter.notifyDataSetChanged()
            }

            else -> super.onOptionsItemSelected(item)
        }

        return true
    }

    private fun importBackup() {
        var skipped = 0
        var added = 0
        var replaced = 0

        lifecycleScope.launch {
            // todo
            // viewModel.tagDao.insertOrIgnore(importData.tags.map { it.key })

            // Importing the tags first so the accounts can fetch the new added.
            for ((tag, _) in importData.tags) {
                // Tags are either added or not touched at all.
                // If a new account has a tag that is already in the db that tag
                // will be associated with the new account.
                // Tags don't have any additional property so no further action
                // is required.
                // *If* new properties are added this needs to handle duplicates
                if (!viewModel.tagDao.exists(tag.name)) {
                    viewModel.tagDao.insert(tag)
                    logd("Inserting tag: $tag")
                    added++
                }
            }

            for ((account, importResult) in importData.accounts) {
                if (importResult.isDuplicate) {
                    when (importResult.action) {
                        ImportAction.Default,
                        ImportAction.Skip -> skipped++
                        ImportAction.Replace -> {
                            val accountId = viewModel.accountDao.getAccount(account)?.accountId

                            accountId?.let {
                                viewModel.updateAccount(account)
                                logd("Updating account: $account (dummy)")
                                viewModel.accountTagCrossRefDao.deleteWithAccountId(accountId)
                                viewModel.accountTagCrossRefDao.insertWithNames(accountId, importData.accountsWithTags[account])
                            }

                            replaced++
                        }
                        ImportAction.KeepBoth -> {
                            val accountId = viewModel.insertAccountWithSameName(account)

                            logd("Adding account with same name: $account (dummy)")

                            viewModel.accountTagCrossRefDao.insertWithNames(accountId, importData.accountsWithTags[account])

                            added++
                        }
                    }
                } else {
                    val accountId = viewModel.insertAccount(account)

                    logd("Inserting account: $account")

                    viewModel.accountTagCrossRefDao.insertWithNames(accountId, importData.accountsWithTags[account])

                    added++
                }
            }

            startActivity(AccountListActivity::class) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    companion object {
        const val EXTRA_IMPORT_DATA = "ImportResultActivity.importData"

        /**
         * Starts this activity and shows the given [backupData].
         */
        fun showResults(context: Context, importData: ImportData) {
            context.startActivity(ImportResultActivity::class) { //FIXME: Start on top?
                putExtra(EXTRA_IMPORT_DATA, importData)
            }
        }
    }
}