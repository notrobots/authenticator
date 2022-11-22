package dev.notrobots.authenticator.ui.backupimportresult

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.showChoice
import dev.notrobots.androidstuff.extensions.showInfo
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.databinding.ActivityImportResultBinding
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AppTheme
import dev.notrobots.authenticator.models.Tag
import dev.notrobots.authenticator.ui.accountlist.AccountListActivity
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import dev.notrobots.authenticator.util.BackupData
import dev.notrobots.preferences2.putAppTheme
import dev.notrobots.preferences2.util.parseEnum
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ImportResultActivity : AuthenticatorActivity() {
    private val binding by viewBindings<ActivityImportResultBinding>()
    private val viewModel by viewModels<AccountListViewModel>()
    private val importResults = mutableMapOf<Any, ImportResult>()
    private var backupData: BackupData? = null
    private val adapter = ImportResultAdapter()
    private var noDuplicates = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        finishOnBackPressEnabled = true

        backupData = intent.getSerializableExtra(EXTRA_DATA) as BackupData

        requireNotNull(backupData) {
            "Backup data is null"
        }

        lifecycleScope.launch {
            for (account in backupData!!.accounts) {
                importResults[account] = ImportResult(
                    account.displayName,
                    R.drawable.ic_account,
                    viewModel.accountDao.exists(account)
                )
            }

            for (tag in backupData!!.tags) {
                importResults[tag] = ImportResult(
                    tag.name,
                    R.drawable.ic_tag,
                    false   // Duplicate tags are ignored
                )
            }

            if (backupData!!.settings.isNotEmpty()) {
                importResults[backupData!!.settings] = ImportResult(
                    getString(R.string.label_settings),
                    R.drawable.ic_settings,
                    false   // Settings will always be duplicate
                )
            }

            noDuplicates = importResults.none { it.value.isDuplicate }

            if (noDuplicates) {
                importBackup()
            } else {
                setContentView(binding.root)
                adapter.setItems(importResults.values.toList())
                binding.list.layoutManager = LinearLayoutManager(this@ImportResultActivity)
                binding.done.setOnClickListener {
                    val isNotResolved = { r: ImportResult ->
                        r.isDuplicate && r.action == ImportAction.Default
                    }

                    if (importResults.values.any(isNotResolved)) {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_import_skip_all -> {
                for (result in importResults.values) {
                    if (result.isDuplicate) {
                        result.action = ImportAction.Skip
                    }
                }
                adapter.notifyDataSetChanged()
            }
            R.id.menu_import_replace_all -> {
                for (result in importResults.values) {
                    if (result.isDuplicate) {
                        result.action = ImportAction.Replace
                    }
                }
                adapter.notifyDataSetChanged()
            }
            R.id.menu_import_keep_all -> {
                for (result in importResults.values) {
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
            val importedAccounts = importResults.filter { it.key is Account }
            val importedTags = importResults.filter { it.key is Tag }
            val importedSettings = importResults.keys.find { it is Map<*, *> } as? Map<String, Any?>

            // Importing the tags first so the accounts can fetch the new added.
            for ((tag, importResult) in importedTags) {
                tag as Tag

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

            for ((account, importResult) in importedAccounts) {
                account as Account

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
                                addTagsToAccount(it, account)
                            }

                            replaced++
                        }
                        ImportAction.KeepBoth -> {
                            val accountId = viewModel.insertAccountWithSameName(account)

                            logd("Adding account with same name: $account (dummy)")
                            addTagsToAccount(accountId, account)

                            added++
                        }
                    }
                } else {
                    val accountId = viewModel.insertAccount(account)

                    logd("Inserting account: $account")
                    addTagsToAccount(accountId, account)

                    added++
                }
            }

            // Settings are always overwritten.
            importedSettings?.let {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this@ImportResultActivity)
//                        val prefsMap = prefs::class.memberProperties.find { it.name == "mMap" }
//
//                        prefsMap?.let {
//                            for (entry in item) {
//
//                            }
//                        }

                //TODO: This should be replace by reflections
                if (it.containsKey(Preferences.APP_THEME)) {
                    //TODO: Preferences2 should let devs put a string or int when an enum value is used
                    prefs.putAppTheme(parseEnum<AppTheme>(it[Preferences.APP_THEME] as String))
                }
            }

            startActivity(AccountListActivity::class) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    private suspend fun addTagsToAccount(accountId: Long, dummy: Account) {
        val tagNames = backupData!!.accountsWithTags[dummy]

        if (tagNames != null) {
            for (tag in tagNames) {
                val tagId = viewModel.tagDao.getTag(tag)!!.tagId

                viewModel.accountTagCrossRefDao.insert(accountId, tagId)
            }
            logd("Adding tags $tagNames to account $dummy (dummy)")
        }
    }

    companion object {
        const val EXTRA_DATA = "ImportResultActivity.DATA"

        fun showResults(context: Context, backupData: BackupData) {
            context.startActivity(ImportResultActivity::class) {
                putExtra(EXTRA_DATA, backupData) //FIXME: Start on top?
            }
        }
    }
}