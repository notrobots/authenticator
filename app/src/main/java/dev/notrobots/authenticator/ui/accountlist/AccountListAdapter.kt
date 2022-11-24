package dev.notrobots.authenticator.ui.accountlist

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
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

class AccountListAdapter : RecyclerView.Adapter<AccountViewHolder>(), DraggableItemAdapter<AccountViewHolder>, Filterable {
    private var listener: Listener = object : Listener {}
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Items that are currently showing despite clear text being disabled.
     *
     * This is only used when [clearTextEnabled] is set to false.
     */
    private var visibleItems = mutableSetOf<Account>()
    private val pinCache = mutableMapOf<Account, String>()
    var items = mutableListOf<Account>()    //FIXME: private
        private set
    val isEmpty
        get() = items.isEmpty()
    var editMode: Boolean = false
    var collapsePins: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var sortMode: SortMode = SortMode.Custom
    var totpIndicatorType: TotpIndicatorType = TotpIndicatorType.Circular
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var collapseIcons: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var clearTextEnabled: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                visibleItems.clear()
                notifyDataSetChanged()
            }
        }
    var hidePinsOnChange: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                //TODO: Clear all timers and reset the states
                visibleItems.clear()
                notifyDataSetChanged()
            }
        }
    var pinTextSize: PinTextSize = PinTextSize.Medium
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
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
        return getItem(position).accountId
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)

        return when (item.type) {
            OTPType.TOTP -> VIEW_TYPE_TOTP
            OTPType.HOTP -> VIEW_TYPE_HOTP
        }
    }

    /**
     * Returns the item at the given [position].
     */
    fun getItem(position: Int): Account {
        return items[position]
    }

    /**
     * Returns the item at the given [position] or null if the index is out of bounds of this list.
     */
    fun getItemOrNull(position: Int): Account? {
        return items.getOrNull(position)
    }

    //region Rendering

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return when (viewType) {
            VIEW_TYPE_HOTP -> AccountViewHolder(parent)
            VIEW_TYPE_TOTP -> TimerAccountViewHolder(parent)

            else -> throw Exception("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val view = holder.itemView
        val account = items[position]
        val binding = holder.binding
        val id = account.accountId
        val icon = KnownIssuers.lookup(account.issuer)

        binding.icon.visibility = if (collapseIcons || editMode) View.GONE else View.VISIBLE
        binding.icon.setImageResource(icon)
        binding.pin.visibility = if (collapsePins || editMode) View.GONE else View.VISIBLE
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
                listener.onItemSelectionChange(account, position, account.accountId, this)
            } else {
                if (!clearTextEnabled) {
                    // If clear text is not enabled (`Preferences.HIDE_PINS` set to True)
                    // we have to white list the accounts that were forced to show.
                    // If the account is already showing we'll remove from the list.

                    if (account in visibleItems) {
                        visibleItems.remove(account)
                    } else {
                        visibleItems.add(account)
                    }

                    refreshPin(holder, account)
                }

                listener.onItemClick(account, position, id, this)
            }
        }
        view.setOnLongClickListener {
            if (!editMode) {
                selectedItems.add(account)
                view.isSelected = true
                listener.onItemSelectionChange(account, position, account.accountId, this)
            }

            listener.onItemLongClick(account, position, id, this)
        }

        if (editMode) {
            // Restore selection state or get it from the account instance
            view.isSelected = account in selectedItems

            binding.edit.setOnClickListener {
                listener.onItemEditClick(account, position, id, this)
            }
            binding.shareQr.setOnClickListener {
                listener.onShareAccount(account, position, account.accountId, this)
            }
            binding.indicators.showView(R.id.edit_container)
            binding.totpRowIndicator.hide()
            binding.totpBackgroundIndicator.hide()
        } else {
            // Clear selection, accounts can only be selected in edit mode
            view.isSelected = false
            binding.pin.setTextAppearance(pinTextSize.res)

            when (account.type) {
                OTPType.TOTP -> {
                    holder as TimerAccountViewHolder
                    holder.setCounter(account)
                    refreshPin(holder, account)
                    refreshTimer(holder, account)

                    if (totpIndicatorType != TotpIndicatorType.Row) {
                        binding.totpRowIndicator.hide()
                    }

                    if (totpIndicatorType != TotpIndicatorType.Background) {
                        binding.totpBackgroundIndicator.hide()
                    }

                    when (totpIndicatorType) {
                        TotpIndicatorType.Background -> {
                            binding.totpBackgroundIndicator.show()
                            binding.indicators.hideView()
                        }
                        TotpIndicatorType.Row -> {
                            binding.indicators.hideView()
                            binding.totpRowIndicator.max = TimeUnit.SECONDS.toMillis(account.period).toInt()
                        }
                        TotpIndicatorType.CircularText -> {
                            binding.indicators.showView(R.id.totp_circular_text_indicator)
                            binding.totpCircularTextIndicatorCircular.max = TimeUnit.SECONDS.toMillis(account.period).toInt()
                        }
                        TotpIndicatorType.CircularSolid -> {
                            binding.indicators.showView(R.id.totp_circular_indicator_solid)
                            binding.totpCircularIndicatorSolid.max = TimeUnit.SECONDS.toMillis(account.period).toInt()
                        }
                        TotpIndicatorType.Circular -> {
                            binding.indicators.showView(R.id.totp_circular_indicator)
                            binding.totpCircularIndicator.max = TimeUnit.SECONDS.toMillis(account.period).toInt()
                        }
                        TotpIndicatorType.Text -> {
                            binding.indicators.showView(R.id.totp_text_indicator)
                        }
                    }
                }
                OTPType.HOTP -> {
                    val primaryColor = view.context.resolveColorAttribute(R.attr.colorPrimary)
                    val textColorSecondary = view.context.resolveColorAttribute(android.R.attr.textColorSecondary)

                    refreshPin(holder, account)
                    binding.indicators.showView(R.id.hotp_indicator)
                    binding.totpRowIndicator.hide()
                    binding.totpBackgroundIndicator.hide()
                    binding.hotpIndicator.setOnClickListener {
                        it as ImageView

                        account.counter++   //TODO: Add a flag that disables the account generation temporary
                        refreshPin(holder, account)
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

    //region Filtering

    override fun getFilter(): Filter {
        return AccountFilter(items.toList())
    }

    inner class AccountFilter(private val source: List<Account>) : Filter() {
        //TODO: If an item is added while a filter is "active", the source will be out of sync

        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val input = constraint.toString().lowercase()
            val results = FilterResults()

            if (input.isNotBlank()) {
                results.values = source.filter {
                    it.name.lowercase().contains(input) ||
                    it.label.lowercase().contains(input) ||
                    it.issuer.lowercase().contains(input)
                }
            } else {
                results.values = source
            }

            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            setItems(results.values as List<Account>)
        }

        fun reset() {
            setItems(source)
        }
    }

    //endregion

    @SuppressLint("NotifyDataSetChanged")
    fun setItems(items: List<Account>) {
        if (this.items.isEmpty()) {
            this.items.addAll(items)
            visibleItems.clear()
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
                    return oldList[oldItemPosition].accountId == items[newItemPosition].accountId
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return oldList[oldItemPosition] == items[newItemPosition]
                }
            })

            visibleItems = visibleItems.filter {
                it in items
            }.toMutableSet()

            oldList.clear()
            oldList.addAll(items)
            result.dispatchUpdatesTo(this)
        }
        pinCache.clear()
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
            topMargin = if (!collapsePins) 8.toDp().toInt() else 0
            verticalBias = if (!collapsePins) 0F else 0.5F
        }
    }

    /**
     * Generates the current pin for the given [account]
     */
    private fun generatePin(account: Account): String {
        return OTPGenerator.generate(account)
    }

    /**
     * Formats the given pin with the given options.
     */
    private fun formatPin(
        pin: String,
        groupSize: Int = 3,
        divider: String = " ",
        clearText: Boolean = true
    ): String {
        val rgx = Regex(".{1,${groupSize}}+")

        return if (clearText) {
            pin
        } else {
            pin.replace(Regex("."), "-")
        }.replace(rgx, "$0$divider").trim()
    }

    /**
     * Refreshes the timer of the given [holder] and [account] without redrawing the whole row.
     */
    fun refreshTimer(holder: TimerAccountViewHolder, account: Account) {
        when (totpIndicatorType) {
            TotpIndicatorType.Background -> {
                holder.totpCounter?.getTimeUntilNextCounter()?.let {
                    refreshPin(holder, account)
                    val max = TimeUnit.SECONDS.toMillis(account.period).toInt()
                    val progress = it * 10000 / max

                    holder.binding.totpBackgroundIndicator.background.level = progress.toInt()
                }
            }
            TotpIndicatorType.Row -> {
                holder.totpCounter?.getTimeUntilNextCounter()?.let {
                    refreshPin(holder, account)
                    holder.binding.totpRowIndicator.progress = it.toInt()
                }
            }
            TotpIndicatorType.CircularText -> {
                holder.totpCounter?.getTimeUntilNextCounter()?.let {
                    refreshPin(holder, account)
                    holder.binding.totpCircularTextIndicatorCircular.progress = it.toInt()
                    holder.binding.totpCircularTextIndicatorText.text = ceil(it / 1000F).toInt().toString()
                }
            }
            TotpIndicatorType.CircularSolid -> {
                holder.totpCounter?.getTimeUntilNextCounter()?.let {
                    refreshPin(holder, account)
                    holder.binding.totpCircularIndicatorSolid.progress = it.toInt()
                }
            }
            TotpIndicatorType.Circular -> {
                holder.totpCounter?.getTimeUntilNextCounter()?.let {
                    refreshPin(holder, account)
                    holder.binding.totpCircularIndicator.progress = it.toInt()
                }
            }
            TotpIndicatorType.Text -> {
                holder.totpCounter?.getTimeUntilNextCounter()?.let {
                    refreshPin(holder, account)
                    holder.binding.totpTextIndicator.text = ceil(it / 1000F).toInt().toString()
                }
            }
        }
    }

    /**
     * Refreshes the pin of the given [holder] and [account] without redrawing the whole row.
     */
    fun refreshPin(holder: AccountViewHolder, account: Account) {
        val pin = generatePin(account)

        if (!pinCache.containsKey(account) || pin != pinCache[account]) {
            if (hidePinsOnChange && !clearTextEnabled && account in visibleItems) {
                visibleItems.remove(account)
            }
            pinCache[account] = pin
        }

        holder.binding.pin.text = formatPin(
            pin,
            clearText = clearTextEnabled || (account in visibleItems)
        )
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

        /**
         * Invoked when an item needs to be shared.
         */
        fun onShareAccount(account: Account, position: Int, id: Long, adapter: AccountListAdapter) = Unit
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