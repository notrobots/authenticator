package dev.notrobots.authenticator.ui.backupimportresult

import dev.notrobots.authenticator.models.Account

data class ImportResult(
    val item: Account,
    val isDuplicate: Boolean = false,
    var importStrategy: ImportStrategy = ImportStrategy.Default
)