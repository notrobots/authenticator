package dev.notrobots.authenticator.models

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.AdapterView
import android.widget.ListAdapter
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialDialogBuilder : MaterialAlertDialogBuilder {
    private var positiveButtonClickListener: DialogInterface.OnClickListener? = null
    private var negativeButtonClickListener: DialogInterface.OnClickListener? = null
    private var neutralButtonClickListener: DialogInterface.OnClickListener? = null

    constructor(context: Context, overrideThemeResId: Int) : super(context, overrideThemeResId)
    constructor(context: Context) : super(context)

    override fun setTitle(titleId: Int): MaterialDialogBuilder {
        return super.setTitle(titleId) as MaterialDialogBuilder
    }

    override fun setTitle(title: CharSequence?): MaterialDialogBuilder {
        return super.setTitle(title) as MaterialDialogBuilder
    }

    override fun setCustomTitle(customTitleView: View?): MaterialDialogBuilder {
        return super.setCustomTitle(customTitleView) as MaterialDialogBuilder
    }

    override fun setMessage(messageId: Int): MaterialDialogBuilder {
        return super.setMessage(messageId) as MaterialDialogBuilder
    }

    override fun setMessage(message: CharSequence?): MaterialDialogBuilder {
        return super.setMessage(message) as MaterialDialogBuilder
    }

    override fun setIcon(iconId: Int): MaterialDialogBuilder {
        return super.setIcon(iconId) as MaterialDialogBuilder
    }

    override fun setIcon(icon: Drawable?): MaterialDialogBuilder {
        return super.setIcon(icon) as MaterialDialogBuilder
    }

    override fun setIconAttribute(attrId: Int): MaterialDialogBuilder {
        return super.setIconAttribute(attrId) as MaterialDialogBuilder
    }

    override fun setPositiveButtonIcon(icon: Drawable?): MaterialDialogBuilder {
        return super.setPositiveButtonIcon(icon) as MaterialDialogBuilder
    }

    override fun setNegativeButtonIcon(icon: Drawable?): MaterialDialogBuilder {
        return super.setNegativeButtonIcon(icon) as MaterialDialogBuilder
    }

    override fun setNeutralButtonIcon(icon: Drawable?): MaterialDialogBuilder {
        return super.setNeutralButtonIcon(icon) as MaterialDialogBuilder
    }

    override fun setCancelable(cancelable: Boolean): MaterialDialogBuilder {
        return super.setCancelable(cancelable) as MaterialDialogBuilder
    }

    override fun setOnCancelListener(onCancelListener: DialogInterface.OnCancelListener?): MaterialDialogBuilder {
        return super.setOnCancelListener(onCancelListener) as MaterialDialogBuilder
    }

    override fun setOnDismissListener(onDismissListener: DialogInterface.OnDismissListener?): MaterialDialogBuilder {
        return super.setOnDismissListener(onDismissListener) as MaterialDialogBuilder
    }

    override fun setOnKeyListener(onKeyListener: DialogInterface.OnKeyListener?): MaterialDialogBuilder {
        return super.setOnKeyListener(onKeyListener) as MaterialDialogBuilder
    }

    override fun setItems(itemsId: Int, listener: DialogInterface.OnClickListener?): MaterialDialogBuilder {
        return super.setItems(itemsId, listener) as MaterialDialogBuilder
    }

    override fun setItems(items: Array<out CharSequence>?, listener: DialogInterface.OnClickListener?): MaterialDialogBuilder {
        return super.setItems(items, listener) as MaterialDialogBuilder
    }

    override fun setAdapter(adapter: ListAdapter?, listener: DialogInterface.OnClickListener?): MaterialDialogBuilder {
        return super.setAdapter(adapter, listener) as MaterialDialogBuilder
    }

    override fun setCursor(cursor: Cursor?, listener: DialogInterface.OnClickListener?, labelColumn: String): MaterialDialogBuilder {
        return super.setCursor(cursor, listener, labelColumn) as MaterialDialogBuilder
    }

    override fun setMultiChoiceItems(itemsId: Int, checkedItems: BooleanArray?, listener: DialogInterface.OnMultiChoiceClickListener?): MaterialDialogBuilder {
        return super.setMultiChoiceItems(itemsId, checkedItems, listener) as MaterialDialogBuilder
    }

    override fun setMultiChoiceItems(items: Array<out CharSequence>?, checkedItems: BooleanArray?, listener: DialogInterface.OnMultiChoiceClickListener?): MaterialDialogBuilder {
        return super.setMultiChoiceItems(items, checkedItems, listener) as MaterialDialogBuilder
    }

    override fun setMultiChoiceItems(cursor: Cursor?, isCheckedColumn: String, labelColumn: String, listener: DialogInterface.OnMultiChoiceClickListener?): MaterialDialogBuilder {
        return super.setMultiChoiceItems(cursor, isCheckedColumn, labelColumn, listener) as MaterialDialogBuilder
    }

    override fun setSingleChoiceItems(itemsId: Int, checkedItem: Int, listener: DialogInterface.OnClickListener?): MaterialDialogBuilder {
        return super.setSingleChoiceItems(itemsId, checkedItem, listener) as MaterialDialogBuilder
    }

    override fun setSingleChoiceItems(cursor: Cursor?, checkedItem: Int, labelColumn: String, listener: DialogInterface.OnClickListener?): MaterialDialogBuilder {
        return super.setSingleChoiceItems(cursor, checkedItem, labelColumn, listener) as MaterialDialogBuilder
    }

    override fun setSingleChoiceItems(items: Array<out CharSequence>?, checkedItem: Int, listener: DialogInterface.OnClickListener?): MaterialDialogBuilder {
        return super.setSingleChoiceItems(items, checkedItem, listener) as MaterialDialogBuilder
    }

    override fun setSingleChoiceItems(adapter: ListAdapter?, checkedItem: Int, listener: DialogInterface.OnClickListener?): MaterialDialogBuilder {
        return super.setSingleChoiceItems(adapter, checkedItem, listener) as MaterialDialogBuilder
    }

    override fun setOnItemSelectedListener(listener: AdapterView.OnItemSelectedListener?): MaterialDialogBuilder {
        return super.setOnItemSelectedListener(listener) as MaterialDialogBuilder
    }

    override fun setView(layoutResId: Int): MaterialDialogBuilder {
        return super.setView(layoutResId) as MaterialDialogBuilder
    }

    override fun setView(view: View?): MaterialDialogBuilder {
        return super.setView(view) as MaterialDialogBuilder
    }

    override fun getBackground(): Drawable? {
        return super.getBackground()
    }

    override fun setBackground(background: Drawable?): MaterialDialogBuilder {
        return super.setBackground(background) as MaterialDialogBuilder
    }

    override fun setBackgroundInsetStart(backgroundInsetStart: Int): MaterialDialogBuilder {
        return super.setBackgroundInsetStart(backgroundInsetStart) as MaterialDialogBuilder
    }

    override fun setBackgroundInsetTop(backgroundInsetTop: Int): MaterialDialogBuilder {
        return super.setBackgroundInsetTop(backgroundInsetTop) as MaterialDialogBuilder
    }

    override fun setBackgroundInsetEnd(backgroundInsetEnd: Int): MaterialDialogBuilder {
        return super.setBackgroundInsetEnd(backgroundInsetEnd) as MaterialDialogBuilder
    }

    override fun setBackgroundInsetBottom(backgroundInsetBottom: Int): MaterialDialogBuilder {
        return super.setBackgroundInsetBottom(backgroundInsetBottom) as MaterialDialogBuilder
    }

    override fun setPositiveButton(text: CharSequence?, listener: DialogInterface.OnClickListener?) = apply {
        super.setPositiveButton(text, null)
        positiveButtonClickListener = listener ?: DialogInterface.OnClickListener { d, _ ->
            d.dismiss()
        }
    }

    override fun setPositiveButton(textId: Int, listener: DialogInterface.OnClickListener?) = apply {
        super.setPositiveButton(textId, null)
        positiveButtonClickListener = listener ?: DialogInterface.OnClickListener { d, _ ->
            d.dismiss()
        }
    }

    override fun setNegativeButton(text: CharSequence?, listener: DialogInterface.OnClickListener?) = apply {
        super.setNegativeButton(text, null)
        negativeButtonClickListener = listener ?: DialogInterface.OnClickListener { d, _ ->
            d.dismiss()
        }
    }

    override fun setNegativeButton(textId: Int, listener: DialogInterface.OnClickListener?) = apply {
        super.setNegativeButton(textId, null)
        negativeButtonClickListener = listener ?: DialogInterface.OnClickListener { d, _ ->
            d.dismiss()
        }
    }

    override fun setNeutralButton(text: CharSequence?, listener: DialogInterface.OnClickListener?) = apply {
        super.setNeutralButton(text, null)
        neutralButtonClickListener = listener ?: DialogInterface.OnClickListener { d, _ ->
            d.dismiss()
        }
    }

    override fun setNeutralButton(textId: Int, listener: DialogInterface.OnClickListener?) = apply {
        super.setNeutralButton(textId, null)
        neutralButtonClickListener = listener ?: DialogInterface.OnClickListener { d, _ ->
            d.dismiss()
        }
    }

    override fun create(): AlertDialog {
        val dialog = super.create()

        dialog.setOnShowListener {
            dialog.getButton(Dialog.BUTTON_POSITIVE).setOnClickListener {
                positiveButtonClickListener?.onClick(dialog, Dialog.BUTTON_POSITIVE)
            }
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setOnClickListener {
                negativeButtonClickListener?.onClick(dialog, Dialog.BUTTON_NEGATIVE)
            }
            dialog.getButton(Dialog.BUTTON_NEUTRAL).setOnClickListener {
                neutralButtonClickListener?.onClick(dialog, Dialog.BUTTON_NEUTRAL)
            }
        }

        return dialog
    }
}