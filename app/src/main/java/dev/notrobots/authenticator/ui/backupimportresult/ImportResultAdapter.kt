package dev.notrobots.authenticator.ui.backupimportresult

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.androidstuff.widget.BindableViewHolder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ItemImportResultBinding

class ImportResultAdapter : RecyclerView.Adapter<ImportResultAdapter.ViewHolder>() {
    private var items = listOf<ImportResult>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding
        val tint = ContextCompat.getColor(
            binding.root.context,
            when (item.importStrategy) {
                ImportStrategy.Default -> R.color.error
                ImportStrategy.Skip -> R.color.blue
                ImportStrategy.Replace -> R.color.warning
                ImportStrategy.KeepBoth -> R.color.success
            }
        )

        binding.name.text = item.title
        binding.icon.setImageResource(item.icon)
        binding.status.visibility = if (item.isDuplicate) View.VISIBLE else View.INVISIBLE
        binding.status.setImageResource(
            when (item.importStrategy) {
                ImportStrategy.Default -> R.drawable.ic_error
                ImportStrategy.Skip -> R.drawable.ic_skip
                ImportStrategy.Replace -> R.drawable.ic_replace
                ImportStrategy.KeepBoth -> R.drawable.ic_copy
            }
        )
        binding.status.setTint(tint)

        if (item.isDuplicate) {
            binding.root.setOnClickListener {
                val popupMenu = PopupMenu(binding.root.context, binding.root)
                popupMenu.inflate(R.menu.menu_import_strategy)

                val infoMenuItem = popupMenu.menu.findItem(R.id.menu_strategy_info)
                val title = SpannableString(
                    when (item.importStrategy) {
                        ImportStrategy.Default -> "Already exists"
                        ImportStrategy.Skip -> "Will be ignored"
                        ImportStrategy.Replace -> "Will replace the old one"    //TODO: Improve this text
                        ImportStrategy.KeepBoth -> "Will keep both"
                    }
                )

                title.setSpan(ForegroundColorSpan(tint), 0, title.length, 0)
                infoMenuItem.title = title

                popupMenu.setOnMenuItemClickListener {
                    when (it.itemId) {
                        R.id.menu_strategy_skip -> item.importStrategy = ImportStrategy.Skip
                        R.id.menu_strategy_replace -> item.importStrategy = ImportStrategy.Replace
                        R.id.menu_strategy_keep_both -> item.importStrategy = ImportStrategy.KeepBoth

                        else -> return@setOnMenuItemClickListener false
                    }

                    notifyItemChanged(position)
                    true
                }
                popupMenu.show()
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun setItems(items: List<ImportResult>) {
        this.items = items
        notifyDataSetChanged()
    }

    class ViewHolder(parent: ViewGroup) : BindableViewHolder<ItemImportResultBinding>(
        ItemImportResultBinding::class,
        parent
    )
}