package dev.notrobots.authenticator.models

import dev.turingcomplete.kotlinonetimepassword.*

class OTPProvider {
    fun generate(account: Account): String {
        return when (account.type) {
            OTPType.TOTP -> {
                GoogleAuthenticator(account.secret).generate()
            }
            OTPType.HOTP -> {
                val config = HmacOneTimePasswordConfig(
                    6,
                    HmacAlgorithm.SHA1
                )
                HmacOneTimePasswordGenerator(
                    account.secret.toByteArray(),
                    config
                ).generate(0)
            }
        }
    }
}
