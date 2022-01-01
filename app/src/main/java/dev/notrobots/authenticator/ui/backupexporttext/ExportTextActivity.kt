package dev.notrobots.authenticator.ui.backupexporttext

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dev.notrobots.androidstuff.extensions.copyToClipboard
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.printHTML
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityExportTextBinding

class ExportTextActivity : AppCompatActivity() {
    private val saveText = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        it?.let {
            val stream = contentResolver.openOutputStream(it)

            try {
                stream!!.write(text.toByteArray())
                makeToast("Saved successfully")
            } catch (e: Exception) {
                makeToast("Cannot save file")
            } finally {
                stream?.close()
            }
        }
    }
    private val binding by viewBindings<ActivityExportTextBinding>()
    private var text: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        text = intent.getStringExtra(EXTRA_TEXT)!!
        binding.output.text = text
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_export_text, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.menu_export_copy -> {
                copyToClipboard(text)
                makeToast("Copied to clipboard")
            }
            R.id.menu_export_save -> {
                saveText.launch(getFilename())
            }
            R.id.menu_export_print -> {
                printHTML(text.replace("\n", "<br/>"))
            }
        }

        return true
    }

    private fun getFilename(): String {
        return "authenticator_txt_${now() / 100}.txt"
    }

    companion object {
        const val EXTRA_TEXT = "ExportTextActivity.URI_TEXT"
    }
}