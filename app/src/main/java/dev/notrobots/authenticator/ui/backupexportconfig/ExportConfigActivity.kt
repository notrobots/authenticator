package dev.notrobots.authenticator.ui.backupexportconfig

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityExportConfigBinding
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.ui.backupexportqr.ExportQRActivity
import dev.notrobots.authenticator.ui.backupexporttext.ExportTextActivity
import dev.notrobots.authenticator.util.AccountExporter

class ExportConfigActivity : AppCompatActivity() {
    private val binding by viewBindings<ActivityExportConfigBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)

        val accounts = intent.getSerializableExtra(EXTRA_ITEMS) as List<Account>

        title = "Export method"
        binding.toolbarLayout.toolbar.setNavigationOnClickListener {
            finish()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.exportOptions.addOption(
            "QR",
            "Export one or more QR codes containing the accounts. This will try and create as little qr codes as possible",
            R.drawable.ic_qr
        ) {
            val export = AccountExporter.exportQR(accounts)

            startActivity(ExportQRActivity::class) {
                putExtra(ExportQRActivity.EXTRA_QR_CODES, ArrayList(export))
            }
        }

        binding.exportOptions.addOption(
            "QR Plain",
            "Export each account a single QR code. All available OTP Authenticators should be able to import this code.",
            R.drawable.ic_qr
        ) {
            val export = AccountExporter.exportPlainQR(accounts)

            startActivity(ExportQRActivity::class) {
                putExtra(ExportQRActivity.EXTRA_QR_CODES, ArrayList(export))
            }
        }

        binding.exportOptions.addOption(
            "Plain text",
            "Export all accounts in a single text file, where each line is an account. All available OTP Authenticators should be able to import these accounts.",
            R.drawable.ic_file
        ) {
            val export = AccountExporter.exportUris(accounts)

            startActivity(ExportTextActivity::class) {
                putExtra(ExportTextActivity.EXTRA_URIS, ArrayList(export))
            }
        }

        binding.exportOptions.addOption(
            "Json",
            "Export all accounts and tags in a single json file. Some OTP Authenticators might be able to import this file.",
            R.drawable.ic_json
        ) {
//            val export = AccountExporter.exportJson(accounts)

//            startActivity(ExportTextActivity::class) {
//                putExtra(ExportTextActivity.EXTRA_URIS, ArrayList(export))
//            }
            makeToast("Not implemented yet")
        }

        binding.exportOptions.addOption(
            "Google Authenticator",
            "Export the accounts so that they can be imported by Google Authenticator.",
            R.drawable.ic_google
        ) {
//            val export = AccountExporter.exportGoogleAuthenticator(accounts)

//            startActivity(ExportTextActivity::class) {
//                putExtra(ExportTextActivity.EXTRA_URIS, ArrayList(export))
//            }
            makeToast("Not implemented yet")
        }
    }

    companion object {
        const val EXTRA_ITEMS = "ExportConfigActivity.ITEMS"
    }
}