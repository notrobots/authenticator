package dev.notrobots.authenticator.util

import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.Tag
import java.io.Serializable

/**
 * Container for the imported data
 */
data class BackupData(
    val accounts: List<Account> = listOf(),
    val tags: List<Tag> = emptyList(),
    val accountsWithTags: Map<Account, List<String>> = emptyMap(),
    val settings: Map<String, Any?> = emptyMap()
) : Serializable {
    val isEmpty: Boolean
        get() = accounts.isEmpty() && tags.isEmpty() && accountsWithTags.isEmpty() && settings.isEmpty()
}