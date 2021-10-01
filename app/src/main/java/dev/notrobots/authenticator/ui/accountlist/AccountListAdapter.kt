package dev.notrobots.authenticator.ui.accountlist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.EntryPoint
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.KnownIssuers
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPProvider
import dev.notrobots.authenticator.util.Coroutines
import dev.notrobots.authenticator.util.logd
import dev.notrobots.authenticator.util.swap
import kotlinx.android.synthetic.main.item_account.view.*
import java.util.*
import javax.inject.Inject

class AccountListAdapter : RecyclerView.Adapter<AccountListAdapter.ViewHolder>() {
    var accounts = emptyList<Account>()
    val selectedAccounts
        get() = accounts.filter { it.isSelected }
    var onItemClickListener: (item: Account, position: Int, id: Long) -> Unit = { _, _, _ -> }
    var onItemLongClickListener: (item: Account, position: Int, id: Long) -> Boolean = { _, _, _ -> true }
    var onItemEditListener: (item: Account) -> Unit = {}
    var editMode = false
    var touchHelper: ItemTouchHelper? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_account,
            parent,
            false
        )

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val view = holder.itemView
        val account = accounts[position]
        val id = account.id!!
        val icon = KnownIssuers.find { k, _ -> k.matches(account.issuer) }

        view.isSelected = account.isSelected
        view.text_account_label.text = account.displayName
        view.text_account_pin.text = OTPProvider.generate(account)
        view.pb_phase.visibility = if (editMode) View.GONE else View.VISIBLE
        view.img_account_edit.visibility = if (editMode) View.VISIBLE else View.GONE
        view.img_drag_handle.visibility = if (editMode) View.VISIBLE else View.GONE
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

    override fun getItemId(position: Int): Long {
        return accounts[position].id!!
    }

    override fun getItemCount(): Int {
        return accounts.size
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_ITEM
    }

    fun swap(from: Int, to: Int) {
        Collections.swap(accounts, from, to)

        if (from != to) {
            swap(accounts[from], accounts[to], { it.order }, { t, v -> t.order = v })
        }

        notifyItemMoved(from, to)
        notifyItemChanged(from)
        notifyItemChanged(to)
    }

    fun getItem(position: Int): Account {
        return accounts[position]
    }

    fun clearSelected() {
        selectedAccounts.forEach { it.isSelected = false }
        notifyDataSetChanged()
    }

    fun setSelected(position: Int, selected: Boolean) {
        accounts[position].isSelected = selected
    }

    fun setData(list: List<Account>) {
        if (accounts.isEmpty()) {
            accounts = list
            notifyItemRangeInserted(0, accounts.size)
        } else {
            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return accounts.size
                }

                override fun getNewListSize(): Int {
                    return list.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return accounts[oldItemPosition].id == list[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    val old = accounts[oldItemPosition]
                    val new = list[newItemPosition]

                    return old == new
                }
            })
            accounts = list
            result.dispatchUpdatesTo(this)
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}