package dev.notrobots.authenticator.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class ProgressView(
    context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
) : View(context, attrs, defStyleAttr, defStyleRes) {
    var progress: Int = 0
        set(value) {
            field = value
            invalidate()
        }
    var max: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int,) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context) : this(context, null, 0, 0)

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

//        Rect().
    }
}