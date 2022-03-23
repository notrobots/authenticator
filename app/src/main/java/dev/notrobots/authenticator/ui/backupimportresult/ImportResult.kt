package dev.notrobots.authenticator.ui.backupimportresult

import androidx.annotation.DrawableRes

data class ImportResult(
    val title: String,
    @DrawableRes val icon: Int,
    val isDuplicate: Boolean = false,
    var importStrategy: ImportStrategy = ImportStrategy.Default
)