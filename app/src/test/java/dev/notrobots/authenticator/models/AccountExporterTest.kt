package dev.notrobots.authenticator.models

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccountExporterTest {
    @Test
    fun testExport() {
        val exporter = AccountExporter().apply {
            exportFormat = ExportFormat.Protobuf
            exportOutput = ExportOutput.Text
        }
        val accounts = listOf(
            Account("max", "12345678").apply {
                issuer = "google.com"
            },
            Account("john", "12345678").apply {
                issuer = "google.com"
            },
            Account("jane", "12345678").apply {
                issuer = "google.com"
            }
        )
        val export = exporter.export(accounts)

        println(export)
    }
}