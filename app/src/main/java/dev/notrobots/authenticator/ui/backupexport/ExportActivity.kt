package dev.notrobots.authenticator.ui.backupexport

import android.content.Intent
import android.os.Bundle
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.util.loge
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.BackupFormat
import dev.notrobots.authenticator.models.BackupOutput
import kotlinx.android.synthetic.main.activity_export.*

class ExportActivity : ThemedActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_export)

        title = "Export accounts"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.extras != null) {
            spinner_export_format.values = BackupFormat.values().toList()
            spinner_export_output.values = BackupOutput.values().toList()

            btn_export_confirm.setOnClickListener {
                val exportFormat = spinner_export_format.selectedValue as BackupFormat
                val exportOutput = spinner_export_output.selectedValue as BackupOutput
                val intent = Intent(this, ExportResultActivity::class.java)

                intent.putExtra(ExportResultActivity.EXTRA_EXPORT_FORMAT, exportFormat)
                intent.putExtra(ExportResultActivity.EXTRA_EXPORT_OUTPUT, exportOutput)
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

    companion object {
        const val EXTRA_ACCOUNT_LIST = "ExportActivity.ACCOUNT_LIST"
        const val EXTRA_GROUP_LIST = "ExportActivity.GROUP_LIST"
    }
}