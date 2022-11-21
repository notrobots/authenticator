package dev.notrobots.authenticator.widget

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.google.android.material.textfield.TextInputLayout

class ImmutableInputLayout(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : TextInputLayout(context, attrs, defStyleAttr), View.OnTouchListener {
    private var clickStart = 0L
    var onClickListener: (View) -> Unit = { }

    var text: String? = ""
        set(value) {
            field = value
            editText?.setText(value)
        }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        editText?.apply {
            inputType = InputType.TYPE_NULL
            keyListener = null
            setOnTouchListener(this@ImmutableInputLayout)
        }
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                clickStart = System.currentTimeMillis()

                true
            }
            MotionEvent.ACTION_UP -> {
                val clickDuration = System.currentTimeMillis() - clickStart

                if (clickDuration < ViewConfiguration.getLongPressTimeout()) {
                    onClickListener(this)
                }

                v.performClick()
                v.requestFocus()

                true
            }

            else -> false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return onTouch(this, event)
    }
}