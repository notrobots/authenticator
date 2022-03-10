package dev.notrobots.authenticator.util

import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType
import dev.turingcomplete.kotlinonetimepassword.*
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

object OTPGenerator {
    fun generate(account: Account): String {
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
                val config = HmacOneTimePasswordConfig(
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