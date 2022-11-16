package dev.notrobots.authenticator.ui.backupexport

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityExportConfigBinding
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import dev.notrobots.authenticator.ui.backupexportqr.ExportQRActivity
import dev.notrobots.authenticator.ui.backupexporttext.ExportTextActivity
import dev.notrobots.authenticator.util.BackupManager
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExportActivity : AuthenticatorActivity() {
    private val binding by viewBindings<ActivityExportConfigBinding>()
    private val viewModel by viewModels<AccountListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        finishOnBackPressEnabled = true

        lifecycleScope.launch {
            val accounts = viewModel.accountDao.getAccounts()
            val accountsWithTags = viewModel.accountDao.getAccountsWithTags()
            val tags = viewModel.tagDao.getTags()

            binding.exportOptionsQr.setOnClickListener {
                val backup = BackupManager.exportQR(accounts, accountsWithTags, tags)

                startActivity(ExportQRActivity::class) {
                    putExtra(ExportQRActivity.EXTRA_QR_CODES, ArrayList(backup))
                    putExtra(ExportQRActivity.EXTRA_QR_TYPE, ExportQRActivity.QR_TYPE_DEFAULT)
                }
            }
            binding.exportOptionsGoogleAuthenticator.setOnClickListener {
                val backup = BackupManager.exportGoogleAuthenticator(accounts)

                startActivity(ExportQRActivity::class) {
                    putExtra(ExportQRActivity.EXTRA_QR_CODES, ArrayList(backup))
                    putExtra(ExportQRActivity.EXTRA_QR_TYPE, ExportQRActivity.QR_TYPE_GOOGLE_AUTHENTICATOR)
                }
            }
            binding.exportOptionsText.setOnClickListener {
                val backup = BackupManager.exportPlainText(accounts, accountsWithTags, tags)

                startActivity(ExportFileActivity::class) {
                    putExtra(ExportFileActivity.EXTRA_FILE_CONTENT, backup)
                    putExtra(ExportFileActivity.EXTRA_FILE_TYPE, ExportFileActivity.FILE_TYPE_TEXT)
                }
            }
            binding.exportOptionsJson.setOnClickListener {
                val backup = BackupManager.exportJson(accounts, accountsWithTags, tags, mapOf()).toString(4)

                startActivity(ExportFileActivity::class) {
                    putExtra(ExportFileActivity.EXTRA_FILE_CONTENT, backup)
                    putExtra(ExportFileActivity.EXTRA_FILE_TYPE, ExportFileActivity.FILE_TYPE_JSON)
                }
            }
        }

        viewModel.accounts.observe(this) {
            val accounts = it



//            binding.exportOptions.addOption(
//                "QR",
//                "Export one or more QR codes. This will try and create as little qr codes as possible.\nExported items: + Accounts",
//                R.drawable.ic_qr
//            ) {
//                val export = BackupManager.exportQR(accounts)
//
//                startActivity(ExportQRActivity::class) {
//                    putExtra(ExportQRActivity.EXTRA_QR_CODES, ArrayList(export))
//                }
//            }
//
//            binding.exportOptions.addOption(
//                "QR Plain",
//                "Export each account a single QR code. All available OTP Authenticators should be able to import this code.",
//                R.drawable.ic_qr
//            ) {
//                val export = BackupManager.exportPlainQR(accounts)
//
//                startActivity(ExportQRActivity::class) {
//                    putExtra(ExportQRActivity.EXTRA_QR_CODES, ArrayList(export))
//                }
//            }
//
//            binding.exportOptions.addOption(
//                "Plain text",
//                "Export all accounts in a single text file, where each line is an account. All available OTP Authenticators should be able to import these accounts.",
//                R.drawable.ic_file
//            ) {
//                val export = BackupManager.exportUris(accounts)
//
//                startActivity(ExportTextActivity::class) {
//                    putExtra(ExportTextActivity.EXTRA_URIS, ArrayList(export))
//                }
//            }
//
//            binding.exportOptions.addOption(
//                "Json",
//                "Export all accounts and tags in a single json file. Some OTP Authenticators might be able to import this file.",
//                R.drawable.ic_json
//            ) {
//            val export = AccountExporter.exportJson(accounts)

//            startActivity(ExportTextActivity::class) {
//                putExtra(ExportTextActivity.EXTRA_URIS, ArrayList(export))
//            }
//                makeToast("Not implemented yet")
//            }

//            binding.exportOptions.addOption(
//                "Google Authenticator",
//                "Export the accounts so that they can be imported by Google Authenticator.",
//                R.drawable.ic_google
//            ) {
//            val export = AccountExporter.exportGoogleAuthenticator(accounts)

//            startActivity(ExportTextActivity::class) {
//                putExtra(ExportTextActivity.EXTRA_URIS, ArrayList(export))
//            }
//                makeToast("Not implemented yet")
//            }
        }
    }
}