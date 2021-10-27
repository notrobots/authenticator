package dev.notrobots.authenticator.models

import androidx.core.net.toUri
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.notrobots.authenticator.extensions.allIndexed
import dev.notrobots.authenticator.extensions.contentEquals
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountExporterTest {
    private val accounts = listOf(
        Account("max", "22334455").apply {
            issuer = "google.com"
            label = "Google"
            type = OTPType.TOTP
        },
        Account("john", "22334455").apply {
            issuer = "google.com"
            label = "Google"
            type = OTPType.HOTP
        },
        Account("jane", "22334455").apply {
            issuer = "google.com"
            label = "Google"
            type = OTPType.TOTP
        }
    )

    @Test
    fun testExport() {
        val exporters = listOf(
            AccountExporter().apply {
                exportFormat = ExportFormat.Default
                exportOutput = ExportOutput.Text
            },
            AccountExporter().apply {
                exportFormat = ExportFormat.Protobuf
                exportOutput = ExportOutput.Text
            },
            AccountExporter().apply {
                exportFormat = ExportFormat.GoogleProtobuf
                exportOutput = ExportOutput.Text
            }
        )

        for (exporter in exporters) {
            val exported = exporter.export(accounts) as String
            val imported = exporter.import(exported)

            assert(accounts.contentEquals(imported) { a, b -> a.id == b.id })
        }
    }

    @Test
    fun testImport() {
        val exporter = AccountExporter()
        
        // Missing secret
        assertThrows<Exception> {
            exporter.import("otpauth://totp/test?issuer=test.com".toUri())
        }

        // Issuer is defined but blank
        assertThrows<Exception> {
            exporter.import("otpauth://totp/danhersam?secret=22334455&issuer= ".toUri())
        }

        // Issuer is defined but blank
        assertThrows<Exception> {
            exporter.import("otpauth://totp/danhersam?secret=22334455&issuer=%20".toUri())
        }

        // No name/label
        assertThrows<Exception> {
            exporter.import("otpauth://totp/?secret=22334455&issuer=google.com".toUri())
        }

        // Label is blank
        assertThrows<Exception> {
            exporter.import("otpauth://totp/ :test?secret=22334455&issuer=google.com".toUri())
        }

        // Name is blank
        assertThrows<Exception> {
            exporter.import("otpauth://totp/label: ?secret=22334455&issuer=google.com".toUri())
        }

        // Invalid name
//        assertThrows<Exception> {
//            exporter.import("otpauth://totp/label:name:?secret=22334455&issuer=google.com".toUri())
//        }

        // Secret is not base32
        assertThrows<Exception> {
            exporter.import("otpauth://totp/label:name?secret=12345678".toUri())
        }
        assertThrows<Exception> {
            exporter.import("otpauth://totp/label:name?secret=abcdefghi".toUri())
        }

        // Valid accounts
        assertDoesNotThrow {
            exporter.import("otpauth://totp/label:name?secret=22334455".toUri())
        }
        assertDoesNotThrow {
            exporter.import("otpauth://totp/name?secret=22334455&issuer=site.com".toUri())
        }
        assertDoesNotThrow {
            exporter.import("otpauth://totp/label:name?secret=22334455&issuer=site.com".toUri())
        }
    }
}