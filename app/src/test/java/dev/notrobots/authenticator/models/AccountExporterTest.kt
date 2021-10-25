package dev.notrobots.authenticator.models

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
        val exporter1 = AccountExporter().apply {
            exportFormat = ExportFormat.Default
            exportOutput = ExportOutput.Text
        }
        val exporter2 = AccountExporter().apply {
            exportFormat = ExportFormat.Protobuf
            exportOutput = ExportOutput.Text
        }
        val exporter3 = AccountExporter().apply {
            exportFormat = ExportFormat.GoogleProtobuf
            exportOutput = ExportOutput.Text
        }

        assert(accounts == exporter1.import(exporter1.export(accounts) as String))
        assert(accounts == exporter2.import(exporter2.export(accounts) as String))
        assert(accounts == exporter3.import(exporter3.export(accounts) as String))
    }
}