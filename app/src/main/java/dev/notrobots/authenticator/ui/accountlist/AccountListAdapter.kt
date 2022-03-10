package dev.notrobots.authenticator.ui.accountlist

import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
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
import dev.notrobots.authenticator.ui.accountlist.AccountListAdapter.AccountViewHolder
import dev.notrobots.authenticator.util.OTPGenerator
import dev.notrobots.authenticator.util.ViewUtil
import java.util.*
import java.util.concurrent.TimeUnit

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
    var showIcons: Boolean = true
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    val selectedItems
        get() = items.filter { it.isSelected }
    val selectedItemCount
        get() = items.count { it.isSelected }

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun getItemId(position: Int): Long {
        return items[position].id
    }

    //region Rendering

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        return AccountViewHolder(parent)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val view = holder.itemView
        val account = items[position]
        val binding = holder.binding
        val id = account.id
        val icon = getIssuerIcon(account.issuer)
        val generatePin = {
            formatPin(OTPGenerator.generate(account))
        }

        binding.icon.visibility = if (showIcons && !editMode) View.VISIBLE else View.GONE
        binding.icon.setImageResource(icon)
        binding.pin.visibility = if (showPins && !editMode) View.VISIBLE else View.GONE

        updateViewMarginsAndConstraints(binding.icon)
        updateViewMarginsAndConstraints(binding.indicator)
        updateViewMarginsAndConstraints(binding.nextPin)
        updateViewMarginsAndConstraints(binding.dragHandle)
        updateViewMarginsAndConstraints(binding.edit)

        if (account.label.isEmpty()) {
            binding.label.text = account.name
            binding.name.disable()
        } else {
            binding.name.text = account.name
            binding.label.text = account.label
            binding.name.show()
        }

        view.isSelected = account.isSelected
        view.setOnClickListener {
            if (editMode) {
                account.toggleSelected()
                view.isSelected = account.isSelected
            }

            listener.onItemClick(account, id, this)
        }
        view.setOnLongClickListener {
            if (!editMode) {
                account.toggleSelected()
                view.isSelected = account.isSelected
            }

            listener.onItemLongClick(account, id, this)
        }

        if (editMode) {
            binding.edit.setOnClickListener {
                listener.onItemEditClick(account, id, this)
            }
            binding.groupEdit.visibility = View.VISIBLE
            binding.groupTotp.visibility = View.GONE
            binding.groupHotp.visibility = View.GONE
        } else {
            binding.groupEdit.visibility = View.GONE

            when (account.type) {
                OTPType.TOTP -> {
                    val timer = TotpTimer(account.period)

                    timer.setListener(object : TotpTimer.Listener {
                        override fun onTick(timeLeft: Long) {
                            binding.indicator.progress = timeLeft.toInt()
                        }

                        override fun onValueChanged() {
                            binding.pin.text = generatePin()
                        }
                    })
                    timer.start()

                    binding.indicator.max = TimeUnit.SECONDS.toMillis(account.period).toInt()
                    binding.groupTotp.visibility = View.VISIBLE
                    binding.groupHotp.visibility = View.GONE
                }
                OTPType.HOTP -> {
//                    binding.pin.text = "- ".repeat(account.digits)
                    binding.pin.text = generatePin()
                    binding.nextPin.setOnClickListener {
                        it as ImageView

                        account.counter++
                        binding.pin.text = generatePin()
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

    //region Drag&Drop

    override fun onCheckCanStartDrag(holder: AccountViewHolder, position: Int, x: Int, y: Int): Boolean {
        // Item can only be dragged if the sort mode is set to Custom
        if (sortMode != SortMode.Custom) {
            holder.itemView.context.makeToast("Change sort mode to Custom to reorder the items")
            return false
        }

        // Item can only be dragged if the adapter is in Item edit mode
        if (!editMode) {
            return false
        }

        // Item can only be dragged by its drag handle
        if (!ViewUtil.hitTest(holder.dragHandle, x, y)) {
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
                    return oldList[oldItemPosition].name == items[newItemPosition].name &&
                            oldList[oldItemPosition].label == items[newItemPosition].label &&
                            oldList[oldItemPosition].issuer == items[newItemPosition].issuer
                }
            })

            this.items.clear()
            this.items.addAll(items)
            result.dispatchUpdatesTo(this)
        }
    }

    fun selectAll() {
        for (item in items) {
            item.isSelected = true
        }

        notifyDataSetChanged()
    }

    fun clearSelected() {
        for (item in items) {
            item.isSelected = false
        }

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
        fun onItemClick(account: Account, id: Long, adapter: AccountListAdapter) = Unit
        fun onItemLongClick(account: Account, id: Long, adapter: AccountListAdapter): Boolean = false
        fun onItemEditClick(account: Account, id: Long, adapter: AccountListAdapter) = Unit
        fun onItemCounterIncrement(account: Account, id: Long, adapter: AccountListAdapter) = Unit
        fun onItemMoved(fromPosition: Int, toPosition: Int) = Unit
    }

    class AccountViewHolder(parent: ViewGroup) : AbstractDraggableItemViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
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
}