package dev.notrobots.authenticator.views

import android.content.Context
import android.util.AttributeSet
import android.widget.*
import com.google.android.material.textfield.TextInputLayout
import dev.notrobots.authenticator.R
import kotlinx.android.synthetic.main.view_betterspinner.view.*
import kotlinx.android.synthetic.main.view_betterspinner.view.*

class BetterSpinner(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : LinearLayout(context, attrs, defStyleAttr) {
    private var adapter: ArrayAdapter<String>
    private var textView: AutoCompleteTextView? = null
    private var layout: TextInputLayout? = null

    /**
     * Values of this spinner, they will be used as entries if there's no entries specified
     *
     * The current value can be retrieved using [selectedValue]
     */
    var values: List<Any?> = emptyList()
        set(value) {
            field = value

            if (entries.isEmpty()) {
                entries = value.map { it.toString() }
            }
        }

    /**
     * Entries of this spinner, they're the visible items on the spinner, these will also be used as
     * values if no values are provided
     *
     * The current entry can be retrieved using [selectedEntry]
     */
    var entries: List<String> = emptyList()
        set(value) {
            field = value

            if (values.isEmpty()) {
                values = value.toList()
            }

            resetAdapter()
        }

    /**
     * Current selected value on the spinner
     */
    val selectedValue: Any?
        get() = values.getOrNull(selectedPosition)

    /**
     * Current selected visible value on the spinner
     */
    val selectedEntry: String?
        get() = entries.getOrNull(selectedPosition)

    /**
     * Current selected item index, -1 if there's no entries
     */
    var selectedPosition: Int = -1
        private set

    /**
     * Spinner's label
     */
    var label: String = ""
        set(value) {
            field = value

            layout?.hint = label
        }

    /**
     * Current selected item
     */
    val selectedItem: Item
        get() = Item(selectedEntry!!, selectedValue)
    var onItemClickListener: (value: Any?) -> Unit = {}

    init {
        inflate(context, R.layout.view_betterspinner, this)

        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, mutableListOf())
        layout = rootView as? TextInputLayout
        textView = rootView.text1
        textView!!.setAdapter(adapter)
        textView!!.setOnItemClickListener { _, _, position, _ ->
            setSelection(position)
            onItemClickListener(values[position])
        }

        with(context.obtainStyledAttributes(attrs, R.styleable.BetterSpinner, defStyleAttr, 0)) {
            getTextArray(R.styleable.BetterSpinner_spinnerValues)?.let {
                values = it.map { it.toString() }
            }
            getTextArray(R.styleable.BetterSpinner_spinnerEntries)?.let {
                entries = it.map { it.toString() }
            }
            getText(R.styleable.BetterSpinner_spinnerLabel)?.let {
                label = it.toString()
            }
        }

        setSelection(if (selectedPosition != -1) selectedPosition else 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
    }

    fun setItems(vararg entries: Pair<String, Any?>) {
        setItems(
            entries.map { it.first },
            entries.map { it.second }
        )
    }

    fun setItems(entries: Iterable<String>, values: Iterable<Any?>) {
        this.values = values.toList()
        this.entries = entries.toList()
    }

    fun setSelection(value: Any?) {
        setSelection(values.indexOf(value))
    }

    fun setSelection(index: Int) {
        selectedPosition = if (index >= 0) index else 0
        textView?.setText(selectedEntry, false)
    }

    private fun resetAdapter() {
        adapter.clear()
        adapter.addAll(entries.toMutableList())
        setSelection(0)
    }

    data class Item(
        val entry: String,
        val value: Any?
    )
}