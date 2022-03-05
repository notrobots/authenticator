package dev.notrobots.authenticator.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Checkable
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ViewGridMenuItemBinding

class GridMenuItem(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr), Checkable {
    private val binding: ViewGridMenuItemBinding
    private var isChecked = false

    /**
     * Title of this menu item.
     */
    var title: String
        get() = binding.title.text.toString()
        set(value) {
            binding.title.text = value
        }

    /**
     * Icon of this menu item.
     */
    var icon: Drawable?
        get() = binding.icon.drawable
        set(value) {
            binding.icon.setImageDrawable(value)
        }

    /**
     * This view's main listener.
     */
    var listener: Listener? = null

    init {
        binding = ViewGridMenuItemBinding.inflate(LayoutInflater.from(context), this, true)
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun setChecked(checkedState: Boolean) {
        isChecked = checkedState

        val color = if (isChecked) {
            context.resolveColorAttribute(R.attr.colorPrimary)
        } else {
            context.resolveColorAttribute(android.R.attr.textColorSecondary)
        }

        binding.icon.setTint(color)
    }

    override fun isChecked(): Boolean {
        return isChecked
    }

    override fun toggle() {
        setChecked(!isChecked)
    }

    fun setIconResource(@DrawableRes res: Int) {
        binding.icon.setImageResource(res)
    }

    interface Listener {
        fun onItemChecked(item: GridMenuItem, checkedState: Boolean)
    }
}