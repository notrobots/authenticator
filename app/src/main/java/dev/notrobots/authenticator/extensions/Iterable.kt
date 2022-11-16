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

fun <T> Iterable<T>.joinToStringIndexed(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: (index: Int, T) -> CharSequence
): String {
    var index = 0

    return joinToString(separator, prefix, postfix, limit, truncated) {
        transform(index++, it)
    }
}
