package dev.notrobots.authenticator.ui.backupexport

import android.content.Intent
import android.os.Bundle
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.util.loge
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.ExportFormat
import dev.notrobots.authenticator.models.ExportOutput
import kotlinx.android.synthetic.main.activity_export.*

class ExportActivity : ThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        title = "Export accounts"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.extras != null) {
            spinner_export_format.values = ExportFormat.values().toList()
            spinner_export_output.values = ExportOutput.values().toList()

            btn_export_confirm.setOnClickListener {
                val exportFormat = spinner_export_format.selectedValue as ExportFormat
                val exportOutput = spinner_export_output.selectedValue as ExportOutput
                val intent = Intent(this, ExportConfigActivity::class.java)

                intent.putExtra(ExportConfigActivity.EXTRA_EXPORT_FORMAT, exportFormat)
                intent.putExtra(ExportConfigActivity.EXTRA_EXPORT_OUTPUT, exportOutput)
                intent.putExtras(this.intent.extras!!)
                startActivity(intent)
            }
        } else {
            loge("Account list is null")
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}