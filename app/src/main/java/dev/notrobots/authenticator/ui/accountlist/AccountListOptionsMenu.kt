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
import dev.notrobots.authenticator.models.TotpIndicatorType
import dev.notrobots.authenticator.widget.GridMenu
import dev.notrobots.authenticator.widget.GridMenuItem

class AccountListOptionsMenu(
    context: Context,
    _sortMode: SortMode,
    _showIcons: Boolean,
    _showPins: Boolean,
    _totpIndicatorType: TotpIndicatorType
) : PopupWindow() {
    private val layoutInflater = LayoutInflater.from(context)
    private val binding = ViewAccountListOptionsBinding.inflate(layoutInflater)
    private val contentView = binding.root
    private var listener: Listener? = null
    var totpIndicatorType: TotpIndicatorType = _totpIndicatorType
        private set
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
            SORT_NAME_ICON,
            SortModeChangeListener(
                SORT_NAME_ASC_ICON,
                SORT_NAME_DESC_ICON,
                SORT_NAME_ICON,
                SortMode.NameAscending,
                SortMode.NameDescending,
                sortMode
            )
        )
        binding.sortOptions.addItem(
            "Label",
            SORT_LABEL_ICON,
            SortModeChangeListener(
                SORT_LABEL_ASC_ICON,
                SORT_LABEL_DESC_ICON,
                SORT_LABEL_ICON,
                SortMode.LabelAscending,
                SortMode.LabelDescending,
                sortMode
            )
        )
        binding.sortOptions.addItem(
            "Issuer",
            SORT_ISSUER_ICON,
            SortModeChangeListener(
                SORT_ISSUER_ASC_ICON,
                SORT_ISSUER_DESC_ICON,
                SORT_ISSUER_ICON,
                SortMode.IssuerAscending,
                SortMode.IssuerDescending,
                sortMode
            )
        )
        binding.sortOptions.addItem(   //FIXME: All items in the grid should have the same size
            "Custom",
            SORT_CUSTOM_ICON,
            SortModeChangeListener(
                SORT_CUSTOM_ICON,
                SORT_CUSTOM_ICON,
                SORT_CUSTOM_ICON,
                SortMode.Custom,
                SortMode.Custom,
                sortMode
            )
        )

        when (sortMode) {
            SortMode.Custom -> {
                binding.sortOptions.setCheckedAt(3)
                binding.sortOptions.getItemAt(3).setIconResource(SORT_CUSTOM_ICON)
            }
            SortMode.NameAscending -> {
                binding.sortOptions.setCheckedAt(0)
                binding.sortOptions.getItemAt(0).setIconResource(SORT_NAME_ASC_ICON)
            }
            SortMode.NameDescending -> {
                binding.sortOptions.setCheckedAt(0)
                binding.sortOptions.getItemAt(0).setIconResource(SORT_NAME_DESC_ICON)
            }
            SortMode.LabelAscending -> {
                binding.sortOptions.setCheckedAt(1)
                binding.sortOptions.getItemAt(1).setIconResource(SORT_LABEL_ASC_ICON)
            }
            SortMode.LabelDescending -> {
                binding.sortOptions.setCheckedAt(1)
                binding.sortOptions.getItemAt(1).setIconResource(SORT_LABEL_DESC_ICON)
            }
            SortMode.IssuerAscending -> {
                binding.sortOptions.setCheckedAt(2)
                binding.sortOptions.getItemAt(2).setIconResource(SORT_ISSUER_ASC_ICON)
            }
            SortMode.IssuerDescending -> {
                binding.sortOptions.setCheckedAt(2)
                binding.sortOptions.getItemAt(2).setIconResource(SORT_ISSUER_DESC_ICON)
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
            SHOW_ICONS_ICON,
            VisibilityToggleChangeListener(
                SHOW_ICONS_ICON,
                HIDE_ICONS_ICON,
                showIcons
            ) {
                showIcons = it
            }
        )
        binding.appearanceOptions.addItem(
            "Pins",
            SHOW_PINS_ICON,
            VisibilityToggleChangeListener(
                SHOW_PINS_ICON,
                HIDE_PINS_ICON,
                showPins
            ) {
                showPins = it
            }
        )
        binding.appearanceOptions.addItem(
            "TOTP",
            TOTP_INDICATOR_ICON,
            TotpIndicatorChangeListener()
        )
        binding.appearanceOptions.getItemAt(0).setIconResource(if (showIcons) SHOW_ICONS_ICON else HIDE_ICONS_ICON)
        binding.appearanceOptions.getItemAt(1).setIconResource(if (showPins) SHOW_PINS_ICON else HIDE_PINS_ICON)
        binding.appearanceOptions.getItemAt(2).setIconResource(TOTP_INDICATOR_ICONS[totpIndicatorType.ordinal])
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
        private const val SHOW_ICONS_ICON = R.drawable.ic_image
        private const val HIDE_ICONS_ICON = R.drawable.ic_hide_image
        private const val SHOW_PINS_ICON = R.drawable.ic_image
        private const val HIDE_PINS_ICON = R.drawable.ic_hide_image
        private const val SORT_NAME_ICON = R.drawable.ic_sort_az
        private const val SORT_NAME_ASC_ICON = R.drawable.ic_sort_az_asc
        private const val SORT_NAME_DESC_ICON = R.drawable.ic_sort_az_desc
        private const val SORT_LABEL_ICON = R.drawable.ic_sort_az
        private const val SORT_LABEL_ASC_ICON = R.drawable.ic_sort_az_asc
        private const val SORT_LABEL_DESC_ICON = R.drawable.ic_sort_az_desc
        private const val SORT_ISSUER_ICON = R.drawable.ic_sort_az
        private const val SORT_ISSUER_ASC_ICON = R.drawable.ic_sort_az_asc
        private const val SORT_ISSUER_DESC_ICON = R.drawable.ic_sort_az_desc
        private const val SORT_CUSTOM_ICON = R.drawable.ic_account
        private const val TOTP_INDICATOR_ICON = R.drawable.ic_account
        private val TOTP_INDICATOR_ICONS = arrayOf(
            R.drawable.ic_account,
            R.drawable.ic_account,
            R.drawable.ic_account
        )
    }

    interface Listener {
        fun onExport()
        fun onImport()
    }

    private inner class VisibilityToggleChangeListener(
        @DrawableRes private val visibleIcon: Int,
        @DrawableRes private val hiddenIcon: Int,
        initialState: Boolean,
        private val onUpdate: (value: Boolean) -> Unit  //TODO Make this abstract and simply extend it when you call addItem, so you don't need the onUpdate() callback
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
        @DrawableRes private val ascendingIcon: Int,
        @DrawableRes private val descendingIcon: Int,
        @DrawableRes private val uncheckedIcon: Int,
        private val ascendingSortMode: SortMode,
        private val descendingSortMode: SortMode,
        initialSortMode: SortMode
    ) : GridMenuItem.Listener {
        private var sortDirection: Int
        private var checkedState = false

        init {
            sortDirection = if (initialSortMode == ascendingSortMode) {
                SORT_ASCENDING
            } else if (initialSortMode == descendingSortMode) {
                SORT_DESCENDING
            } else {
                SORT_ASCENDING
            }
        }

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

    private inner class TotpIndicatorChangeListener : GridMenuItem.Listener {
        override fun onItemChecked(item: GridMenuItem, checkedState: Boolean) {
            val next = (totpIndicatorType.ordinal + 1) % TotpIndicatorType.values().size
            val icon = TOTP_INDICATOR_ICONS[next]

            totpIndicatorType = TotpIndicatorType.values()[next]
            item.setIconResource(icon)
        }
    }
}