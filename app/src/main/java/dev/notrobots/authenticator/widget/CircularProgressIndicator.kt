package dev.notrobots.authenticator.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import dev.notrobots.androidstuff.extensions.resolveColorAttribute
import dev.notrobots.authenticator.R

class CircularProgressIndicator(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : View(context, attrs, defStyleAttr) {
    private val paint: Paint
    private var rect: RectF? = null
    private val currentAngle
        get() = progress * 360F / max
    var max: Int = 100
    var progress: Int = 75
    var progressColor: Int = 0
        set(value) {
            field = value
            paint.color = value
            invalidate()
        }

    init {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        progressColor = context.resolveColorAttribute(R.attr.colorPrimary)

        if (isInEditMode) {
            progress = 25
            max = 100
        }

        with(context.obtainStyledAttributes(attrs, R.styleable.CircularProgressIndicator, defStyleAttr, 0)) {
            if (hasValue(R.styleable.CircularProgressIndicator_cpi_progress)) {
                progress = getInteger(R.styleable.CircularProgressIndicator_cpi_progress, 75)
            }

            if (hasValue(R.styleable.CircularProgressIndicator_cpi_max)) {
                max = getInteger(R.styleable.CircularProgressIndicator_cpi_max, 100)
            }

            recycle()
        }
    }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun onDraw(canvas: Canvas) {
        val angle = currentAngle

        if (rect == null) {
            rect = RectF(
                1F,
                1F,
                (width - 1).toFloat(),
                (height - 1).toFloat()
            )
        }

        if (angle < 360) {
            canvas.drawArc(
                rect!!,
                -90F,
                angle,
                true,
                paint
            )
        } else {
            canvas.drawOval(rect!!, paint)
        }
    }
}