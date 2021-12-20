package dev.notrobots.authenticator.ui.backupimport

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ItemImportOptionBinding

class ImportOptionAdapter(
    context: Context,
    items: List<ImportOption>
) : ArrayAdapter<ImportOption>(context, 0, items) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            R.layout.item_import_option,
            parent,
            false
        )
        val binding = ItemImportOptionBinding.bind(view)
        val item = getItem(position)!!

        binding.title.text = item.title
        binding.description.text = item.description
        binding.icon.setImageResource(item.icon)

        return binding.root
    }
}