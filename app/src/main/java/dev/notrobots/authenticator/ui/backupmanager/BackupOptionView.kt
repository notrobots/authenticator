package dev.notrobots.authenticator.ui.backupmanager

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.LinearLayout
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.children
import dev.notrobots.androidstuff.extensions.setDisabled
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ViewBackupOptionBinding
import dev.notrobots.authenticator.util.bindView

typealias OnCheckedChangeListener = (view: BackupOptionView, isChecked: Boolean) -> Unit

class BackupOptionView(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int
) : LinearLayoutCompat(context, attrs, defStyleAttr), Checkable {
    //FIXME: This is the way of initializing a custom view, fix this everywhere and in the android-stuff library
    private val binding = bindView<ViewBackupOptionBinding>(this, true)
    private var onCheckedChangeListener: OnCheckedChangeListener? = null
    private var checkedState = false
    private var broadcasting = false
    var title: String = ""
        set(value) {
            binding.title.text = value
            field = value
        }

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context) : this(context, null, 0)

    init {
        binding.header.isClickable = true
        binding.header.setOnClickListener {
            if (isEnabled) {
                toggle()
            }
        }
        binding.toggle.isClickable = false

        with(context.obtainStyledAttributes(attrs, R.styleable.BackupOptionView, defStyleAttr, 0)){
            //XXX: Does this line invoke the listener?
            isChecked = getBoolean(R.styleable.BackupOptionView_checked, false)
            title = getString(R.styleable.BackupOptionView_title) ?: ""
            recycle()
        }

        if (isInEditMode) {
            isChecked = true
            title = "Backup option"
            inflate(context, R.layout.view_backup_option, this)
        }
    }

    //TODO: Implement this
//    override fun onSaveInstanceState(): Parcelable? {
//        return super.onSaveInstanceState()
//    }

    //TODO: Implement this
//    override fun onRestoreInstanceState(state: Parcelable?) {
//        super.onRestoreInstanceState(state)
//    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (binding == null) {
            super.addView(child, index, params);
        } else {
            binding.content.addView(child, index, params)
        }
    }

    override fun setChecked(checked: Boolean) {
        checkedState = checked
        binding.toggle.isChecked = checked
        binding.content.setDisabled(!checked)

        if (broadcasting) {
            return
        }

        broadcasting = true
        onCheckedChangeListener?.invoke(this, checked)
        broadcasting = false
    }

    override fun isChecked(): Boolean {
        return checkedState
    }

    override fun toggle() {
        isChecked = !checkedState
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        binding.toggle.isEnabled = enabled
        binding.header.isEnabled = enabled
        binding.title.isEnabled = enabled   //FIXME: TextView color should change when disabled

        for (child in binding.content.children) {
            child.isEnabled = enabled
        }
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        this.onCheckedChangeListener = listener
    }
}