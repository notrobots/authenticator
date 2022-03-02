package dev.notrobots.authenticator.ui.accountlist

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.annotation.DrawableRes
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ViewAccountListOptionsBinding
import dev.notrobots.authenticator.extensions.toPx
import dev.notrobots.authenticator.models.SortMode
import dev.notrobots.authenticator.widget.GridMenu
import dev.notrobots.authenticator.widget.GridMenuItem

class AccountListOptionsMenu(
    context: Context,
    _sortMode: SortMode,
    _showIcons: Boolean,
    _showPins: Boolean
) : PopupWindow() {
    private val layoutInflater = LayoutInflater.from(context)
    private val binding = ViewAccountListOptionsBinding.inflate(layoutInflater)
    private val contentView = binding.root
    private var listener: Listener? = null
    var sortMode: SortMode = _sortMode
        private set
    var showIcons: Boolean = _showIcons
        private set
    var showPins: Boolean = _showPins
        private set

    init {
        val backgroundColor = context.resolveColorAttribute(R.attr.colorSurface)

        setContentView(contentView)
        setBackgroundDrawable(ColorDrawable(backgroundColor))
        width = LinearLayout.LayoutParams.WRAP_CONTENT
        height = LinearLayout.LayoutParams.WRAP_CONTENT
        isOutsideTouchable = true
        elevation = 12.toPx()
        animationStyle = R.style.PopupWindowContextMenuAnimation  //FIXME: Copy the animation of the standard options menu

        binding.sortOptions.selectionMode = GridMenu.SelectionMode.Single
        binding.sortOptions.addItem(
            "Name",
            R.drawable.ic_sort_az,
            SortModeChangeListener(
                R.drawable.ic_sort_az_asc,
                R.drawable.ic_sort_az_desc,
                R.drawable.ic_sort_az,
                SortMode.NameAscending,
                SortMode.NameDescending
            )
        )
        binding.sortOptions.addItem(
            "Label",
            R.drawable.ic_sort_az,
            SortModeChangeListener(
                R.drawable.ic_sort_az_asc,
                R.drawable.ic_sort_az_desc,
                R.drawable.ic_sort_az,
                SortMode.LabelAscending,
                SortMode.LabelDescending
            )
        )
        binding.sortOptions.addItem(
            "Issuer",
            R.drawable.ic_sort_az,
            SortModeChangeListener(
                R.drawable.ic_sort_az_asc,
                R.drawable.ic_sort_az_desc,
                R.drawable.ic_sort_az,
                SortMode.IssuerAscending,
                SortMode.IssuerDescending
            )
        )
        binding.sortOptions.addItem(   //FIXME: All items in the grid should have the same size
            "Custom",
            R.drawable.ic_account,
            SortModeChangeListener(
                R.drawable.ic_account,
                R.drawable.ic_account,
                R.drawable.ic_account,
                SortMode.Custom,
                SortMode.Custom
            )
        )

        when (sortMode) {
            SortMode.Custom -> {
                binding.sortOptions.setChecked(3)
                binding.sortOptions.getItemAt(3).setIconResource(R.drawable.ic_account)
            }
            SortMode.NameAscending -> {
                binding.sortOptions.setChecked(0)
                binding.sortOptions.getItemAt(0).setIconResource(R.drawable.ic_sort_az_asc)
            }
            SortMode.NameDescending -> {
                binding.sortOptions.setChecked(0)
                binding.sortOptions.getItemAt(0).setIconResource(R.drawable.ic_sort_az_desc)
            }
            SortMode.LabelAscending -> {
                binding.sortOptions.setChecked(1)
                binding.sortOptions.getItemAt(1).setIconResource(R.drawable.ic_sort_az_asc)
            }
            SortMode.LabelDescending -> {
                binding.sortOptions.setChecked(1)
                binding.sortOptions.getItemAt(1).setIconResource(R.drawable.ic_sort_az_desc)
            }
            SortMode.IssuerAscending -> {
                binding.sortOptions.setChecked(2)
                binding.sortOptions.getItemAt(2).setIconResource(R.drawable.ic_sort_az_asc)
            }
            SortMode.IssuerDescending -> {
                binding.sortOptions.setChecked(2)
                binding.sortOptions.getItemAt(2).setIconResource(R.drawable.ic_sort_az_desc)
            }
            SortMode.TagAscending -> TODO()
            SortMode.TagDescending -> TODO()
        }

        binding.backupOptions.addItem("Export", R.drawable.ic_database_export) { _, _ ->
            listener?.onExport()
            dismiss()
        }
        binding.backupOptions.addItem("Import", R.drawable.ic_database_import) { _, _ ->
            listener?.onImport()
            dismiss()
        }

        binding.appearanceOptions.addItem(
            "Icons",
            R.drawable.ic_image,
            VisibilityToggleChangeListener(
                R.drawable.ic_image,
                R.drawable.ic_hide_image,
                showIcons
            ) {
                showIcons = it
            }
        )
        binding.appearanceOptions.addItem(
            "Pins",
            R.drawable.ic_image,
            VisibilityToggleChangeListener(
                R.drawable.ic_image,
                R.drawable.ic_hide_image,
                showPins
            ) {
                showPins = it
            }
        )
        binding.appearanceOptions.getItemAt(0).setIconResource(if (showIcons) R.drawable.ic_image else R.drawable.ic_hide_image)
        binding.appearanceOptions.getItemAt(1).setIconResource(if (showPins) R.drawable.ic_image else R.drawable.ic_hide_image)
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun show(view: View) {
        showAtLocation(
            view,
            Gravity.TOP or Gravity.END,
            8.toPx().toInt(),
            48.toPx().toInt()
        )
    }

    companion object {
        private const val SORT_NONE = 0
        private const val SORT_ASCENDING = 1
        private const val SORT_DESCENDING = -1
    }

    interface Listener {
        fun onExport()
        fun onImport()
    }

    private inner class VisibilityToggleChangeListener(
        private @DrawableRes val visibleIcon: Int,
        private @DrawableRes val hiddenIcon: Int,
        initialState: Boolean,
        private val onUpdate: (value: Boolean) -> Unit
    ) : GridMenuItem.Listener {
        private var state: Boolean = initialState

        override fun onItemChecked(item: GridMenuItem, checkedState: Boolean) {
            state = !state
            updateItemView(item)
        }

        private fun updateItemView(item: GridMenuItem) {
            item.setIconResource(if (state) visibleIcon else hiddenIcon)
            onUpdate(state)
        }
    }

    private inner class SortModeChangeListener(
        @DrawableRes val ascendingIcon: Int,
        @DrawableRes val descendingIcon: Int,
        @DrawableRes val uncheckedIcon: Int,
        val ascendingSortMode: SortMode,
        val descendingSortMode: SortMode
    ) : GridMenuItem.Listener {
        private var sortDirection = SORT_ASCENDING
        private var checkedState = false

        override fun onItemChecked(item: GridMenuItem, checkedState: Boolean) {
            if (checkedState) {
                if (this.checkedState) {
                    toggleSortDirection()
                    updateItemView(item)
                } else {
                    if (sortDirection == SORT_NONE) {
                        sortDirection = SORT_ASCENDING
                    }

                    updateItemView(item)
                }
            } else {
                item.setIconResource(uncheckedIcon)
            }

            this.checkedState = checkedState
        }

        private fun updateItemView(item: GridMenuItem) {
            val icon = when (sortDirection) {
                SORT_ASCENDING -> {
                    sortMode = ascendingSortMode
                    ascendingIcon
                }
                SORT_DESCENDING -> {
                    sortMode = descendingSortMode
                    descendingIcon
                }
                SORT_NONE -> uncheckedIcon

                else -> throw Exception("Unknown sort direction")
            }

            item.setIconResource(icon)
        }

        private fun toggleSortDirection() {
            sortDirection *= -1
        }
    }
}