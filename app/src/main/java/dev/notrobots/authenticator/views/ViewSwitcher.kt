package dev.notrobots.authenticator.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.core.view.children

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

        //TODO: Let devs choose whether to show the first view by default or not
        if (views.size > 0) {
            showViewAt(0)
        }
    }

    fun getView(@IdRes id: Int): View {
        return views.find { it.id == id } ?: throw Exception("Cannot find view with id: $id")
    }

    fun getViewAt(position: Int): View {
        return views.getOrNull(position) ?: throw Exception("Cannot find view at position: $position")
    }

    //TODO: addView, removeView, clearViews

    fun showView(@IdRes id: Int) {
        showView(getView(id))
    }

    fun showViewAt(position: Int) {
        showView(getViewAt(position))
    }

    fun hideAll() {
        for (view in views) {
            view.visibility = View.GONE
        }
    }

    private fun showView(view: View) {
        visibleView?.visibility = View.GONE
        visibleView = view
        visibleView!!.visibility = View.VISIBLE
    }
}