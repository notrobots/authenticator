package dev.notrobots.authenticator.ui.backupexporttext

import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityExportTextBinding

class ExportTextActivity : AppCompatActivity() {
    private val saveText = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        it?.let {
            val stream = contentResolver.openOutputStream(it)

            try {
                stream!!.write(uris.joinToString("\n").toByteArray())
                makeToast("Saved successfully")
            } catch (e: Exception) {
                makeToast("Cannot save file")
            } finally {
                stream?.close()
            }
        }
    }
    private val binding by viewBindings<ActivityExportTextBinding>()
    private lateinit var uris: List<Uri>
    private var hasInteracted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            showExitWarning()
        }
        uris = intent.getSerializableExtra(EXTRA_URIS) as List<Uri>

        binding.listOutput.addOption(
            "Show",
            "Show the backup data",
            R.drawable.ic_eye
        ) {
            if (uris.size > 1) {
                val adapter = ExportTextAdapter(this, uris)

                showList(null, adapter, itemClickListener = {
                    copyToClipboard(it)
                    makeToast("Copied to clipboard")
                })
            } else {
                showInfo(null, uris[0])
            }
        }
        binding.listOutput.addOption(
            "Copy",
            "Copy the backup data to the clipboard",
            R.drawable.ic_copy
        ) {
            copyToClipboard(uris.joinToString("\n"))
            makeToast("Copied to clipboard")
            hasInteracted = true
        }
        binding.listOutput.addOption(
            "Save",
            "Save the backup data to the storage",
            R.drawable.ic_save
        ) {
            saveText.launch(getFilename())
            hasInteracted = true
        }
    }

    override fun onBackPressed() {
        showExitWarning()
    }

    private fun showExitWarning() {
        if (!hasInteracted) {
            showChoice(
                "Go back",
                "Are you sure you want to go back before saving/printing your backup?",
                "Yes",
                {
                    finish()
                },
                "No"
            )
        } else {
            finish()
        }
    }

    private fun getFilename(): String {
        return "authenticator_txt_${now() / 100}.txt"
    }

    companion object {
        const val EXTRA_URIS = "ExportTextActivity.URI_URIS"
    }
}