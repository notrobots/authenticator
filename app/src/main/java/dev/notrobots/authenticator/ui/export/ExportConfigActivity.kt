package dev.notrobots.authenticator.ui.export

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.commit
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.*

class ExportConfigActivity : ThemedActivity() {
    private var accounts: List<Account>? = null
    private val exporter by lazy {
        AccountExporter().apply {
            exportOutput = intent.getSerializableExtra(EXTRA_EXPORT_OUTPUT) as ExportOutput
            exportFormat = intent.getSerializableExtra(EXTRA_EXPORT_FORMAT) as ExportFormat
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export_detail)

        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.elevation = 0F
        accounts = intent.getSerializableExtra(EXTRA_ACCOUNT_LIST) as List<Account>

        val export = exporter.export(accounts!!)
        val fragment = when (exporter.exportOutput) {
            ExportOutput.Text -> ExportTextFragment().apply {
                text = export as String
            }
            ExportOutput.QR -> ExportQRFragment().apply {
                qrCodes = export as List<QRCode>
            }
        }

        supportFragmentManager.commit {
            replace(R.id.export_fragment_container, fragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {   //FIXME: This doesn't work!
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_EXPORT_FORMAT = "ExportConfigActivity.EXPORT_FORMAT"
        const val EXTRA_EXPORT_OUTPUT = "ExportConfigActivity.EXPORT_OUTPUT"
        const val EXTRA_ACCOUNT_LIST = "ExportConfigActivity.ACCOUNT_LIST"
    }
}