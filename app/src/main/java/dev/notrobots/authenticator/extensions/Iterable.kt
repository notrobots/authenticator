package dev.notrobots.authenticator.extensions

import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountWithTags
import dev.notrobots.authenticator.models.Tag

internal operator fun Iterable<AccountWithTags>.get(account: Account): AccountWithTags? {
    return find { it.account == account }
}

internal fun Iterable<AccountWithTags>.getTags(account: Account): List<Tag>? {
    return find { it.account == account }?.tags
}

internal fun Iterable<String>.contains(value: String, ignoreCase: Boolean): Boolean {
    return find { it.equals(value, ignoreCase) } != null
}

inline fun <T> Iterable<T>.indexOfFirstOrNull(predicate: (T) -> Boolean): Int? {
    val index = indexOfFirst(predicate)

    return if (index > -1) index else null
}
