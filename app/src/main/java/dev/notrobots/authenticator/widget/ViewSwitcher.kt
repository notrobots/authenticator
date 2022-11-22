package dev.notrobots.authenticator.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.core.view.children
import androidx.core.view.isGone
import androidx.core.view.isVisible
import dev.notrobots.androidstuff.extensions.hide
import dev.notrobots.authenticator.R

class ViewSwitcher(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : FrameLayout(context, attrs, defStyleAttr) {
    private val views = mutableListOf<View>()

    /**
     * The id of the current visible view or -1 if no view is vibile.
     */
    var visibleViewId: Int = -1
        private set

    /**
     * The current visible view or null if no view is visible.
     */
    var visibleView: View? = null
        private set

    init {
        with(context.obtainStyledAttributes(attrs, R.styleable.ViewSwitcher, defStyleAttr, 0)) {
            if (hasValue(R.styleable.ViewSwitcher_visibleView)) {
                visibleViewId = getResourceId(R.styleable.ViewSwitcher_visibleView, 0)
            }

            recycle()
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        super.addView(child, index, params)
        views.add(child)
    }

    override fun removeView(view: View?) {
        super.removeView(view)
        views.remove(view)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        if (childCount > 0) {
            children.forEach { it.isGone = true }

            if (visibleViewId != -1) {
                showView(visibleViewId)
            } else {
                //TODO: Let devs choose if they want to show the first view by default or not
                showViewAt(0)
            }
        }
    }

    /**
     * Returns the view with the given [id] or throws an Exception if there is no view with that id.
     */
    fun getView(@IdRes id: Int): View {
        return views.find { it.id == id } ?: throw Exception("Cannot find view with id: $id")
    }

    /**
     * Returns the view at the given [position] or throws an Exception if there is no view at that position.
     */
    fun getViewAt(position: Int): View {
        return views.getOrNull(position) ?: throw Exception("Cannot find view at position: $position")
    }

    //TODO: setShowMode(Single, Multiple)

    /**
     * Shows the view with the given [id].
     */
    fun showView(@IdRes id: Int) {
        showView(getView(id))
    }

    /**
     * Shows the view at the given [position].
     */
    fun showViewAt(position: Int) {
        showView(getViewAt(position))
    }

    /**
     * Hides the current visible view.
     */
    fun hideView() {
        visibleView?.isGone = true
        visibleViewId = -1
        visibleView = null
    }

    private fun showView(view: View) {
        hideView()
        visibleView = view
        visibleViewId = view.id
        visibleView!!.isVisible = true
    }
}