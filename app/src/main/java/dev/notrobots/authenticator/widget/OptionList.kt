package dev.notrobots.authenticator.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import dev.notrobots.androidstuff.extensions.resolveDrawable
import dev.notrobots.androidstuff.extensions.resolveString
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ViewOptionlistBinding

class OptionList(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private val options = mutableListOf<OptionListItem>()
    private val binding = bindView<ViewOptionlistBinding>(this, true)

    init {
        if (isInEditMode) {
            val description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed iaculis laoreet scelerisque. Quisque sed lorem ornare, sagittis erat et, finibus nisl. Cras ac finibus dolor. "

            addOption("Option 1", description, R.drawable.ic_account)
            addOption("Option 2", description, R.drawable.ic_file)
            addOption("Option 3", description, R.drawable.ic_info)
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (binding == null) {
            super.addView(child, index, params)
        } else {
            if (child !is OptionListItem) {
                throw Exception("This view only accepts child views of type ${OptionListItem::class}")
            }

            binding.content.addView(child, index, params)
            options.add(child)
        }
    }

    override fun removeView(view: View?) {
        super.removeView(view)

        options.remove(view)
    }

    fun addOption(
        title: Any?,
        description: Any? = null,
        icon: Any? = null,
        clickListener: OnClickListener? = null
    ): OptionListItem {
        val optionListItem = OptionListItem(context)

        optionListItem.title = context.resolveString(title)
        optionListItem.description = context.resolveString(description)
        optionListItem.setOnClickListener(clickListener)

        context.resolveDrawable(icon)?.let {
            optionListItem.setIconDrawable(it)
        }

        addView(optionListItem)

        return optionListItem
    }

//    data class Option(
//        val title: Any?,
//        val description: Any? = null,
//        val icon: Any? = null,
//        val listener: () -> Unit = {}
//    )
}