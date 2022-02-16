package dev.notrobots.authenticator.dialogs

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

abstract class InstantDialog(
    fragmentManager: FragmentManager? = null,
    tag: String? = null
) : DialogFragment() {
    init {
        fragmentManager?.let {
            show(it, tag)
        }
    }
}