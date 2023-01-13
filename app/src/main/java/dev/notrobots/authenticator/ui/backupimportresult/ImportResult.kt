package dev.notrobots.authenticator.ui.backupimportresult

import androidx.annotation.DrawableRes
import java.io.Serializable

data class ImportResult(
    val title: String,
    @DrawableRes val icon: Int,
    val isDuplicate: Boolean = false,
    var action: ImportAction = ImportAction.Default
) : Serializable