package dev.notrobots.authenticator.models

import android.net.Uri
import androidx.core.net.toUri
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountTest {
    @Test
    fun testParse() {
        // Missing secret
        assertThrows(Exception::class.java) {
            Account.parse("otpauth://totp/test?issuer=test.com".toUri())
        }

        // Issuer is defined but blank
        assertThrows(Exception::class.java) {
            Account.parse("otpauth://totp/danhersam?secret=JBSWY3DPEHPK3PXP&issuer= ".toUri())
        }

        // Issuer is defined but blank
        assertThrows(Exception::class.java) {
            Account.parse("otpauth://totp/danhersam?secret=JBSWY3DPEHPK3PXP&issuer=%20".toUri())
        }

        // No name/label
        assertThrows(Exception::class.java) {
            Account.parse("otpauth://totp/?secret=JBSWY3DPEHPK3PXP&issuer=google.com".toUri())
        }

        // Label is blank
        assertThrows(Exception::class.java) {
            Account.parse("otpauth://totp/ :test?secret=JBSWY3DPEHPK3PXP&issuer=google.com".toUri())
        }

        // Name is blank
        assertThrows(Exception::class.java) {
            Account.parse("otpauth://totp/label: ?secret=JBSWY3DPEHPK3PXP&issuer=google.com".toUri())
        }

        // Secret is not base32
        assertThrows(Exception::class.java) {
            Account.parse("otpauth://totp/label:name?secret=12345678".toUri())
        }
        assertThrows(Exception::class.java) {
            Account.parse("otpauth://totp/label:name?secret=abcdefghi".toUri())
        }

        Account.parse("otpauth://totp/label:name?secret=33445566eeffgghh".toUri())
        Account.parse("otpauth://totp/danhersam?secret=JBSWY3DPEHPK3PXP&issuer=danhersam.com".toUri())
    }
}