package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DialogBuilder : MaterialAlertDialogBuilder {
    private var positiveButtonClickListener: DialogInterface.OnClickListener? = null
    private var negativeButtonClickListener: DialogInterface.OnClickListener? = null
    private var neutralButtonClickListener: DialogInterface.OnClickListener? = null

    constructor(context: Context, overrideThemeResId: Int) : super(context, overrideThemeResId)
    constructor(context: Context) : super(context)

    override fun setPositiveButton(text: CharSequence?, listener: DialogInterface.OnClickListener?): MaterialAlertDialogBuilder {
        positiveButtonClickListener = listener
        return super.setPositiveButton(text, null)
    }

    override fun setPositiveButton(textId: Int, listener: DialogInterface.OnClickListener?): MaterialAlertDialogBuilder {
        positiveButtonClickListener = listener
        return super.setPositiveButton(textId, null)
    }

    override fun setNegativeButton(text: CharSequence?, listener: DialogInterface.OnClickListener?): MaterialAlertDialogBuilder {
        negativeButtonClickListener = listener
        return super.setNegativeButton(text, null)
    }

    override fun setNegativeButton(textId: Int, listener: DialogInterface.OnClickListener?): MaterialAlertDialogBuilder {
        negativeButtonClickListener = listener
        return super.setNegativeButton(textId, null)
    }

    override fun setNeutralButton(text: CharSequence?, listener: DialogInterface.OnClickListener?): MaterialAlertDialogBuilder {
        neutralButtonClickListener = listener
        return super.setNeutralButton(text, null)
    }

    override fun setNeutralButton(textId: Int, listener: DialogInterface.OnClickListener?): MaterialAlertDialogBuilder {
        neutralButtonClickListener = listener
        return super.setNeutralButton(textId, null)
    }

    override fun create(): AlertDialog {
        val dialog = super.create()

        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener{
                if (positiveButtonClickListener != null) {
                    positiveButtonClickListener!!.onClick(dialog, Dialog.BUTTON_POSITIVE)
                } else {
                    dialog.dismiss()
                }
            }
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setOnClickListener{
                if (negativeButtonClickListener != null) {
                    negativeButtonClickListener!!.onClick(dialog, Dialog.BUTTON_NEGATIVE)
                } else {
                    dialog.dismiss()
                }
            }
            dialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener{
                if (neutralButtonClickListener != null) {
                    neutralButtonClickListener!!.onClick(dialog, Dialog.BUTTON_NEUTRAL)
                } else {
                    dialog.dismiss()
                }
            }
        }

        return dialog
    }

//    fun setDismissOnPositiveButtonClick() {
//        positiveButtonClickListener = DialogInterface.OnClickListener { dialog, _ ->
//            dialog.dismiss()
//        }
//    }
//
//    fun setDismissOnNegativeButtonClick() {
//        negativeButtonClickListener = DialogInterface.OnClickListener { dialog, _ ->
//            dialog.dismiss()
//        }
//    }
//
//    fun setDismissOnNeutralButtonClick() {
//        neutralButtonClickListener = DialogInterface.OnClickListener { dialog, _ ->
//            dialog.dismiss()
//        }
//    }
}