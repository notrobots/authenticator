package dev.notrobots.authenticator.views

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.constraintlayout.widget.ConstraintLayout

class CheckableConstraintLayout(
    context: Context,
    attrs: AttributeSet?
) : ConstraintLayout(context, attrs), Checkable {
    private var checked: Boolean = false

    override fun setChecked(checked: Boolean) {
        this.checked = checked
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun toggle() {
        this.checked = !this.checked
    }
}