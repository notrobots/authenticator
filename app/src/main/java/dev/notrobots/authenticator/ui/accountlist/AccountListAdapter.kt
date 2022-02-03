package dev.notrobots.authenticator.ui.accountlist

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.expandable.ExpandableDraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractExpandableItemViewHolder
import dev.notrobots.androidstuff.extensions.hide
import dev.notrobots.androidstuff.extensions.setDisabled
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.androidstuff.util.swap
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.KnownIssuers
import dev.notrobots.authenticator.databinding.ItemAccountBinding
import dev.notrobots.authenticator.databinding.ItemGroupBinding
import dev.notrobots.authenticator.extensions.find
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.util.ViewUtil
import kotlinx.android.synthetic.main.item_account.view.*
import kotlinx.android.synthetic.main.item_group.view.*
import java.util.*

private typealias ParentViewHolder = AccountListAdapter.GroupViewHolder
private typealias ChildViewHolder = AccountListAdapter.AccountViewHolder

class AccountListAdapter : AbstractExpandableItemAdapter<ParentViewHolder, ChildViewHolder>(), ExpandableDraggableItemAdapter<ParentViewHolder, ChildViewHolder> {
    private var listener: Listener = object : Listener {}
    private val handler = Handler(Looper.getMainLooper())
    var items = mutableListOf<GroupWithAccounts>()
        private set
    var editMode: EditMode = EditMode.Disabled
    var showPins: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()  //FIXME: Only notify accounts
        }
    var showIcons: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    val groups
        get() = items.map { it.group }
    val selectedGroups
        get() = groups.filter { it.isSelected }
    val selectedGroupCount
        get() = groups.count { it.isSelected }
    val accounts
        get() = items.flatMap { it.accounts }
    val selectedAccounts
        get() = accounts.filter { it.isSelected }
    val selectedAccountCount
        get() = accounts.count { it.isSelected }
    val selectedItemCount
        get() = selectedGroupCount + selectedAccountCount
    val hasSelectableItems
        get() = items.size > 1

    init {
        setHasStableIds(true)
    }

    override fun getGroupCount(): Int {
        return items.size
    }

    override fun getChildCount(groupPosition: Int): Int {
        return items[groupPosition].accounts.size
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groups[groupPosition].id
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return items[groupPosition].accounts[childPosition].id
    }

    override fun getGroupItemViewType(groupPosition: Int): Int {
        return VIEW_TYPE_GROUP
    }

    override fun getChildItemViewType(groupPosition: Int, childPosition: Int): Int {
        return VIEW_TYPE_ACCOUNT
    }

    //region Rendering

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder(R.layout.item_group, parent)
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return AccountViewHolder(R.layout.item_account, parent)
    }

    override fun onBindGroupViewHolder(holder: GroupViewHolder, groupPosition: Int, viewType: Int) {
        val group = groups[groupPosition]
        val view = holder.itemView
        val binding = holder.binding

        if (!group.isDefault) {
            view.isSelected = group.isSelected
            view.setOnClickListener {
                if (group.isDefault) {
                    return@setOnClickListener
                }

                if (editMode == EditMode.Item) {
                    group.toggleSelected()
                    view.isSelected = group.isSelected
                    selectAccounts(groupPosition, group.isSelected)
                }

                listener.onGroupClick(group, group.id, this)
            }
            view.setOnLongClickListener {
                if (group.isDefault) {
                    return@setOnLongClickListener false
                }

                if (editMode == EditMode.Disabled) {
                    group.toggleSelected()
                    view.isSelected = group.isSelected
                    selectAccounts(groupPosition, group.isSelected)
                }

                listener.onGroupLongClick(group, group.id, this)
            }

            binding.name.text = group.name
            binding.groupEdit.setDisabled(editMode != EditMode.Item)
            binding.edit.setOnClickListener {
                listener.onGroupEdit(group, group.id, this)
            }
        } else {
            binding.name.text = null
            binding.groupEdit.hide()
        }
    }

    override fun onBindChildViewHolder(holder: AccountViewHolder, groupPosition: Int, childPosition: Int, viewType: Int) {
        val view = holder.itemView
        val account = getAccount(groupPosition, childPosition)
        val binding = holder.binding
        val id = account.id
        val icon = KnownIssuers.find { k, _ ->
            val rgx = Regex(k, RegexOption.IGNORE_CASE)

            rgx.matches(account.issuer)
        }!!.value

        binding.icon.visibility = if (showIcons && editMode == EditMode.Disabled) View.VISIBLE else View.GONE
        binding.icon.setImageResource(icon)
        binding.pin.visibility = if (showPins) View.VISIBLE else View.INVISIBLE
        binding.name.text = account.displayName
        view.isSelected = account.isSelected
        view.setOnClickListener {
            if (editMode == EditMode.Item) {
                val group = groups.find { it.id == account.groupId }

                if (!group!!.isSelected) {
                    account.toggleSelected()
                    view.isSelected = account.isSelected
                }
            }

            listener.onItemClick(account, id, this)
        }
        view.setOnLongClickListener {
            if (editMode == EditMode.Disabled) {
                account.toggleSelected()
                view.isSelected = account.isSelected
            }

            listener.onItemLongClick(account, id, this)
        }

        if (editMode == EditMode.Item) {
            binding.edit.setOnClickListener {
                listener.onItemEdit(account, id, this)
            }
            binding.groupEdit.visibility = View.VISIBLE
            binding.groupTotp.visibility = View.GONE
            binding.groupHotp.visibility = View.GONE
        } else {
            binding.groupEdit.visibility = View.GONE

            when (account.type) {
                OTPType.TOTP -> {
                    binding.pin.text = OTPGenerator.generate(account)
                    binding.groupTotp.visibility = View.VISIBLE
                    binding.groupHotp.visibility = View.GONE
                }
                OTPType.HOTP -> {
                    //view.text_account_pin.text = "- - - - - -"           // "-".repeat(account.digits)
                    binding.nextPin.setOnClickListener {
                        it as ImageView

                        account.counter++
                        binding.pin.text = OTPGenerator.generate(account)
                        it.isEnabled = false
                        it.setTint(Color.LTGRAY)    //FIXME: Use the app's colors

                        handler.postDelayed({
                            it.isEnabled = true
                            it.setTint(Color.BLUE)
                        }, Account.HOTP_CODE_INTERVAL)

                        listener.onItemCounterIncrement(account, id, this)
                    }
                    binding.groupHotp.visibility = View.VISIBLE
                    binding.groupTotp.visibility = View.GONE
                }
            }
        }
    }

    //endregion

    //region Expand state

    override fun onCheckCanExpandOrCollapseGroup(holder: GroupViewHolder, groupPosition: Int, x: Int, y: Int, expand: Boolean): Boolean {
        // If the adapter is in Group or Item edit mode,
        // do no collapse/expand the groups
        if (editMode != EditMode.Disabled) {
            return false
        }

        // The default group cannot expand or collapse
        if (groups[groupPosition].isDefault) {
            return false
        }

        return true
    }

    override fun getInitialGroupExpandedState(groupPosition: Int): Boolean {
        val group = groups[groupPosition]

        // If the adapter is in Group edit mode,
        // collapse the groups so that they're easily movable
        //
        // Note: This won't collapse the group when calling notifyDataSetChanged(),
        // the group expand state must be changed using the RecyclerViewExpandableItemManager
//        if (editMode == EditMode.Group) {
//            return false
//        }

        // The default group is always expanded
        if (group.isDefault) {
            return true
        }

        return group.isExpanded
    }

    //endregion

    //region Drag&Drop

    override fun onCheckGroupCanStartDrag(holder: ParentViewHolder, groupPosition: Int, x: Int, y: Int): Boolean {
        // Item can only be dragged by its drag handle
        if (!ViewUtil.hitTest(holder.dragHandle, x, y)) {
            return false
        }

        // Item can only be dragged if the adapter is in Group edit mode
//        if (editMode != EditMode.Group) {
//            return false
//        }

        return true
    }

    override fun onCheckChildCanStartDrag(holder: ChildViewHolder, groupPosition: Int, childPosition: Int, x: Int, y: Int): Boolean {
        // Item can only be dragged by its drag handle
        if (!ViewUtil.hitTest(holder.dragHandle, x, y)) {
            return false
        }

        // Item can only be dragged if the adapter is in Item edit mode
        if (editMode != EditMode.Item) {
            return false
        }

        return true
    }

    override fun onGetGroupItemDraggableRange(holder: ParentViewHolder, groupPosition: Int): ItemDraggableRange? {
        return null
    }

    override fun onGetChildItemDraggableRange(holder: ChildViewHolder, groupPosition: Int, childPosition: Int): ItemDraggableRange? {
        return null
    }

    override fun onMoveGroupItem(fromGroupPosition: Int, toGroupPosition: Int) {
        val range = if (fromGroupPosition < toGroupPosition) {
            fromGroupPosition until toGroupPosition
        } else {
            fromGroupPosition downTo toGroupPosition + 1
        }
        val next = if (fromGroupPosition < toGroupPosition) 1 else -1

        for (i in range) {
            Collections.swap(items, i, i + next)
            swap(groups[i], groups[i + next], { it.order }, { g, v -> g.order = v })
        }

        listener.onGroupMoved(fromGroupPosition, toGroupPosition)
    }

    override fun onMoveChildItem(fromGroupPosition: Int, fromChildPosition: Int, toGroupPosition: Int, toChildPosition: Int) {
        val rows = mutableMapOf<Int, Int>()

        val fromGroup = items[fromGroupPosition]
        val toGroup = items[toGroupPosition]

        if (fromGroupPosition == toGroupPosition) {
            val range = if (fromChildPosition < toChildPosition) {
                fromChildPosition until toChildPosition
            } else {
                fromChildPosition downTo toChildPosition + 1
            }
            val next = if (fromChildPosition < toChildPosition) 1 else -1

            for (i in range) {
                Collections.swap(toGroup.accounts, i, i + next)
                swap(toGroup.accounts[i], toGroup.accounts[i + next], { it.order }, { g, v -> g.order = v })
            }
        } else {
            val item = fromGroup.accounts.removeAt(fromChildPosition)

            item.groupId = toGroup.group.id
            toGroup.accounts.add(toChildPosition, item)

            for (i in toChildPosition until toGroup.accounts.size) {
                toGroup.accounts[i].order = i + 1L
            }
        }

        listener.onItemMoved(rows)
    }

    override fun onCheckGroupCanDrop(draggingGroupPosition: Int, dropGroupPosition: Int): Boolean {
        return true
    }

    override fun onCheckChildCanDrop(draggingGroupPosition: Int, draggingChildPosition: Int, dropGroupPosition: Int, dropChildPosition: Int): Boolean {
        return true
    }

    override fun onGroupDragStarted(groupPosition: Int) {
        notifyDataSetChanged()
    }

    override fun onChildDragStarted(groupPosition: Int, childPosition: Int) {
        notifyDataSetChanged()
    }

    override fun onGroupDragFinished(fromGroupPosition: Int, toGroupPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    override fun onChildDragFinished(fromGroupPosition: Int, fromChildPosition: Int, toGroupPosition: Int, toChildPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    //endregion

    fun removeAccounts(groupPosition: Int, accounts: List<Account>): Boolean {
        return items[groupPosition].accounts.removeAll(accounts)
    }

    fun removeAccount(groupPosition: Int, account: Account): Boolean {
        return items[groupPosition].accounts.remove(account)
    }

    fun removeGroups(groups: List<AccountGroup>) {
        for (group in groups) {
            removeGroup(group)
        }
    }

    fun removeGroup(group: AccountGroup): Boolean {
        val groupWithAccounts = items.find { it.group == group }

        if (groupWithAccounts != null) {
            groupWithAccounts.accounts.clear()

            return items.remove(groupWithAccounts)
        }

        return false
    }

    fun getGroupPosition(groupId: Long): Int {
        return items.indexOfFirst { it.group.id == groupId }
    }

    fun getGroup(groupPosition: Int): AccountGroup {
        return groups[groupPosition]
    }

    fun getAccount(groupPosition: Int, accountPosition: Int): Account {
        return items[groupPosition].accounts[accountPosition]
    }

    fun getAccounts(groupPosition: Int): MutableList<Account> {
        return items[groupPosition].accounts
    }

    fun getAccounts(groupId: Long): MutableList<Account> {
        return getAccounts(groups.indexOfFirst { it.id == groupId })
    }

    fun setItems(items: List<GroupWithAccounts>) {
        this.items = items.toMutableList()
        notifyDataSetChanged()
    }

    fun selectAll() {
        for (item in items) {
            if (!item.group.isDefault) {
                item.group.isSelected = true
            }

            for (account in item.accounts) {
                account.isSelected = true
            }
        }

        notifyDataSetChanged()
    }

    fun clearSelected() {
        for (item in items) {
            if (!item.group.isDefault) {
                item.group.isSelected = false
            }

            for (account in item.accounts) {
                account.isSelected = false
            }
        }

        notifyDataSetChanged()
    }

    fun selectAccounts(groupPosition: Int, selected: Boolean) {
        for ((index, account) in items[groupPosition].accounts.withIndex()) {
            account.isSelected = selected
            listener.onItemSelected(account, groupPosition, index)
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    fun reorderAccounts(groupId: Long) {
        val group = items.find { it.group.id == groupId }

        for ((i, account) in group!!.accounts.withIndex()) {
            account.order = i + 1L
        }
    }

    fun reorderAccounts(groupPosition: Int) {
        for ((i, account) in items[groupPosition].accounts.withIndex()) {
            account.order = i + 1L
        }
    }

    fun reorderGroups() {
        for ((i, group) in groups.withIndex()) {
            if (group.order > 0) {
                group.order = i + 1L
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_GROUP = 0
        private const val VIEW_TYPE_ACCOUNT = 1
    }

    enum class EditMode {
        Disabled,
        Item,
//        Group
    }

    interface Listener {
        fun onGroupClick(group: AccountGroup, id: Long, adapter: AccountListAdapter) = Unit
        fun onGroupLongClick(group: AccountGroup, id: Long, adapter: AccountListAdapter): Boolean = false
        fun onGroupEdit(group: AccountGroup, id: Long, adapter: AccountListAdapter) = Unit
        fun onGroupMoved(fromGroupPosition: Int, toGroupPosition: Int) = Unit

        fun onItemClick(account: Account, id: Long, adapter: AccountListAdapter) = Unit
        fun onItemLongClick(account: Account, id: Long, adapter: AccountListAdapter): Boolean = false
        fun onItemEdit(account: Account, id: Long, adapter: AccountListAdapter) = Unit
        fun onItemCounterIncrement(account: Account, id: Long, adapter: AccountListAdapter) = Unit
        fun onItemMoved(updatedRows: Map<Int, Int>) = Unit
        fun onSelectAllItems(groupPosition: Int) = Unit

        fun onGroupSelected(group: AccountGroup, groupPosition: Int) = Unit
        fun onItemSelected(account: Account, groupPosition: Int, childPosition: Int) = Unit

        fun onItemRemoved(account: Account, groupPosition: Int, childPosition: Int) = Unit
        fun onGroupRemoved(group: AccountGroup, groupPosition: Int) = Unit
    }

    //region View holders

    abstract class BaseViewHolder<T>(layoutRes: Int, parent: ViewGroup) : AbstractExpandableItemViewHolder(
        LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
    ), DraggableItemViewHolder {
        private val dragState = DraggableItemState()
        val dragHandle = itemView.findViewById<View>(R.id.drag_handle)

        override fun setDragStateFlags(flags: Int) {
            dragState.flags = flags
        }

        override fun getDragStateFlags(): Int {
            return dragState.flags
        }

        override fun getDragState(): DraggableItemState {
            return dragState
        }
    }

    class AccountViewHolder(layoutRes: Int, parent: ViewGroup) : BaseViewHolder<Account>(layoutRes, parent) {
        val binding = ItemAccountBinding.bind(itemView)
    }

    class GroupViewHolder(layoutRes: Int, parent: ViewGroup) : BaseViewHolder<AccountGroup>(layoutRes, parent) {
        val binding = ItemGroupBinding.bind(itemView)
    }

    //endregion
}