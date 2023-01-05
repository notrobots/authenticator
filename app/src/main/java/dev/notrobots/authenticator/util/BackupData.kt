package dev.notrobots.authenticator.util

import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.Tag
import java.io.Serializable

/**
 * Container for the imported data
 */
data class BackupData(
    val accounts: Set<Account> = emptySet(),
    val tags: Set<Tag> = emptySet(),
    val accountsWithTags: Map<Account, Set<String>> = emptyMap()
) : Serializable {
    val isEmpty: Boolean
        get() = accounts.isEmpty() && tags.isEmpty() && accountsWithTags.isEmpty()
}