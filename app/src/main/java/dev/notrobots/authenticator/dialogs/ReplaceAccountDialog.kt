package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountTagCrossRefDao
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.models.ImportData
import dev.notrobots.authenticator.models.MaterialDialogBuilder
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReplaceAccountDialog(
    private var importData: ImportData? = null
) : DialogFragment() {
    private val logger = Logger(this)

    @Inject
    protected lateinit var accountDao: AccountDao

    @Inject
    protected lateinit var tagDao: TagDao

    @Inject
    protected lateinit var accountTagCrossRefDao: AccountTagCrossRefDao

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let {
            importData = it.getSerializable(SAVED_STATE_IMPORT_DATA) as? ImportData
        }

        requireNotNull(importData) {    //TODO android-stuff Logger should have methods like this
            "importData must not be null"
        }

        if (importData!!.accounts.size > 1) {
            logger.logw(
                "importData.accounts has more than one account. " +
                "This dialog should only be used with a single account"
            )
        }

        return MaterialDialogBuilder(requireContext())
            .setTitle(R.string.label_account_already_exists_title)
            .setMessage(R.string.label_account_already_exists_description)
            .setPositiveButton(R.string.label_replace) { d, i ->
                importData?.let { importData ->
                    requireActivity().lifecycleScope.launch {
                        val account = importData.accounts.keys.first()
                        val accountId = accountDao.getAccount(account)?.accountId

                        accountId?.let {
                            accountDao.update(account, true)
                            accountTagCrossRefDao.deleteWithAccountId(accountId)
                            accountTagCrossRefDao.insertWithNames(accountId, importData.accountsWithTags[account])
                        }
                    }
                }
                d.dismiss()
            }
            .setNeutralButton(R.string.label_keep_both) { d, i ->
                importData?.let { importData ->
                    requireActivity().lifecycleScope.launch {
                        tagDao.insertOrIgnore(importData.tags.keys)

                        val account = importData.accounts.keys.first()
                        val accountId = accountDao.insertAccountWithSameName(account)
                        val tagNames = importData.accountsWithTags[account]

                        accountTagCrossRefDao.insertWithNames(accountId, tagNames)
                    }
                }
                d.dismiss()
            }
            .setNegativeButton(R.string.label_cancel, null)
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SAVED_STATE_IMPORT_DATA, importData)
    }

    companion object {
        private const val SAVED_STATE_IMPORT_DATA = "ReplaceAccountDialog.importData"
    }
}