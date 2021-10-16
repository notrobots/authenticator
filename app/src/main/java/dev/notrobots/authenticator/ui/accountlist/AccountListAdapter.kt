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
import dev.notrobots.authenticator.models.GroupWithAccounts
import kotlinx.android.synthetic.main.item_account.view.*
import kotlinx.android.synthetic.main.item_account_group.view.text_group_name
import java.util.*

class AccountListAdapter(var groupWithAccounts: GroupWithAccounts) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var listener: Listener? = null
    val selectedAccounts
        get() = groupWithAccounts.accounts.filter { it.isSelected }
    var editMode: EditMode = EditMode.Disabled
    var isExpanded: Boolean
        get() = groupWithAccounts.group.isExpanded
        set(value) {
            groupWithAccounts.group.isExpanded = value
            if (value) {
                notifyItemRangeInserted(1, groupWithAccounts.accounts.size)
            } else {
                notifyItemRangeRemoved(1, groupWithAccounts.accounts.size)
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
                val account = groupWithAccounts.accounts[position - 1]
                val id = account.id
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
                    listener?.onEdit(account, position, id)
                }
                view.setOnClickListener {       //FIXME: Selection state should be changed here to improve performance
                    listener?.onClick(account, position, id)
                }
                view.setOnLongClickListener {
                    listener?.onLongClick(account, position, id) ?: false
                }
            }
            is GroupViewHolder -> {
                val group = groupWithAccounts.group

                view.text_group_name.text = group.name
                view.img_account_edit.visibility = if (editMode == EditMode.Group && !group.isDefault) View.VISIBLE else View.GONE
                view.img_account_edit.setOnClickListener {
                    listener?.onEdit(group, position, group.id)
                }
                view.img_drag_handle.visibility = if (editMode == EditMode.Group && !group.isDefault) View.VISIBLE else View.GONE
                view.img_drag_handle.setOnTouchListener { v, event ->
                    touchHelper?.startDrag(holder)

                    true
                }
                view.isSelected = group.isSelected

                if (!group.isDefault) {
                    view.setOnClickListener {
                        if (editMode == EditMode.Group) {
                            group.toggleSelected()
                            view.isSelected = group.isSelected
                        }

                        if (editMode == EditMode.Disabled) {
                            isExpanded = !isExpanded
                        }

                        listener?.onClick(group, position, group.id)
                    }
                    view.setOnLongClickListener {
                        listener?.onLongClick(group, position, group.id) ?: false
                    }
                } else {
                    if (editMode == EditMode.Group) {
                        view.text_group_name.text = "Default group"
                    }
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return when (position) {
            0 -> groupWithAccounts.group.id

            else -> groupWithAccounts.accounts[position].id
        }
    }

    override fun getItemCount(): Int {
        return when {
            editMode == EditMode.Group || !isExpanded -> 1

            else -> (groupWithAccounts.accounts.size ?: 0) + 1
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_GROUP else VIEW_TYPE_ITEM
    }

    fun swap(from: Int, to: Int) {
        val accounts = groupWithAccounts.accounts

        Collections.swap(groupWithAccounts.accounts, from - 1, to - 1)

        if (from != to) {
            swap(accounts[from - 1], accounts[to - 1], { it.order }, { t, v -> t.order = v })
        }

        notifyItemMoved(from, to)
        notifyItemChanged(from) //TODO: Check if this is needed
        notifyItemChanged(to)
    }

    fun clearSelected() {
        selectedAccounts.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    fun setData(groupWithAccounts: GroupWithAccounts) {
        if (this.groupWithAccounts.accounts.isEmpty()) {
            this.groupWithAccounts = groupWithAccounts
            notifyItemRangeInserted(0, this.groupWithAccounts.accounts.size)
        } else {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return this@AccountListAdapter.groupWithAccounts.accounts.size
                }

                override fun getNewListSize(): Int {
                    return groupWithAccounts.accounts.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return this@AccountListAdapter.groupWithAccounts.accounts[oldItemPosition].id == groupWithAccounts.accounts[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = this@AccountListAdapter.groupWithAccounts.accounts[oldItemPosition]
                    val new = groupWithAccounts.accounts[newItemPosition]

                    return old == new
                }
            })
            this.groupWithAccounts = groupWithAccounts
            result.dispatchUpdatesTo(this)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_GROUP = 1
    }

    interface Listener {
        fun onClick(account: Account, position: Int, id: Long)
        fun onLongClick(account: Account, position: Int, id: Long): Boolean
        fun onEdit(account: Account, position: Int, id: Long)
        fun onClick(group: AccountGroup, position: Int, id: Long)
        fun onLongClick(group: AccountGroup, position: Int, id: Long): Boolean
        fun onEdit(group: AccountGroup, position: Int, id: Long)
    }

    enum class EditMode {
        Disabled,
        Item,
        Group
    }

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}