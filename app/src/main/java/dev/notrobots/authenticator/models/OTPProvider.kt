package dev.notrobots.authenticator.models

import dev.turingcomplete.kotlinonetimepassword.*
import org.apache.commons.codec.binary.Base32

object OTPProvider {    //FIXME: OTPGenerator
    fun checkSecret(secret: String): Boolean {
        return try {
            val gen = GoogleAuthenticator(secret)

            gen.isValid(gen.generate())
        }catch (e: Exception) {
            false
        }
    }

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
                    Base32().decode(account.secret.toByteArray()),
                    config
                ).generate(account.counter)
            }
        }
    }
}
