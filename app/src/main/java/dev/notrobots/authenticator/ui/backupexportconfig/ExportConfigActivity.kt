package dev.notrobots.authenticator.ui.backupexportconfig

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.databinding.ActivityExportConfigBinding
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.ui.backupexportqr.ExportQRActivity
import dev.notrobots.authenticator.ui.backupexporttext.ExportTextActivity

class ExportConfigActivity : AppCompatActivity() {
    private val binding by viewBindings<ActivityExportConfigBinding>()
    private val qrStyles = mapOf(
        "Default" to QRCodeStyle.Default,
        "Inverted" to QRCodeStyle.Inverted,
        "Rainbow" to QRCodeStyle.Rainbow,
        "Trans" to QRCodeStyle.Trans
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val items = intent.getSerializableExtra(EXTRA_ITEMS) as List<BaseAccount>
        val groups = items.filterIsInstance<AccountGroup>()
        val accounts = items.filterIsInstance<Account>()

        title = "Export config"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.output.onSelectionChanged = { value, _ ->
            binding.qrStyle.visibility = if (value == BackupOutput.QR) View.VISIBLE else View.GONE
        }
        binding.format.setValues<BackupFormat>()
        binding.output.setValues<BackupOutput>()
        binding.qrStyle.entries = qrStyles.map { it.key }
        binding.qrStyle.values = qrStyles.map { it.value }
        binding.done.setOnClickListener {
            val data = AccountExporter.Data(groups, accounts).apply {
                format = binding.format.selectedValue as BackupFormat
            }

            when (binding.output.selectedValue) {
                BackupOutput.QR -> {
                    val export = AccountExporter().export(data, AccountExporter.QR_MAX_BYTES)

                    startActivity(ExportQRActivity::class) {
                        putExtra(ExportQRActivity.EXTRA_QR_CODES, ArrayList(export))
                        putExtra(ExportQRActivity.EXTRA_QR_STYLE, binding.qrStyle.selectedValue as QRCodeStyle)
                    }
                }
                BackupOutput.Text -> {
                    val export = AccountExporter().exportText(data)

                    startActivity(ExportTextActivity::class) {
                        putExtra(ExportTextActivity.EXTRA_TEXT, export)
                    }
                }

                else -> throw Exception("Unknown output format")
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        return false
    }

    companion object {
        const val EXTRA_ITEMS = "ExportConfigActivity.ITEMS"
    }
}