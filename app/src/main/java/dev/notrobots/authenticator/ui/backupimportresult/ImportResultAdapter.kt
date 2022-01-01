package dev.notrobots.authenticator.ui.backupimportresult

import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.androidstuff.widget.BindableViewHolder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ItemImportResultBinding
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import kotlinx.android.synthetic.main.item_import_result.view.*

class ImportResultAdapter : RecyclerView.Adapter<ImportResultAdapter.ViewHolder>() {
    private var items = listOf<ImportResult>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(R.layout.item_import_result, parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        binding.name.text = when (val i = item.item) {
            is Account -> i.displayName
            is AccountGroup -> i.name

            else -> null
        }
        binding.icon.setImageResource(
            when (item.item) {
                is Account -> R.drawable.ic_account
                is AccountGroup -> R.drawable.ic_group

                else -> 0
            }
        )
        binding.status.visibility = if (item.isDuplicate) View.VISIBLE else View.INVISIBLE
        binding.status.setImageResource(
            when (item.importStrategy) {
                ImportStrategy.Default -> R.drawable.ic_error
                ImportStrategy.Skip -> R.drawable.ic_skip
                ImportStrategy.Replace -> R.drawable.ic_replace
//                ImportStrategy.KeepBoth -> R.drawable.ic_copy
            }
        )
        binding.status.setTint(
            ContextCompat.getColor(
                binding.root.context,
                when (item.importStrategy) {
                    ImportStrategy.Default -> R.color.error
                    ImportStrategy.Skip -> R.color.blue
                    ImportStrategy.Replace -> R.color.warning
//                    ImportStrategy.KeepBoth -> R.color.blue
                }
            )
        )
        binding.status.setOnClickListener {
            val popupMenu = PopupMenu(binding.root.context, binding.root)

            popupMenu.inflate(R.menu.menu_import_strategy)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_strategy_skip -> item.importStrategy = ImportStrategy.Skip
                    R.id.menu_strategy_replace -> item.importStrategy = ImportStrategy.Replace
//                    R.id.menu_strategy_keep_both -> item.importStrategy = ImportStrategy.KeepBoth

                    else -> return@setOnMenuItemClickListener false
                }

                notifyItemChanged(position)
                true
            }
            popupMenu.show()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: List<ImportResult>) {
        this.items = items
        notifyDataSetChanged()
    }

    class ViewHolder(layoutRes: Int, parent: ViewGroup) : BindableViewHolder<ItemImportResultBinding>(
        layoutRes,
        parent,
        ItemImportResultBinding::class
    )
}