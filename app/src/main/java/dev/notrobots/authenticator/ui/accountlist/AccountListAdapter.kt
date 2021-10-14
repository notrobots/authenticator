package dev.notrobots.authenticator.ui.accountlist

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.KnownIssuers
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPProvider
import dev.notrobots.androidstuff.util.*
import dev.notrobots.authenticator.models.AccountGroup
import kotlinx.android.synthetic.main.item_account.view.*
import kotlinx.android.synthetic.main.item_account_group.view.text_account_group_name
import java.util.*

class AccountListAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    var group: AccountGroup? = null
    val selectedAccounts
        get() = group?.accounts?.filter { it.isSelected } ?: emptyList()

    //TODO: setListener
    var onItemClickListener: (item: Account, position: Int, id: Long) -> Unit = { _, _, _ -> }
    var onItemLongClickListener: (item: Account, position: Int, id: Long) -> Boolean = { _, _, _ -> true }
    var onItemEditListener: (item: Account) -> Unit = {}

    var editMode: EditMode = EditMode.Disabled
        set(value) {
            field = value
            //TODO: Notify
        }
    var isExpanded: Boolean = group?.isExpanded ?: true
        set(value) {
            field = value
            group!!.isExpanded = value
            if (value) {
                notifyItemRangeInserted(1, group!!.accounts.size)
            } else {
                notifyItemRangeRemoved(1, group!!.accounts.size)
            }
            notifyItemChanged(0)
        }
    var touchHelper: ItemTouchHelper? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflate = { layout: Int ->
            LayoutInflater.from(parent.context).inflate(
                layout,
                parent,
                false
            )
        }

        return when (viewType) {
            VIEW_TYPE_ITEM -> AccountViewHolder(inflate(R.layout.item_account))
            VIEW_TYPE_GROUP -> GroupViewHolder(inflate(R.layout.item_account_group))

            else -> error("Unknown view type")
        }
    }

    @SuppressLint("ClickableViewAccessibility") //FIXME: bad practice
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val view = holder.itemView

        when (holder) {
            is AccountViewHolder -> {
                val account = group!!.accounts[position - 1]
                val id = account.id!!
                val icon = KnownIssuers.find { k, _ -> k.matches(account.issuer) }

                view.isSelected = account.isSelected
                view.text_account_label.text = account.displayName
                view.text_account_pin.text = OTPProvider.generate(account)
                view.pb_phase.visibility = if (editMode == EditMode.Item) View.GONE else View.VISIBLE
                view.img_account_edit.visibility = if (editMode == EditMode.Item) View.VISIBLE else View.GONE
                view.img_drag_handle.visibility = if (editMode == EditMode.Item) View.VISIBLE else View.GONE
                view.img_drag_handle.setOnTouchListener { v, event ->
                    touchHelper?.startDrag(holder)

                    true
                }
                view.img_account_edit.setOnClickListener {
                    onItemEditListener(account)
                }
                view.setOnClickListener {
                    onItemClickListener(account, position, id)
                }
                view.setOnLongClickListener {
                    onItemLongClickListener(account, position, id)
                }
            }
            is GroupViewHolder -> {
                val group = group!!

                view.text_account_group_name.text = group.name
                view.img_account_edit.visibility = if (editMode == EditMode.Group) View.VISIBLE else View.GONE
                view.img_drag_handle.visibility = if (editMode == EditMode.Group) View.VISIBLE else View.GONE
                view.img_drag_handle.setOnTouchListener { v, event ->
                    touchHelper?.startDrag(holder)

                    true
                }
                view.isSelected = group.isSelected
                view.setOnClickListener {
                    if (editMode == EditMode.Group) {
                        group.toggleSelected()
                        view.isSelected = group.isSelected
                    }

                    if (editMode == EditMode.Disabled) {
                        isExpanded = !isExpanded
                    }
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return group?.get(position)?.id ?: -1
    }

    override fun getItemCount(): Int {
        return if (editMode == EditMode.Group || !isExpanded) 1 else (group?.accounts?.size ?: 0) + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_GROUP else VIEW_TYPE_ITEM
    }

    fun swap(from: Int, to: Int) {
        if (group != null) {
            Collections.swap(group!!.accounts, from - 1, to - 1)

            if (from != to) {
                swap(group!![from - 1], group!![to - 1], { it.order }, { t, v -> t.order = v })
            }

            notifyItemMoved(from, to)
            notifyItemChanged(from)
            notifyItemChanged(to)
        }
    }

    fun clearSelected() {
        selectedAccounts.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    fun setData(group: AccountGroup) {
        if (this.group == null) {
            this.group = group
            notifyItemRangeInserted(0, this.group!!.accounts.size)
        } else {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return this@AccountListAdapter.group!!.accounts.size
                }

                override fun getNewListSize(): Int {
                    return group.accounts.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return this@AccountListAdapter.group!!.accounts[oldItemPosition].id == group.accounts[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = this@AccountListAdapter.group!!.accounts[oldItemPosition]
                    val new = group.accounts[newItemPosition]

                    return old == new
                }
            })
            this.group = group
            result.dispatchUpdatesTo(this)
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_GROUP = 1
    }

    enum class EditMode {
        Disabled,
        Item,
        Group
    }

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}