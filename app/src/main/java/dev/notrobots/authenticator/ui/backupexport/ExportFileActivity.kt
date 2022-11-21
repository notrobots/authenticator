package dev.notrobots.authenticator.ui.backupexport

import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityExportFileBinding
import dev.notrobots.authenticator.extensions.joinToStringIndexed
import dev.notrobots.authenticator.extensions.setHTMLText
import dev.notrobots.authenticator.util.BackupManager

class ExportFileActivity : AuthenticatorActivity() {
    private val saveText = registerForActivityResult(ActivityResultContracts.CreateDocument()) {
        it?.let {
            val stream = contentResolver.openOutputStream(it)

            try {
                stream!!.write(fileContent.toByteArray())
                stream.flush()
                makeToast("Saved successfully")
            } catch (e: Exception) {
                makeToast("Cannot save file")
            } finally {
                stream?.close()
            }
        }
    }
    private val binding by viewBindings<ActivityExportFileBinding>()
    private var fileContent = ""
    private var fileType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        finishOnBackPressEnabled = true

        fileContent = intent.getStringExtra(EXTRA_FILE_CONTENT) ?: ""
        fileType = intent.getIntExtra(EXTRA_FILE_TYPE, 0)

        if (fileContent.isBlank()) {
            makeToast("Exported file content is empty")
            finish()
            return
        }

        binding.filePreview.setHorizontallyScrolling(true)
        binding.filePreview.setHTMLText(styleFileContent(fileContent))
        binding.show.setOnClickListener {
            binding.filePreview.isGone = !binding.filePreview.isGone
            binding.show.title = if (binding.filePreview.isGone) "Show" else "Hide"
            binding.show.setIconResource(if (binding.filePreview.isGone) R.drawable.ic_eye else R.drawable.ic_eye_off)
        }
        binding.copy.setOnClickListener {
            copyToClipboard(fileContent)
            makeToast("Copied to clipboard")
        }
        binding.save.setOnClickListener {
            saveText.launch(getFilename())
        }
    }

    private fun styleFileContent(content: String): String {
        return when (fileType) {
            FILE_TYPE_TEXT -> {
                content.replace(Regex("\n"), "<br/>")
                    .replace("otpauth://", "<font color='#348ceb'>otpauth://</font>")
                    .replace(Regex("tag/", RegexOption.IGNORE_CASE), "<font color='#1f9c51'>tag/</font>")
                    .replace(Regex("totp/", RegexOption.IGNORE_CASE), "<font color='#1f9c51'>totp/</font>")
                    .replace(Regex("hotp/", RegexOption.IGNORE_CASE), "<font color='#1f9c51'>hotp/</font>")
//                val lines = styled.split("<br/>")
//                val indexLength = lines.size.toString().length

                //XXX: The indexes could be displayed with another textview,
                // this would allow the user to only copy the actual line and avoid copying the index
//                lines.joinToStringIndexed("<br/>") { index, s ->
//                    val i = index.toString().padStart(indexLength, '\u00A0') //FIXME: This space doesn't seem to be as big as a character
//
//                    //TODO: The line number color should be different for odd and even lines to improve readability
//                    "<span style=\"background-color:#a2a3a3; color:#ffffff\">$i.</span> $s"
//                    // style="background-color: #003300;"
//                }
            }
            FILE_TYPE_JSON -> content
                .replace(Regex("[{}\\[\\]]", RegexOption.IGNORE_CASE), "<font color='#348ceb'>$0</font>")
                .replace(Regex("\".+\"\\s?:", RegexOption.IGNORE_CASE), "<font color='#bf2c4e'>$0</font>")
                .replace("\n", "<br/>")

            else -> throw Exception("Unknown file type")
        }
    }

    private fun getFilename(): String {
        return when (fileType) {
            FILE_TYPE_TEXT -> BackupManager.textBackupFilename
            FILE_TYPE_JSON -> BackupManager.jsonBackupFilename

            else -> throw Exception("Unknown file type")
        }
    }

    companion object {
        const val EXTRA_FILE_CONTENT = "ExportTextActivity.FILE_CONTENT"
        const val EXTRA_FILE_TYPE = "ExportTextActivity.FILE_TYPE"
        const val FILE_TYPE_TEXT = 0
        const val FILE_TYPE_JSON = 100
    }
}