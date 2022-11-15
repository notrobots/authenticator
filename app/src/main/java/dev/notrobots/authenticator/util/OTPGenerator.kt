package dev.notrobots.authenticator.util

import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType
import dev.turingcomplete.kotlinonetimepassword.*
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

object OTPGenerator {
    fun generate(account: Account): String {
        // The "Google way" is to pass the generator a base32 string that is decoded internally.
        // Other authenticators take a decoded string.
        // Since the [GoogleAuthenticator] generator doesn't support any additional
        // configuration we use the standard generators and pass in a decoded base32 string.
        //
        // More info: https://github.com/marcelkliemannel/kotlin-onetimepassword#google-authenticator
        val secret = Base32().decode(account.secret.toByteArray())

        return when (account.type) {
            OTPType.TOTP -> {
                val config = TimeBasedOneTimePasswordConfig(
                    account.period,
                    TimeUnit.SECONDS,
                    account.digits,
                    account.algorithm
                )
                TimeBasedOneTimePasswordGenerator(
                    secret,
                    config
                ).generate(now())
            }
            OTPType.HOTP -> {
                val config = HmacOneTimePasswordConfig( //TODO: Keep an instance somewhere
                    account.digits,
                    account.algorithm
                )
                HmacOneTimePasswordGenerator(
                    secret,
                    config
                ).generate(account.counter)
            }
        }
    }
}
