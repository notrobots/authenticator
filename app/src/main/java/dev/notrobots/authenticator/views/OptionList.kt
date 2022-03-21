package dev.notrobots.authenticator.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.androidstuff.extensions.resolveDrawable
import dev.notrobots.androidstuff.extensions.resolveString
import dev.notrobots.androidstuff.extensions.setTint
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ItemListOptionBinding
import dev.notrobots.authenticator.databinding.ViewOptionlistBinding
import dev.notrobots.authenticator.extensions.getResourceIdOrNull

class OptionList(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private val options = mutableListOf<Option>()
    private val adapter = OptionAdapter(context)
    private val binding: ViewOptionlistBinding

    /**
     * Text appearance of the option's title
     */
    var titleTextAppearance: Int = 0
        set(value) {
            adapter.notifyDataSetChanged()
            field = value
        }

    /**
     * Text appearance of the option's description
     */
    var descriptionTextAppearance: Int = 0
        set(value) {
            adapter.notifyDataSetChanged()
            field = value
        }

    /**
     * Tint of the option's icon
     */
    var iconTint: Int = 0
        set(value) {
            adapter.notifyDataSetChanged()
            field = value
        }

    init {
        binding = ViewOptionlistBinding.inflate(LayoutInflater.from(context), this, true)

        titleTextAppearance = R.style.OptionList_Title
        descriptionTextAppearance = R.style.OptionList_Description
        iconTint = context.resolveColorAttribute(R.attr.colorPrimary)

        with(context.obtainStyledAttributes(attrs, R.styleable.OptionList, defStyleAttr, 0)) {
            val tint = getColor(R.styleable.OptionList_option_iconTint, 0)

            if (tint != 0) {
                iconTint = tint
            }

            getResourceIdOrNull(R.styleable.OptionList_option_titleTextAppearance)?.let {
                titleTextAppearance = it
            }
            getResourceIdOrNull(R.styleable.OptionList_option_descriptionTextAppearance)?.let {
                descriptionTextAppearance = it
            }
        }

        binding.optionList.adapter = adapter
        binding.optionList.setOnItemClickListener { _, _, position, _ ->
            options[position].listener()
        }

        if (isInEditMode) {
            val description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed iaculis laoreet scelerisque. Quisque sed lorem ornare, sagittis erat et, finibus nisl. Cras ac finibus dolor. "

            addOption("Option 1", description, R.drawable.ic_account)
            addOption("Option 2", description, R.drawable.ic_file)
            addOption("Option 3", description, R.drawable.ic_info)
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    fun addOption(option: Option) {
        options.add(option)
        adapter.notifyDataSetChanged()
    }

    fun addOption(
        title: Any?,
        description: Any? = null,
        icon: Any? = null,
        listener: () -> Unit = {}
    ) {
        options.add(Option(title, description, icon, listener))
        adapter.notifyDataSetChanged()
    }

    fun removeOption(option: Option) {
        options.remove(option)
        adapter.notifyDataSetChanged()
    }

    fun removeOptionAt(position: Int) {
        options.removeAt(position)
        adapter.notifyDataSetChanged()
    }

    data class Option(
        val title: Any?,
        val description: Any? = null,
        val icon: Any? = null,
        val listener: () -> Unit = {}
    )

    private inner class OptionAdapter(context: Context) : ArrayAdapter<Option>(context, 0) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(
                R.layout.item_list_option,
                parent,
                false
            )
            val option = options[position]
            val binding = ItemListOptionBinding.bind(view)

            binding.title.text = context.resolveString(option.title)
            binding.title.setTextAppearance(titleTextAppearance)
            binding.description.text = context.resolveString(option.description)
            binding.description.setTextAppearance(descriptionTextAppearance)
            binding.icon.setTint(iconTint)

            if (option.icon != null) {
                val icon = context.resolveDrawable(option.icon)

                binding.icon.setImageDrawable(icon)
                binding.icon.visibility = View.VISIBLE
            } else {
                binding.icon.visibility = View.INVISIBLE
            }

            return binding.root
        }

        override fun getCount(): Int {
            return options.size
        }
    }
}