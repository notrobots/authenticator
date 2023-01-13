package dev.notrobots.authenticator.util

import androidx.fragment.app.FragmentActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.dialogs.ReplaceAccountDialog
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.ImportData
import dev.notrobots.authenticator.models.Tag
import dev.notrobots.authenticator.ui.backupimportresult.ImportResult
import dev.notrobots.authenticator.ui.backupimportresult.ImportResultActivity

/**
 * Utility for handling backup import and export processes.
 *
 * The methods in this utility are usually just shortcuts for launching activities or
 * showing dialogs when needed.
 */
object BackupUtil {
    /**
     * Checks if there are any duplicates in the given [backupData] and returns an instance of [ImportData].
     */
    suspend fun getDuplicates(backupData: BackupData, accountDao: AccountDao): ImportData {
        val accounts = mutableMapOf<Account, ImportResult>()
        val tags = mutableMapOf<Tag, ImportResult>()

        for (account in backupData.accounts) {
            accounts[account] = ImportResult(
                account.displayName,
                R.drawable.ic_account,
                accountDao.exists(account)
            )
        }

        for (tag in backupData.tags) {
            tags[tag] = ImportResult(
                tag.name,
                R.drawable.ic_tag
                // Duplicate tags are ignored
            )
        }

        return ImportData(
            accounts,
            tags,
            backupData.accountsWithTags
        )
    }

    /**
     * Handles the import process and shows the correct prompt to the user if needed.
     */
    suspend fun importBackupData(activity: FragmentActivity, backupData: BackupData, accountDao: AccountDao) {
        val importData = getDuplicates(backupData, accountDao)

        if (importData.hasDuplicates) {
            if (importData.isSingleAccountDuplicate) {
                ReplaceAccountDialog(importData)
                    .show(activity.supportFragmentManager, null)
            } else {
                ImportResultActivity.showResults(activity, importData)
            }
        } else {
            //FIXME: Add the items without launching ImportResultActivity

            ImportResultActivity.showResults(activity, importData)
        }
    }
}