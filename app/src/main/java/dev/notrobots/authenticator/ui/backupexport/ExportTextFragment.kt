package dev.notrobots.authenticator.ui.backupexport

import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.fragment.app.Fragment
import dev.notrobots.androidstuff.extensions.copyToClipboard
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.printHTML
import dev.notrobots.androidstuff.util.now
import dev.notrobots.authenticator.R
import kotlinx.android.synthetic.main.fragment_export_text.*

class ExportTextFragment : Fragment() {
    private val saveText = registerForActivityResult(CreateDocument()) {
        it?.let {
            val stream = requireContext().contentResolver.openOutputStream(it)

            try {
                stream!!.write(text.toByteArray())
                requireContext().makeToast("Saved successfully")
            } catch (e: Exception) {
                requireContext().makeToast("Cannot save file")
            } finally {
                stream?.close()
            }
        }
    }
    private val exportFileName
        get() = "authenticator_txt_${now() / 100}.txt"
    var text: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)

        return inflater.inflate(R.layout.fragment_export_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        text_export_output.text = text
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_export_text, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_export_copy -> {
                requireContext().copyToClipboard(text)
                requireContext().makeToast("Copied to clipboard")
            }
            R.id.menu_export_save -> {
                saveText.launch(exportFileName)
            }
            R.id.menu_export_print -> {
                requireContext().printHTML(text.replace("\n", "<br/>"))
            }
        }

        return true
    }
}