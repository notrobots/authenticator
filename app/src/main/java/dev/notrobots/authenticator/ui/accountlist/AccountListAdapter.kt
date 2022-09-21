package dev.notrobots.authenticator.ui.accountlist

import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemAdapter
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemState
import com.h6ah4i.android.widget.advrecyclerview.draggable.DraggableItemViewHolder
import com.h6ah4i.android.widget.advrecyclerview.draggable.ItemDraggableRange
import com.h6ah4i.android.widget.advrecyclerview.utils.AbstractDraggableItemViewHolder
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.swap
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.KnownIssuers
import dev.notrobots.authenticator.databinding.ItemAccountBinding
import dev.notrobots.authenticator.extensions.toDp
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.models.TotpTimer
import dev.notrobots.authenticator.ui.accountlist.AccountListAdapter.AccountViewHolder
import dev.notrobots.authenticator.util.OTPGenerator
import dev.notrobots.authenticator.util.ViewUtil
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

class AccountListAdapter : RecyclerView.Adapter<AccountViewHolder>(), DraggableItemAdapter<AccountViewHolder> {
    private var listener: Listener = object : Listener {}
    private val handler = Handler(Looper.getMainLooper())
    var items = mutableListOf<Account>()    //FIXME: private
        private set
    var editMode: Boolean = false
    var showPins: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var sortMode: SortMode = SortMode.Custom
    var totpIndicatorType: TotpIndicatorType = TotpIndicatorType.Text
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var showIcons: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    val selectedItems = mutableSetOf<Account>()
    val selectedItemCount
        get() = selectedItems.size
    var totpTimer: TotpTimer? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].id
    }

    override fun getItemViewType(position: Int): Int {
        val item = items[position]

        return when (item.type) {
            OTPType.TOTP -> VIEW_TYPE_TOTP
            OTPType.HOTP -> VIEW_TYPE_HOTP
        }
    }

    //region Rendering

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return when (viewType) {
            VIEW_TYPE_HOTP -> AccountViewHolder(parent)
            VIEW_TYPE_TOTP -> TimerAccountViewHolder(parent)

            else -> throw Exception("Unkown view type")
        }
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val view = holder.itemView
        val account = items[position]
        val binding = holder.binding
        val id = account.id
        val icon = getIssuerIcon(account.issuer)

        binding.icon.visibility = if (showIcons && !editMode) View.VISIBLE else View.GONE
        binding.icon.setImageResource(icon)
        binding.pin.visibility = if (showPins && !editMode) View.VISIBLE else View.GONE
        binding.dragHandle.visibility = if (editMode) View.VISIBLE else View.GONE

        updateViewMarginsAndConstraints(binding.icon)
        updateViewMarginsAndConstraints(binding.dragHandle)

        // Label & Name
        if (account.label.isEmpty()) {
            binding.label.text = account.name
            binding.name.disable()
        } else {
            binding.name.text = account.name
            binding.label.text = account.label
            binding.name.show()
        }

        // Click events
        view.setOnClickListener {
            if (editMode) {
                // Toggle account selection state
                if (account in selectedItems) {
                    selectedItems.remove(account)
                } else {
                    selectedItems.add(account)
                }
                view.isSelected = account in selectedItems

                listener.onItemSelectionChange(account, position, account.id, this)
            } else {
                listener.onItemClick(account, position, id, this)
            }
        }
        view.setOnLongClickListener {
            if (!editMode) {
                selectedItems.add(account)
                view.isSelected = true
                listener.onItemSelectionChange(account, position, account.id, this)
            }

            listener.onItemLongClick(account, position, id, this)
        }

        if (editMode) {
            // Restore selection state or get it from the account instance
            view.isSelected = account in selectedItems

            binding.edit.setOnClickListener {
                listener.onItemEditClick(account, position, id, this)
            }
            binding.indicators.showView(R.id.edit)
        } else {
            // Clear selection, accounts can only be selected in edit mode
            view.isSelected = false

            when (account.type) {
                OTPType.TOTP -> {
                    holder as TimerAccountViewHolder
                    holder.setCounter(account)
                    binding.pin.text = generatePin(account)

                    when (totpIndicatorType) {
                        TotpIndicatorType.Circular -> {
                            binding.indicators.showView(R.id.totp_circular_indicator)
                            binding.totpCircularIndicator.max = TimeUnit.SECONDS.toMillis(account.period).toInt()
                            holder.totpCounter?.getTimeUntilNextCounter()?.let {
                                binding.pin.text = generatePin(account)
                                binding.totpCircularIndicator.progress = it.toInt()
                            }
                        }
                        TotpIndicatorType.Text -> {
                            binding.indicators.showView(R.id.totp_text_indicator)
                            holder.totpCounter?.getTimeUntilNextCounter()?.let {
                                binding.pin.text = generatePin(account)
                                binding.totpTextIndicator.text = ceil(it / 1000F).toInt().toString()
                            }
                        }
                    }
                }
                OTPType.HOTP -> {
                    val primaryColor = view.context.resolveColorAttribute(R.attr.colorPrimary)
                    val textColorSecondary = view.context.resolveColorAttribute(android.R.attr.textColorSecondary)

//                    binding.pin.text = "- ".repeat(account.digits)
                    binding.pin.text = generatePin(account)
                    binding.indicators.showView(R.id.hotp_indicator)
                    binding.hotpIndicator.setOnClickListener {
                        it as ImageView

                        account.counter++
                        binding.pin.text = generatePin(account)
                        it.isEnabled = false
                        it.setTint(textColorSecondary)

                        handler.postDelayed({
                            it.isEnabled = true
                            it.setTint(primaryColor)
                        }, Account.HOTP_CODE_INTERVAL)

                        listener.onItemHOTPCounterChange(account, position, id, this)
                    }
                }
            }
        }
    }

    //endregion

    //region Drag&Drop

    override fun onCheckCanStartDrag(holder: AccountViewHolder, position: Int, x: Int, y: Int): Boolean {
        // Item can only be dragged if the adapter is in Item edit mode
        if (!editMode) {
            return false
        }

        // Item can only be dragged by its drag handle
        if (!ViewUtil.hitTest(holder.dragHandle, x, y)) {
            return false
        }

        // Item can only be dragged if the sort mode is set to Custom
        if (sortMode != SortMode.Custom) {
            holder.itemView.context.makeToast("Change sort mode to Custom to reorder the items")
            return false
        }

        return true
    }

    override fun onGetItemDraggableRange(holder: AccountViewHolder, position: Int): ItemDraggableRange? {
        return null
    }

    override fun onMoveItem(fromPosition: Int, toPosition: Int) {
        val range = if (fromPosition < toPosition) {
            fromPosition until toPosition
        } else {
            fromPosition downTo toPosition + 1
        }
        val next = if (fromPosition < toPosition) 1 else -1

        for (i in range) {
            Collections.swap(items, i, i + next)
            swap(items[i], items[i + next], { it.order }, { g, v -> g.order = v })
        }

        listener.onItemMoved(fromPosition, toPosition)
    }

    override fun onCheckCanDrop(draggingPosition: Int, dropPosition: Int): Boolean {
        return true
    }

    override fun onItemDragStarted(position: Int) {
        notifyDataSetChanged()
    }

    override fun onItemDragFinished(fromPosition: Int, toPosition: Int, result: Boolean) {
        notifyDataSetChanged()
    }

    //endregion

    fun setItems(items: List<Account>) {
        if (this.items.isEmpty()) {
            this.items = items.toMutableList()
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
                    return oldList[oldItemPosition].id == items[newItemPosition].id
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition] == items[newItemPosition]
                }
            })

            this.items.clear()
            this.items.addAll(items)
            result.dispatchUpdatesTo(this)
        }
    }

    fun selectAll() {
        selectedItems.addAll(items)
        notifyDataSetChanged()
    }

    fun clearSelected() {
        selectedItems.clear()
        notifyDataSetChanged()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    /**
     * Updates the given [view]'s margins and constraints based on the
     * app's state
     */
    private fun updateViewMarginsAndConstraints(view: View) {
        view.updateLayoutParams<ConstraintLayout.LayoutParams> {
            topMargin = if (showPins) 8.toDp().toInt() else 0
            verticalBias = if (showPins) 0F else 0.5F
        }
    }

    /**
     * Generates the current pin for the given [account]
     */
    private fun generatePin(account: Account): String {
        return formatPin(OTPGenerator.generate(account))
    }

    /**
     * Returns the given [pin] formatted based on the given [divider] and [groupSize]
     */
    private fun formatPin(
        pin: String,
        groupSize: Int = 3,
        divider: String = " "
    ): String {
        val rgx = Regex(".{1,${groupSize}}+")

        return pin.replace(rgx, "$0$divider").trim()
    }

    /**
     * Returns the resource used for the given [issuer]
     */
    private fun getIssuerIcon(issuer: String): Int {
        val key = KnownIssuers.keys.find {
            val rgx = Regex(it, RegexOption.IGNORE_CASE)

            rgx.matches(issuer)
        }

        return KnownIssuers[key]!!
    }

    companion object {
        const val VIEW_TYPE_HOTP = 0
        const val VIEW_TYPE_TOTP = 1
        private val GROUP_SIZES = mapOf(
            4 to 4,
            6 to 3,
            8 to 4,
            10 to 5,
            12 to 4,
            14 to 2
        )
    }

    interface Listener {
        /**
         * Invoked when an item is clicked.
         *
         * This is only invoked when [editMode] is set to false.
         */
        fun onItemClick(account: Account, position: Int, id: Long, adapter: AccountListAdapter) = Unit

        /**
         * Invoked when an item is long clicked.
         */
        fun onItemLongClick(account: Account, position: Int, id: Long, adapter: AccountListAdapter): Boolean = false

        /**
         * Invoked when an item needs to be edited.
         *
         * This is only invoked when [editMode] is set to true.
         */
        fun onItemEditClick(account: Account, position: Int, id: Long, adapter: AccountListAdapter) = Unit

        /**
         * Invoked when an item's counter is incremented.
         *
         * This is only invoked when [editMode] is set to false.
         */
        fun onItemHOTPCounterChange(account: Account, position: Int, id: Long, adapter: AccountListAdapter) = Unit

        /**
         * Invoked when an item is moved.
         */
        fun onItemMoved(fromPosition: Int, toPosition: Int) = Unit

        /**
         * Invoked when an item selection has changed.
         */
        fun onItemSelectionChange(account: Account, position: Int, id: Long, adapter: AccountListAdapter) = Unit
    }

    open inner class AccountViewHolder(parent: ViewGroup) : AbstractDraggableItemViewHolder(
        ViewUtil.inflate(R.layout.item_account, parent)
    ), DraggableItemViewHolder {
        private val dragState = DraggableItemState()
        val binding = ItemAccountBinding.bind(itemView)
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

    inner class TimerAccountViewHolder(parent: ViewGroup) : AccountViewHolder(parent) {
        var totpCounter: TotpCounter? = null
            private set

        fun setCounter(account: Account) {
            if (totpCounter?.timeStep != account.period) {
                totpCounter = TotpCounter(account.period)
            }
        }
    }
}