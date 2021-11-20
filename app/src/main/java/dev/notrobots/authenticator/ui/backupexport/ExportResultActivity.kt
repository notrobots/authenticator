package dev.notrobots.authenticator.ui.backupexport

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.commit
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.*

class ExportResultActivity : ThemedActivity() {
    private val exporter by lazy {
        AccountExporter().apply {
            exportOutput = intent.getSerializableExtra(EXTRA_EXPORT_OUTPUT) as ExportOutput
            exportFormat = intent.getSerializableExtra(EXTRA_EXPORT_FORMAT) as ExportFormat
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_detail)

        val groups = intent.getSerializableExtra(ExportActivity.EXTRA_GROUP_LIST) as List<AccountGroup>
        val accounts = intent.getSerializableExtra(ExportActivity.EXTRA_ACCOUNT_LIST) as List<Account>

        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 0F

        val fragment = when (exporter.exportOutput) {
            ExportOutput.Text -> ExportTextFragment().apply {
                text = exporter.exportText(accounts, groups)
            }
            ExportOutput.QR -> ExportQRFragment().apply {
                qrCodes = exporter.exportQR(accounts, groups)
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