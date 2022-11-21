package dev.notrobots.authenticator.widget

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.core.text.HtmlCompat
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ViewOptionListItemBinding
import dev.notrobots.authenticator.extensions.setHTMLText

class OptionListItem(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private val binding = bindView<ViewOptionListItemBinding>(this, true)

    /**
     * Text appearance of the option's title
     */
    var titleTextAppearance: Int = 0
        set(value) {
            binding.title.setTextAppearance(titleTextAppearance)
            field = value
        }

    /**
     * Text appearance of the option's description
     */
    var descriptionTextAppearance: Int = 0
        set(value) {
            binding.description.setTextAppearance(descriptionTextAppearance)
            field = value
        }

    /**
     * Tint of the option's icon
     */
    var iconTint: Int = 0
        set(value) {
            binding.icon.setTint(value)
            field = value
        }

    var title: CharSequence = ""
        set(value) {
            binding.title.setHTMLText(value)
            field = value
        }

    var description: CharSequence = ""
        set(value) {
            binding.description.setHTMLText(value)
            field = value
        }

    init {
        with(context.obtainStyledAttributes(attrs, R.styleable.OptionListItem, defStyleAttr, 0)) {
            val primaryColor = context.resolveColorAttribute(R.attr.colorPrimary)

            iconTint = getColor(R.styleable.OptionListItem_option_iconTint, primaryColor)

            titleTextAppearance = getResourceId(R.styleable.OptionListItem_option_titleTextAppearance, R.style.OptionList_Item_Title)
            descriptionTextAppearance = getResourceId(R.styleable.OptionListItem_option_descriptionTextAppearance, R.style.OptionList_Item_Description)

            if (hasValue(R.styleable.OptionListItem_option_icon)) {
                setIconResource(getResourceId(R.styleable.OptionListItem_option_icon, 0))
            }

            if (hasValue(R.styleable.OptionListItem_option_title)) {
                title = getText(R.styleable.OptionListItem_option_title) ?: ""
            }

            if (hasValue(R.styleable.OptionListItem_option_description)) {
                description = getText(R.styleable.OptionListItem_option_description) ?: ""
            }

            recycle()
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (binding == null) {
            super.addView(child, index, params)
        }else {
            binding.content.addView(child)
        }
    }

    override fun removeView(view: View?) {
        if (binding == null) {
            super.removeView(view)
        }else {
            binding.content.removeView(view)
        }
    }

    fun setIconDrawable(drawable: Drawable) {
        binding.icon.setImageDrawable(drawable)
    }

    fun setIconResource(@DrawableRes res: Int) {
        binding.icon.setImageResource(res)
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        binding.root.setOnClickListener(listener)
    }

    override fun setOnLongClickListener(listener: OnLongClickListener?) {
        binding.root.setOnLongClickListener(listener)
    }
}