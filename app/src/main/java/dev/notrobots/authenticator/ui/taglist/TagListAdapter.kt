package dev.notrobots.authenticator.ui.taglist

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.notrobots.androidstuff.extensions.disable
import dev.notrobots.androidstuff.extensions.setDisabled
import dev.notrobots.androidstuff.extensions.show
import dev.notrobots.authenticator.databinding.ItemTagBinding
import dev.notrobots.authenticator.models.Tag
import dev.notrobots.authenticator.widget.BindableViewHolder

class TagListAdapter : RecyclerView.Adapter<TagListAdapter.TagViewHolder>() {
    private var items = mutableListOf<Tag>()
    private var listener: Listener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        return TagViewHolder(parent)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val binding = holder.binding
        val tag = getItem(position)

        binding.name.text = tag.name
        binding.delete.setOnClickListener {
            listener?.onDelete(tag, tag.tagId, position)
        }
        binding.edit.setOnClickListener {
            listener?.onEdit(tag, tag.tagId, position)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getItem(position: Int): Tag {
        return items[position]
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<Tag>) {
        if (this.items.isEmpty()) {
            this.items.addAll(items)
            notifyDataSetChanged()
        } else {
            val oldList = this.items
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return oldList.size
                }

                override fun getNewListSize(): Int {
                    return items.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition] == items[newItemPosition]
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition] == items[newItemPosition]
                }
            })

            oldList.clear()
            oldList.addAll(items)
            result.dispatchUpdatesTo(this)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    class TagViewHolder(parent: ViewGroup) : BindableViewHolder<ItemTagBinding>(parent, ItemTagBinding::class)

    interface Listener {
        fun onDelete(tag: Tag, id: Long, position: Int)
        fun onEdit(tag: Tag, id: Long, position: Int)
    }
}