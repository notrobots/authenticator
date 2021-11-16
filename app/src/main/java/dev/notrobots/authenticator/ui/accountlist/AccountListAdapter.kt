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
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.androidstuff.util.swap
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.KnownIssuers
import dev.notrobots.authenticator.extensions.find
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.util.ViewUtil
import kotlinx.android.synthetic.main.item_account.view.*
import kotlinx.android.synthetic.main.item_account_group.view.*
import kotlinx.android.synthetic.main.item_account_group.view.img_account_edit
import kotlinx.android.synthetic.main.item_account_group.view.img_drag_handle
import java.util.*

private typealias ParentViewHolder = AccountListAdapter.GroupViewHolder
private typealias ChildViewHolder = AccountListAdapter.AccountViewHolder

class AccountListAdapter : AbstractExpandableItemAdapter<ParentViewHolder, ChildViewHolder>(), ExpandableDraggableItemAdapter<ParentViewHolder, ChildViewHolder> {
    private var listener: Listener = object : Listener {}
    private var items = listOf<GroupWithAccounts>()
    private val handler = Handler(Looper.getMainLooper())
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
    val accounts
        get() = items.flatMap { it.accounts }
    val selectedAccounts
        get() = accounts.filter { it.isSelected }

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
        return GroupViewHolder(R.layout.item_account_group, parent)
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return AccountViewHolder(R.layout.item_account, parent)
    }

    override fun onBindGroupViewHolder(holder: GroupViewHolder, groupPosition: Int, viewType: Int) {
        val groupWithAccounts = items[groupPosition]
        val group = groups[groupPosition]
        val view = holder.itemView
        val childCount = getChildCount(groupPosition)

        view.text_group_account_count.visibility = if (editMode == EditMode.Disabled) View.VISIBLE else View.GONE
        view.text_group_account_count.text = if (childCount > 0) childCount.toString() else null
        view.img_group_select.visibility = if (editMode == EditMode.Item && !groupWithAccounts.isEmpty) View.VISIBLE else View.GONE
        view.img_group_select.setOnClickListener {
            for (account in groupWithAccounts.accounts) {
                account.isSelected = true
            }
            listener.onSelectAllItems(groupPosition)
        }

        if (!group.isDefault) {
            view.isSelected = group.isSelected
            view.setOnClickListener {
                if (!group.isDefault) {
                    if (editMode == EditMode.Group) {
                        group.toggleSelected()
                        view.isSelected = group.isSelected
                    }

                    listener.onGroupClick(group, group.id, this)
                }
            }
            view.setOnLongClickListener {
                if (!group.isDefault) {
                    if (editMode == EditMode.Disabled) {
                        group.toggleSelected()
                        view.isSelected = group.isSelected
                    }

                    listener.onGroupLongClick(group, group.id, this)
                } else {
                    false
                }
            }

            view.text_group_name.text = group.name
            view.img_drag_handle.visibility = if (editMode == EditMode.Group) View.VISIBLE else View.GONE
            view.img_account_edit.visibility = if (editMode == EditMode.Group) View.VISIBLE else View.GONE
            view.img_account_edit.setOnClickListener {
                listener.onGroupEdit(group, group.id, this)
            }
        } else {
            view.text_group_name.text = null
        }
    }

    override fun onBindChildViewHolder(holder: AccountViewHolder, groupPosition: Int, childPosition: Int, viewType: Int) {
        val view = holder.itemView
        val account = getAccount(groupPosition, childPosition)
        val id = account.id
        val icon = KnownIssuers.find { k, _ ->
            val rgx = Regex(k, RegexOption.IGNORE_CASE)

            rgx.matches(account.issuer)
        }!!.value

        view.img_account_icon.visibility = if (showIcons && editMode == EditMode.Disabled) View.VISIBLE else View.GONE
        view.text_account_pin.visibility = if (showPins) View.VISIBLE else View.INVISIBLE
        view.text_account_label.text = account.displayName
        view.img_account_icon.setImageResource(icon)
        view.isSelected = account.isSelected
        view.setOnClickListener {
            if (editMode == EditMode.Item) {
                account.toggleSelected()
                view.isSelected = account.isSelected
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
            view.img_account_edit.setOnClickListener {
                listener.onItemEdit(account, id, this)
            }
            view.group_account_edit.visibility = View.VISIBLE
            view.group_account_totp.visibility = View.GONE
            view.group_account_hotp.visibility = View.GONE
        } else {
            when (account.type) {
                OTPType.TOTP -> {
                    view.text_account_pin.text = OTPGenerator.generate(account)
                    view.group_account_totp.visibility = View.VISIBLE
                    view.group_account_hotp.visibility = View.GONE
                }
                OTPType.HOTP -> {
                    //view.text_account_pin.text = "- - - - - -"           // "-".repeat(account.digits)
                    view.img_account_counter_update.setOnClickListener {
                        it as ImageView

                        account.counter++
                        view.text_account_pin.text = OTPGenerator.generate(account)
                        it.isEnabled = false
                        it.setTint(Color.LTGRAY)    //FIXME: Use the app's colors

                        handler.postDelayed({
                            it.isEnabled = true
                            it.setTint(Color.BLUE)
                        }, Account.HOTP_CODE_INTERVAL)

                        listener.onItemCounterIncrement(account, id, this)
                    }
                    view.group_account_hotp.visibility = View.VISIBLE
                    view.group_account_totp.visibility = View.GONE
                }
            }

            view.group_account_edit.visibility = View.GONE
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
        if (editMode == EditMode.Group) {
            return false
        }

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
        if (editMode != EditMode.Group) {
            return false
        }

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
        this.items = items
        notifyDataSetChanged()
    }

    fun clearSelectedAccounts() {
        for (account in selectedAccounts) {
            account.isSelected = false
        }

        notifyDataSetChanged()
    }

    fun selectAllAccounts() {
        for (account in accounts) {
            account.isSelected = true
        }

        notifyDataSetChanged()
    }

    fun clearSelectedGroups() {
        for (group in selectedGroups) {
            group.isSelected = false
        }

        notifyDataSetChanged()
    }

    fun selectAllGroups() {
        for (group in groups) {
            if (!group.isDefault) {
                group.isSelected = true
            }
        }

        notifyDataSetChanged()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    companion object {
        private const val VIEW_TYPE_GROUP = 0
        private const val VIEW_TYPE_ACCOUNT = 1
    }

    enum class EditMode {
        Disabled,
        Item,
        Group
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
    }

    //region View holders

    abstract class BaseViewHolder<T>(layoutRes: Int, parent: ViewGroup) : AbstractExpandableItemViewHolder(
            LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
    ), DraggableItemViewHolder {
        private val dragState = DraggableItemState()
        val dragHandle = itemView.img_drag_handle

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

    class AccountViewHolder(layoutRes: Int, parent: ViewGroup) : BaseViewHolder<Account>(layoutRes, parent)
    class GroupViewHolder(layoutRes: Int, parent: ViewGroup) : BaseViewHolder<AccountGroup>(layoutRes, parent)

    //endregion
}