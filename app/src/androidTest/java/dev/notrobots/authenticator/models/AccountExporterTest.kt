package dev.notrobots.authenticator.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.notrobots.androidstuff.extensions.contentEquals
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import org.junit.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountExporterTest {
    @Test
    fun testExport() {
        val groups = listOf(
            AccountGroup("Group 1").apply {
                id = 2
                order = 1
            },
            AccountGroup("Group 2").apply {
                id = 3
                order = 2
            }
        )
        val accounts = listOf(
            Account("max", "22334455").apply {
                issuer = "google.com"
                label = "Google"
                groupId = 2
                type = OTPType.TOTP
                algorithm = HmacAlgorithm.SHA1
                digits = 8
            },
            Account("john", "22334455").apply {
                issuer = "google.com"
                label = "Google"
                groupId = 2
                type = OTPType.HOTP
                algorithm = HmacAlgorithm.SHA256
                digits = 10
                counter = 10
            },
            Account("jane", "22334455").apply {
                issuer = "google.com"
                label = "Google"
                groupId = 3
                type = OTPType.TOTP
                algorithm = HmacAlgorithm.SHA512
                period = 60
            }
        )
        val exporters = listOf(
            AccountExporter().apply {
                exportFormat = ExportFormat.Default
            },
            AccountExporter().apply {
                exportFormat = ExportFormat.Protobuf
            }
        )

        for (exporter in exporters) {
            val exported = exporter.exportText(accounts, groups)
            val imported = exporter.import(exported)
            val importedGroups = imported.filterIsInstance<AccountGroup>()
            val importedAccounts = imported.filterIsInstance<Account>()

            assert(accounts.contentEquals(importedAccounts) { a, b ->
                a.order == b.order &&
                a.name == b.name &&
                a.secret == b.secret &&
                a.issuer == b.issuer &&
                a.label == b.label &&
                a.type == b.type &&
                a.counter == b.counter &&
                a.digits == b.digits &&
                a.period == b.period &&
                a.algorithm == b.algorithm &&
                a.groupId == b.groupId
            })
            assert(groups.contentEquals(importedGroups) { a, b ->
                a.name == b.name &&
                a.order == b.order
            })
        }
    }

    @Test
    fun testImport() {
        val exporter = AccountExporter()

        // Account
        assertThrows<Exception> {
            // Secret is missing
            exporter.import("otpauth://totp/name?issuer=test.com")

            // Issuer is defined but blank
            exporter.import("otpauth://totp/name?secret=22334455&issuer= ")
            exporter.import("otpauth://totp/name?secret=22334455&issuer=%20")

            // Name/label not defined
            exporter.import("otpauth://totp/?secret=22334455&issuer=google.com")

            // Name is blank
            exporter.import("otpauth://totp/label: ?secret=22334455&issuer=google.com")

            // Label is blank
            exporter.import("otpauth://totp/ :test?secret=22334455&issuer=google.com")

            // Secret is not base32
            exporter.import("otpauth://totp/label:name?secret=12345678")
            exporter.import("otpauth://totp/label:name?secret=abcdefghi")

            // Algorithm is unknown
            exporter.import("otpauth://totp/name?secret=22334455&algorithm=md5")

            // Digits value is invalid
            exporter.import("otpauth://totp/name?secret=22334455&digits=test")

            // Counter value is invalid
            exporter.import("otpauth://totp/name?secret=22334455&counter=test")

            // Period is invalid
            exporter.import("otpauth://totp/name?secret=22334455&period=test")

            // Digits number is not an integer
            exporter.import("otpauth://totp/name?secret=22334455&digits=test")
        }

        // Group
        assertThrows<Exception> {
            // Name not defined
            exporter.import("otpauth://group/")
            exporter.import("otpauth://group?order=1")

            // Invalid order
            exporter.import("otpauth://group/Group 1?order=test")
        }

        val account = assertDoesNotThrow {
            exporter.import(
                "otpauth://totp/label:name" +
                "?secret=22335544" +
                "&issuer=site.com" +
                "&counter=60" +
                "&digits=8" +
                "&period=40" +
                "&algorithm=sha256" +
                "&group=5"
            ).first() as Account
        }

        assert(account.label == "label")
        assert(account.name == "name")
        assert(account.type == OTPType.TOTP)
        assert(account.secret == "22335544")
        assert(account.counter == 60L)
        assert(account.digits == 8)
        assert(account.period == 40L)
        assert(account.algorithm == HmacAlgorithm.SHA256)
        assert(account.groupId == 5L)
    }
}