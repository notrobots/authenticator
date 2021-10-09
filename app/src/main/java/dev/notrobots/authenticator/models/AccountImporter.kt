package dev.notrobots.authenticator.models

import android.net.Uri

class AccountImporter {
//    private val accounts: List<Uri>

    constructor(content: String) {
        // Types of string content
        // + Line by line plain text Uri
        // + Line by line proto3 Uri
        // + CSV plain text
    }

    constructor(uri: Uri) {
        // Types of uris
        // + standard otpauth (plain text)
        // + GA otpauth-migration (proto3)
        // + otpauth/export (proto3)
    }

    fun import(): List<Account> {
        return emptyList()
    }
}