package dev.notrobots.authenticator.widget

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.widget.*
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ViewMaterialspinnerBinding
import dev.notrobots.authenticator.util.bindCustomView

class MaterialSpinner(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : LinearLayout(context, attrs, defStyleAttr) {
    private val binding = bindCustomView<ViewMaterialspinnerBinding>(this)
    private var adapter: ArrayAdapter<String>
    private var itemSelectedListener: ItemSelectedListener? = null

    /**
     * Values of this spinner.
     *
     * The current value can be retrieved using [selectedValue]
     */
    var values: List<Any?> = emptyList()
        private set

    /**
     * Labels of this spinner, they're the visible options on the spinner.
     *
     * The current label can be retrieved using [selectedLabel]
     */
    var labels: List<String> = emptyList()
        private set

    /**
     * Current selected value on the spinner.
     */
    val selectedValue: Any?
        get() = values.getOrNull(selectedPosition)

    /**
     * Current selected visible value on the spinner.
     */
    val selectedLabel: String?
        get() = labels.getOrNull(selectedPosition)

    /**
     * Current selected item index, -1 if there's no entries.
     */
    var selectedPosition: Int = -1
        private set

    /**
     * Spinner's hint
     */
    var hint: String? = ""
        set(value) {
            field = value
            binding.layout.hint = hint
        }

    init {
        isSaveEnabled = true
        adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, mutableListOf())
        binding.text.setAdapter(adapter)
        binding.text.setOnItemClickListener { _, _, position, _ ->
            setSelection(position)
        }

        with(context.obtainStyledAttributes(attrs, R.styleable.MaterialSpinner, defStyleAttr, 0)) {
            if (hasValue(R.styleable.MaterialSpinner_spinnerValues)) {
                val values = getTextArray(R.styleable.MaterialSpinner_spinnerValues)

                setValues(values.map { it.toString() })
            }

            if (hasValue(R.styleable.MaterialSpinner_spinnerLabels)) {
                val labels = getTextArray(R.styleable.MaterialSpinner_spinnerLabels)

                setLabels(labels.map { it.toString() })
            }

            if (hasValue(R.styleable.MaterialSpinner_spinnerLabel)) {
                hint = getText(R.styleable.MaterialSpinner_spinnerLabel).toString()
            }

            recycle()   //TODO: Call all the recycle on all other views
        }

        hint = hint
        setSelection(if (selectedPosition != -1) selectedPosition else 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

//    override fun onSaveInstanceState(): Parcelable {
//        return SavedState(super.onSaveInstanceState())
//    }
//
//    override fun onRestoreInstanceState(state: Parcelable?) {
//        val savedState = state as SavedState
//
//        super.onRestoreInstanceState(savedState.superState)
//
//        setLabels(savedState.labels)
//        setValues(savedState.values)
//        selectedPosition = savedState.selectedPosition
//        setSelection(selectedPosition)
//    }

    /**
     * Sets the values on this Spinner.
     *
     * These will be used as labels if no labels are provided, the [toString] method will be used to stringify them.
     */
    fun setValues(values: Iterable<Any?>) {
        this.values = values.toList()

        if (labels.isEmpty()) {
            labels = values.map { it.toString() }
        }
    }

    /**
     * Sets the labels on this Spinner.
     */
    fun setLabels(labels: Iterable<String>) {
        this.labels = labels.toList()
        resetAdapter()
    }

    /**
     * Sets both the labels and values on this Spinner.
     */
    fun setItems(labels: Iterable<String>, values: Iterable<Any?>) {
        this.values = values.toList()
        this.labels = labels.toList()
        setSelection(0)
    }

    /**
     * Sets the values on this Spinner based on the specified enum type.
     */
    inline fun <reified E : Enum<E>> setValues() {
        setValues(E::class.java.enumConstants.toList())
        setSelection(0)
    }

    /**
     * Sets the labels on this Spinner based on the specified enum type.
     */
    inline fun <reified E : Enum<E>> setLabels() {
        setLabels(E::class.java.enumConstants.map { it.name })
        setSelection(0)
    }

    /**
     * Sets both the labels and values on this Spinner based on the specified enum type.
     */
    inline fun <reified E : Enum<E>> setItems() {
        setValues<E>()
        setLabels<E>()
    }

    fun setOnItemSelectedListener(listener: ItemSelectedListener?) {
        this.itemSelectedListener = listener
    }

    /**
     * Selects the given [value].
     */
    fun setSelection(value: Any?) {
        val index = values.indexOf(value)

        if (index > -1) {
            setSelection(index)
        } else {
            throw Exception("Item $value was not found inside this ${MaterialSpinner::class.simpleName}")
        }
    }

    /**
     * Selects the item at the given [index].
     */
    fun setSelection(index: Int) {
        selectedPosition = if (index >= 0) index else 0
        binding.text.setText(selectedLabel, false)
        itemSelectedListener?.onItemSelected(selectedLabel, selectedValue, this)
    }

    private fun resetAdapter() {
        adapter.clear()
        adapter.addAll(labels.toMutableList())
        setSelection(0)
    }

    fun interface ItemSelectedListener {
        fun onItemSelected(label: String?, value: Any?, spinner: MaterialSpinner)
    }

//    private class SavedState : BaseSavedState {
//        var values: List<Any?> = emptyList()
//        var labels: List<String> = emptyList()
//        var selectedPosition: Int = -1
//
//        constructor(superState: Parcelable?) : super(superState)
//
//        constructor(parcel: Parcel) : super(parcel) {
//            parcel.readList(values, values::class.java.classLoader)
//            parcel.readList(labels, labels::class.java.classLoader)
//            selectedPosition = parcel.readInt()
//        }
//
//        override fun writeToParcel(out: Parcel, flags: Int) {
//            super.writeToParcel(out, flags)
//
//            out.writeList(values)
//            out.writeList(labels)
//            out.writeInt(selectedPosition)
//        }
//
//        companion object {
//            @JvmField
//            val CREATOR = object : Parcelable.Creator<SavedState?> {
//                override fun createFromParcel(`in`: Parcel): SavedState? {
//                    return SavedState(`in`)
//                }
//
//                override fun newArray(size: Int): Array<SavedState?> {
//                    return arrayOfNulls<SavedState>(size)
//                }
//            }
//        }
//    }
}