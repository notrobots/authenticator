package dev.notrobots.authenticator.ui.backupexport

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.notrobots.androidstuff.widget.BindableViewHolder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ItemExportBinding
import dev.notrobots.authenticator.models.Account

class ExportAdapter(
    private val items: List<Account>
) : RecyclerView.Adapter<ExportAdapter.ViewHolder>() {
    private val checkedStates = mutableMapOf<Account, Boolean>()
    val checkedItems
        get() = checkedStates
            .filter { it.value }
            .map { it.key }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(R.layout.item_export, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        if (item !in checkedStates) {
            checkedStates[item] = false
        }

        binding.item.isChecked = checkedStates[item]!!
        binding.item.setOnCheckedChangeListener { _, value ->
            checkedStates[item] = value
        }
        binding.item.text = when (item) {
            is Account -> item.displayName

            else -> throw Exception("Type not supported")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(layoutRes: Int, parent: ViewGroup) : BindableViewHolder<ItemExportBinding>(
        layoutRes,
        parent,
        ItemExportBinding::class
    )
}