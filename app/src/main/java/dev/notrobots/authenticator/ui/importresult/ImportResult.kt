package dev.notrobots.authenticator.ui.importresult

import dev.notrobots.authenticator.models.BaseAccount

data class ImportResult(
    val item: BaseAccount,
    val isDuplicate: Boolean = false,
    var importStrategy: ImportStrategy = ImportStrategy.Default
)