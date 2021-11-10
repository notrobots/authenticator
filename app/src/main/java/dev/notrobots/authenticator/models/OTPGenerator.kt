package dev.notrobots.authenticator.models

import dev.notrobots.androidstuff.util.now
import dev.turingcomplete.kotlinonetimepassword.*
import org.apache.commons.codec.binary.Base32
import java.util.concurrent.TimeUnit

object OTPGenerator {

//    fun checkSecret(secret: String): Boolean {
//        return try {
//            val gen = GoogleAuthenticator(secret)
//
//            gen.isValid(gen.generate())
//        }catch (e: Exception) {
//            false
//        }
//    }

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
