package dev.notrobots.authenticator.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import androidx.annotation.DrawableRes
import androidx.core.view.children

/**
 * # GridMenu
 *
 * View that displays a list of [GridMenuItem] in a grid.
 *
 * ## Selection modes
 *
 * Items in the grid are checkable and the view has 3 selection modes:
 * + [SelectionMode.None], the items act as regular buttons
 * + [SelectionMode.Single], the items act as toggles and there can only be one active at a time
 * + [SelectionMode.Multiple], the items act as toggles and multiple items can be selected at a time or none
 *
 * ## Add items
 *
 * Items can be added using [addItem] or in the xml file as children of this view
 */
class GridMenu(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : GridLayout(context, attrs, defStyleAttr), View.OnClickListener {
    /**
     * Current checked item or null if no item is selected.
     */
    var checkedItem: GridMenuItem? = null
        private set

    /**
     * Selection mode.
     *
     * One of [SelectionMode].
     */
    var selectionMode = SelectionMode.None

    init {
        columnCount = 4
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    //TODO: Let the users add the views directly on the layout
    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        require(child is GridMenuItem) {
            "Child views must be of type ${GridMenuItem::class.simpleName}"
        }

        super.addView(child, index, params)
    }

    override fun onClick(view: View) {
        setChecked(view as GridMenuItem)
    }

    fun getItemAt(position: Int): GridMenuItem {
        return children.elementAt(position) as GridMenuItem
    }

    fun addItem(
        title: String,
        @DrawableRes icon: Int,
        listener: GridMenuItem.Listener? = null
    ) {
        val child = GridMenuItem(context)

        child.title = title
        child.setIconResource(icon)
        child.listener = listener
        child.setOnClickListener(this)

        addView(child)
    }

    fun addItem(
        title: String,
        @DrawableRes icon: Int,
        listener: (item: GridMenuItem, checkedState: Boolean) -> Unit
    ) {
        addItem(title, icon, object : GridMenuItem.Listener {
            override fun onItemChecked(item: GridMenuItem, checkedState: Boolean) {
                listener(item, checkedState)
            }
        })
    }

    fun setChecked(position: Int) {
        setChecked(getItemAt(position))
    }

    private fun setChecked(view: GridMenuItem) {
        when (selectionMode) {
            SelectionMode.Single -> {
                if (checkedItem != view) {
                    checkedItem?.isChecked = false
                    checkedItem?.listener?.onItemChecked(checkedItem!!, false)
                    view.isChecked = true
                    checkedItem = view
                }
            }
            SelectionMode.Multiple -> {
                view.toggle()
            }

            else -> {}
        }

        view.listener?.onItemChecked(view, view.isChecked)
    }

    enum class SelectionMode {
        None,
        Single,
        Multiple
    }
}