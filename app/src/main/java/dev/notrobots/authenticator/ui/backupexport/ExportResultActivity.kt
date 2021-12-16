package dev.notrobots.authenticator.ui.backupexport

import android.os.Bundle
import androidx.fragment.app.commit
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.*

class ExportResultActivity : ThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_detail)

        val exportOutput = intent.getSerializableExtra(EXTRA_EXPORT_OUTPUT) as BackupOutput
        val exportFormat = intent.getSerializableExtra(EXTRA_EXPORT_FORMAT) as BackupFormat
        val groups = intent.getSerializableExtra(ExportActivity.EXTRA_GROUP_LIST) as List<AccountGroup>
        val accounts = intent.getSerializableExtra(ExportActivity.EXTRA_ACCOUNT_LIST) as List<Account>
        val exporter = AccountExporter()    //TODO: AccountExporter could have static methods
        val exportData = AccountExporter.Data()

        exportData.groups.addAll(groups)
        exportData.accounts.addAll(accounts)
        exportData.format = exportFormat

        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 0F

        val fragment = when (exportOutput) {
            BackupOutput.Text -> ExportTextFragment().apply {
                text = exporter.exportText(exportData)
            }
            BackupOutput.QR -> ExportQRFragment().apply {
                qrCodes = exporter.exportQR(exportData)
            }
        }

        supportFragmentManager.commit {
            replace(R.id.export_fragment_container, fragment)
        }
    }

    companion object {
        const val EXTRA_EXPORT_FORMAT = "ExportResultActivity.EXPORT_FORMAT"
        const val EXTRA_EXPORT_OUTPUT = "ExportResultActivity.EXPORT_OUTPUT"
    }
}