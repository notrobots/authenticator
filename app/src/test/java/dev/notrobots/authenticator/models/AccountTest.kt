package dev.notrobots.authenticator.models

import androidx.core.net.toUri
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountTest {
    @Test
    fun testParse() {
        // Missing secret
        assertThrows(Exception::class.java) {
            AccountExporter().import("otpauth://totp/test?issuer=test.com".toUri())
        }

        // Issuer is defined but blank
        assertThrows(Exception::class.java) {
            AccountExporter().import("otpauth://totp/danhersam?secret=JBSWY3DPEHPK3PXP&issuer= ".toUri())
        }

        // Issuer is defined but blank
        assertThrows(Exception::class.java) {
            AccountExporter().import("otpauth://totp/danhersam?secret=JBSWY3DPEHPK3PXP&issuer=%20".toUri())
        }

        // No name/label
        assertThrows(Exception::class.java) {
            AccountExporter().import("otpauth://totp/?secret=JBSWY3DPEHPK3PXP&issuer=google.com".toUri())
        }

        // Label is blank
        assertThrows(Exception::class.java) {
            AccountExporter().import("otpauth://totp/ :test?secret=JBSWY3DPEHPK3PXP&issuer=google.com".toUri())
        }

        // Name is blank
        assertThrows(Exception::class.java) {
            AccountExporter().import("otpauth://totp/label: ?secret=JBSWY3DPEHPK3PXP&issuer=google.com".toUri())
        }

        // Secret is not base32
        assertThrows(Exception::class.java) {
            AccountExporter().import("otpauth://totp/label:name?secret=12345678".toUri())
        }
        assertThrows(Exception::class.java) {
            AccountExporter().import("otpauth://totp/label:name?secret=abcdefghi".toUri())
        }

        AccountExporter().import("otpauth://totp/label:name?secret=33445566eeffgghh".toUri())
        AccountExporter().import("otpauth://totp/danhersam?secret=JBSWY3DPEHPK3PXP&issuer=danhersam.com".toUri())
    }
}