package dev.notrobots.authenticator.ui.backupexporttext

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ItemExportTextBinding

class ExportTextAdapter(
    context: Context,
    uris: List<Uri>
) : ArrayAdapter<Uri>(context, 0, uris) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_export_text,
            parent,
            false
        )
        val binding = ItemExportTextBinding.bind(view)
        val uri = getItem(position)

        binding.rowNumber.text = "${position + 1}"
        binding.text.text = uri.toString()

        return binding.root
    }
}