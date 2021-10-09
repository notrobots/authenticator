package dev.notrobots.authenticator.models

import android.net.Uri

abstract class ProtocolBuffer (private var accounts: List<Account>) {
    abstract fun encode(): List<Uri>
    abstract fun decode(): List<Uri>
}

class GoogleAuthenticatorProtocolBuffer(accounts: List<Account>) : ProtocolBuffer(accounts) {
    override fun encode(): List<Uri> {
        TODO("Not yet implemented")
    }

    override fun decode(): List<Uri> {
        TODO("Not yet implemented")
    }
}

class AuthenticatorProtocolBuffer(accounts: List<Account>) : ProtocolBuffer(accounts) {
    override fun encode(): List<Uri> {
        TODO("Not yet implemented")
    }

    override fun decode(): List<Uri> {
        TODO("Not yet implemented")
    }
}