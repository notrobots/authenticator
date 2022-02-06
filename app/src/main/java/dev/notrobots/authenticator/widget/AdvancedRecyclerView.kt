package dev.notrobots.authenticator.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * [Custom RecyclerView that adds support for an empty view][https://stackoverflow.com/a/27801394/15872880]
 */
class AdvancedRecyclerView : RecyclerView {
    private var emptyView: View? = null
    private val observer = object : AdapterDataObserver() {
        override fun onChanged() {
            checkIfEmpty()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            checkIfEmpty()
        }
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    override fun setAdapter(adapter: Adapter<*>?) {
        val oldAdapter = getAdapter()
        oldAdapter?.unregisterAdapterDataObserver(observer)
        super.setAdapter(adapter)
        adapter?.registerAdapterDataObserver(observer)
        checkIfEmpty()
    }

    fun setEmptyView(emptyView: View?) {
        this.emptyView = emptyView
        checkIfEmpty()
    }

    private fun checkIfEmpty() {
        if (emptyView != null && adapter != null) {
            val emptyViewVisible = adapter!!.itemCount == 0

            emptyView!!.visibility = if (emptyViewVisible) VISIBLE else GONE
            visibility = if (emptyViewVisible) GONE else VISIBLE
        }
    }
}