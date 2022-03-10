package dev.notrobots.authenticator.models

import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.notrobots.authenticator.util.AccountExporter
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountExporterTest {
    private val groups = listOf(
        AccountGroup("Group 1").apply { id = 2; order = 0 },
        AccountGroup("Group 2").apply { id = 3; order = 1 },
        AccountGroup("Group 3").apply { id = 4; order = 2 }
    )
    private val accounts = listOf(
        Account("Account 1", "22334455").apply { groupId = 2; type = OTPType.HOTP },
        Account("Account 2", "22334466").apply { groupId = 2 },
        Account("Account 3", "22332277").apply { groupId = 3 },
        Account("Account 4", "22334455").apply { groupId = 3 },
        Account("Account 5", "22444455").apply { groupId = 4; type = OTPType.HOTP },
        Account("Account 6", "22774477").apply { groupId = 4 },
        Account("Account 7", "77334455").apply { },
        Account("Account 8", "22223355").apply { type = OTPType.HOTP }
    )

    @Test
    fun export() {
        val exportData = listOf(
            AccountExporter.ImportedData(groups, accounts),
            AccountExporter.ImportedData(groups, accounts).apply {
                this.format = BackupFormat.Default
            }
        )

        for (data in exportData) {
            val exporter = AccountExporter()
            val exported = exporter.exportText(data)
            val imported = exporter.import(exported)
            val importedGroups = imported.groups
            val importedAccounts = imported.accounts

            for (i in accounts.indices) {
                assert(accounts[i].order == importedAccounts[i].order)
                assert(accounts[i].name == importedAccounts[i].name)
                assert(accounts[i].secret == importedAccounts[i].secret)
                assert(accounts[i].issuer == importedAccounts[i].issuer)
                assert(accounts[i].label == importedAccounts[i].label)
                assert(accounts[i].type == importedAccounts[i].type)
                assert(accounts[i].counter == importedAccounts[i].counter)
                assert(accounts[i].digits == importedAccounts[i].digits)
                assert(accounts[i].period == importedAccounts[i].period)
                assert(accounts[i].algorithm == importedAccounts[i].algorithm)
                assert(accounts[i].groupId == importedAccounts[i].groupId)
            }

            for (i in groups.indices) {
                assert(groups[i].name == importedGroups[i].name)
                assert(groups[i].order == importedGroups[i].order)
            }
        }
    }

//    @Test
//    fun testImport_withAccounts() {
//        val exporter = AccountExporter()
//
//        assertThrows<Exception> {
//            // Secret is missing
//            exporter.import("otpauth://totp/name?issuer=test.com")
//
//            // Issuer is defined but blank
//            exporter.import("otpauth://totp/name?secret=22334455&issuer= ")
//            exporter.import("otpauth://totp/name?secret=22334455&issuer=%20")
//
//            // Name/label not defined
//            exporter.import("otpauth://totp/?secret=22334455&issuer=google.com")
//
//            // Name is blank
//            exporter.import("otpauth://totp/label: ?secret=22334455&issuer=google.com")
//
//            // Label is blank
//            exporter.import("otpauth://totp/ :test?secret=22334455&issuer=google.com")
//
//            // Secret is not base32
//            exporter.import("otpauth://totp/label:name?secret=12345678")
//            exporter.import("otpauth://totp/label:name?secret=abcdefghi")
//
//            // Algorithm is unknown
//            exporter.import("otpauth://totp/name?secret=22334455&algorithm=md5")
//
//            // Digits value is invalid
//            exporter.import("otpauth://totp/name?secret=22334455&digits=test")
//
//            // Counter value is invalid
//            exporter.import("otpauth://totp/name?secret=22334455&counter=test")
//
//            // Period is invalid
//            exporter.import("otpauth://totp/name?secret=22334455&period=test")
//
//            // Digits number is not an integer
//            exporter.import("otpauth://totp/name?secret=22334455&digits=test")
//        }
//    }
//
//    @Test
//    fun testImport_withGroups() {
//        val exporter = AccountExporter()
//
//        // Group
//        assertThrows<Exception> {
//            // Name not defined
//            exporter.import("otpauth://group/")
//            exporter.import("otpauth://group?order=1")
//
//            // Invalid order
//            exporter.import("otpauth://group/Group 1?order=test")
//        }
//    }
//
//    @Test
//    fun testImport_withAllParameters() {
//        val exporter = AccountExporter()
//        val account = assertDoesNotThrow {
//            exporter.import(
//                "otpauth://totp/label:name" +
//                "?secret=22335544" +
//                "&issuer=site.com" +
//                "&counter=60" +
//                "&digits=8" +
//                "&period=40" +
//                "&algorithm=sha256"
//            ).accounts.first()
//        }
//
//        assert(account.label == "label")
//        assert(account.name == "name")
//        assert(account.type == OTPType.TOTP)
//        assert(account.secret == "22335544")
//        assert(account.counter == 60L)
//        assert(account.digits == 8)
//        assert(account.period == 40L)
//        assert(account.algorithm == HmacAlgorithm.SHA256)
//    }
}