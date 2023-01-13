package dev.notrobots.authenticator.models

import dev.notrobots.authenticator.ui.backupimportresult.ImportResult
import dev.notrobots.authenticator.util.AccountsWithTags
import java.io.Serializable

data class ImportData(
    val accounts: Map<Account, ImportResult> = mapOf(),
    val tags: Map<Tag, ImportResult> = mapOf(),
    val accountsWithTags: AccountsWithTags = mapOf()
) : Serializable {
    /**
     * Whether or not there is a single account and it is also a duplicate.
     */
    val isSingleAccountDuplicate
        get() = accounts.size == 1 && accounts.values.first().isDuplicate

    /**
     * Whether or not there is at least one duplicate account.
     *
     * Note: Tags won't be checked.
     */
    val hasDuplicates
        get() = accounts.values.any { it.isDuplicate }
}