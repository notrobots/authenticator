package dev.notrobots.authenticator.models

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.text.InputFilter
import android.view.View
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.TAG_NAME_MAX_LENGTH
import dev.notrobots.authenticator.databinding.DialogAddTagBinding
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.dialogs.DialogBuilder
import dev.notrobots.authenticator.extensions.setMaxLength
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private typealias DialogOnClickListener = (dialog: DialogInterface) -> Unit

fun AddOrEditTagDialog(
    context: Context,
    coroutineScope: CoroutineScope,
    tagDao: TagDao,
    tag: Tag? = null,
    show: Boolean = true,
    onChange: () -> Unit = {}
): Dialog {
    val dialogBinding = bindView<DialogAddTagBinding>(context)

    dialogBinding.nameLayout.setMaxLength(TAG_NAME_MAX_LENGTH)
    dialogBinding.nameLayout.setErrorWhen(R.string.error_empty_field) { s -> s.isBlank() }

    tag?.let {
        dialogBinding.name.setText(it.name)
    }

    return DialogBuilder(context)
        .setTitle(
            if (tag == null) {
                R.string.label_add_tag
            } else {
                R.string.label_edit_tag
            }
        )
        .setView(dialogBinding.root)
        .setPositiveButton(R.string.label_ok) { dialog, _ ->
            val name = (dialogBinding.name.text ?: "").toString().trim()

            if (!dialogBinding.nameLayout.hasErrors) {
                coroutineScope.launch {
                    if (tag != null) {
                        if (tag.name == name) {
                            dialog.dismiss()
                        } else if (tagDao.exists(name)) {
                            dialogBinding.nameLayout.setError(R.string.error_tag_already_exists)
                        } else {
                            tag.name = name
                            tagDao.update(tag)
                            onChange()
                            dialog.dismiss()
                        }
                    } else {
                        if (tagDao.exists(name)) {
                            dialogBinding.nameLayout.setError(R.string.error_tag_already_exists)
                        } else {
                            val tag = Tag(name)

                            tagDao.insert(tag)
                            onChange()
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
        .setNegativeButton(R.string.label_cancel, null)
        .create()
        .apply {
            if (show) {
                show()
            }
        }
}

fun BaseDialog(
    context: Context,
    title: Any,
    message: Any? = null,
    positiveButton: Any? = null,
    positiveButtonCallback: DialogOnClickListener? = null,
    negativeButton: Any? = null,
    negativeButtonCallback: DialogOnClickListener? = null,
    neutralButton: Any? = null,
    neutralButtonCallback: DialogOnClickListener? = null,
    show: Boolean = true
): Dialog {
    return DialogBuilder(context)
        .setTitle(context.resolveString(title))
        .setMessage(context.resolveString(message))
        .setPositiveButton(
            context.resolveString(positiveButton),
            if (positiveButtonCallback != null) {
                { d, _ -> positiveButtonCallback(d) }
            } else {
                null
            }
        )
        .setNegativeButton(
            context.resolveString(negativeButton),
            if (negativeButtonCallback != null) {
                { d, _ -> negativeButtonCallback(d) }
            } else {
                null
            }
        )
        .setNeutralButton(
            context.resolveString(neutralButton),
            if (neutralButtonCallback != null) {
                { d, _ -> neutralButtonCallback(d) }
            } else {
                null
            }
        )
        .create()
        .apply {
            if (show) {
                show()
            }
        }
}

fun BaseCustomDialog(
    context: Context,
    title: Any,
    view: View,
    positiveButton: Any? = null,
    positiveButtonCallback: DialogOnClickListener? = null,
    negativeButton: Any? = null,
    negativeButtonCallback: DialogOnClickListener? = null,
    neutralButton: Any? = null,
    neutralButtonCallback: DialogOnClickListener? = null,
    show: Boolean = true
): Dialog {
    return BaseDialog(
        context,
        title,
        null,
        positiveButton,
        positiveButtonCallback,
        negativeButton,
        negativeButtonCallback,
        neutralButton,
        neutralButtonCallback,
    ).apply {
        setContentView(view)

        if (show) {
            show()
        }
    }
}