package dev.notrobots.authenticator.ui.export

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ViewFlipper
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.util.loge
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountExporter
import dev.notrobots.authenticator.models.ExportFormat
import dev.notrobots.authenticator.models.ExportOutput
import kotlinx.android.synthetic.main.activity_export.*

class ExportAccountsActivity : ThemedActivity() {
    private var accounts: List<Account>? = null
    private val exporter = AccountExporter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        title = "Export accounts"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        accounts = intent.getSerializableExtra(EXTRA_ACCOUNT_LIST) as List<Account>

        if (accounts == null) {
            loge("Account list is null")
            finish()
        }

        spinner_export_1.values = ExportFormat.values().toList()
        spinner_export_2.values = ExportOutput.values().toList()

        btn_export_confirm.setOnClickListener {
            exporter.exportFormat = spinner_export_1.selectedValue as ExportFormat
            exporter.exportOutput = spinner_export_2.selectedValue as ExportOutput

            val export = exporter.export(accounts!!)

            if (exporter.exportOutput == ExportOutput.QR) {
                export as List<Bitmap>

                frame_export.showView(R.id.image_export_output)
                image_export_output.clearImages()
                image_export_output.setImageBitmaps(export)
            } else {
                frame_export.showView(R.id.text_export_output)
                text_export_output.text = export as String
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_ACCOUNT_LIST = "ExportAccountsActivity.ACCOUNT_LIST"
    }
}