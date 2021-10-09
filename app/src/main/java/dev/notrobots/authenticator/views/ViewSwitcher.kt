package dev.notrobots.authenticator.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.core.view.children
import dev.notrobots.androidstuff.util.logd

class ViewSwitcher(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private val views = mutableListOf<View>()
    var visibleView: View? = null
        private set

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun onFinishInflate() {
        super.onFinishInflate()

        views.addAll(children.toMutableList())

        for (view in views) {
            view.visibility = View.INVISIBLE
        }
    }

    fun getView(@IdRes id: Int): View? {
        return views.find { it.id == id }
    }

    fun getViewAt(position: Int): View? {
        return views.getOrNull(position)
    }

    //TODO: addView, removeView, clearViews

    fun showView(@IdRes id: Int) {
        val view = getView(id)

        requireNotNull(view) { "Cannot find view with id: $id" }

        showView(view)
    }

    fun showViewAt(position: Int) {
        val view = getViewAt(position)

        requireNotNull(view) { "Cannot find view at position: $position" }

        showView(view)
    }

    fun hideAll() {
        for (view in views) {
            view.visibility = View.INVISIBLE
        }
    }

    private fun showView(view: View) {
        visibleView?.visibility = View.INVISIBLE
        visibleView = view
        visibleView!!.visibility = View.VISIBLE
    }
}