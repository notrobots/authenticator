package dev.notrobots.authenticator.widget

import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import dev.notrobots.androidstuff.widget.BaseViewHolder
import dev.notrobots.authenticator.util.bindView
import kotlin.reflect.KClass

open class BindableViewHolder<T : ViewBinding> : BaseViewHolder {
    val binding: T

    constructor(itemView: View, type: KClass<T>) : super(itemView) {
        this.binding = bindView(type, itemView)
    }

    constructor(parent: ViewGroup, type: KClass<T>) : this(bindView(type, parent))

    constructor(binding: T) : super(binding.root) {
        this.binding = binding
    }
}