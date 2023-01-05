package dev.notrobots.authenticator.dialogs.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.RadioButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatCheckedTextView
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.extensions.first
import dev.notrobots.authenticator.models.TotpIndicatorType
import dev.notrobots.authenticator.widget.preference.CountdownIndicatorPreference
import dev.notrobots.preferences2.dialogs.MaterialPreferenceDialog
import dev.notrobots.preferences2.util.parseEnum
import java.util.concurrent.TimeUnit

/**
 * Material PreferenceDialog class that displays the values of the [TotpIndicatorType] enum.
 *
 * This class is adapted from the base [androidx.preference.ListPreferenceDialogFragmentCompat].
 */
//TODO: preferences2: A preference class that can show enum values
class CountdownIndicatorPreferenceDialog : MaterialPreferenceDialog() {
    private var clickedDialogEntryIndex = 0
    private val items = TotpIndicatorType.values().toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val preference = preference as CountdownIndicatorPreference

            clickedDialogEntryIndex = preference.findIndexOfValue(preference.value)
        } else {
            clickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_INDEX, clickedDialogEntryIndex)
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)

        val adapter = CountdownIndicatorAdapter(requireContext(), items, clickedDialogEntryIndex)

        builder.setAdapter(adapter) { d, which ->
            clickedDialogEntryIndex = which
            onClick(d, DialogInterface.BUTTON_POSITIVE)
            dialog!!.dismiss()
        }
//        builder.setSingleChoiceItems(adapter, clickedDialogEntryIndex) { d, which ->
//            clickedDialogEntryIndex = which
//            onClick(d, DialogInterface.BUTTON_POSITIVE)
//            dialog!!.dismiss()
//        }
        builder.setPositiveButton(null, null)
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val preference = preference as CountdownIndicatorPreference
            val value = preference.entryValues[clickedDialogEntryIndex].toString()

            if (preference.callChangeListener(value)) {
                preference.value = value
            }
        }
    }

    private class CountdownIndicatorAdapter(
        context: Context,
        items: List<TotpIndicatorType>,
        private val checkedItem: Int
    ) : ArrayAdapter<TotpIndicatorType>(context, 0, items) {
        private val layoutInflater by lazy {
            LayoutInflater.from(context)
        }

        override fun hasStableIds(): Boolean {
            return true
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        @SuppressLint("ViewHolder")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val layoutRes = when (getItem(position)!!) {
                TotpIndicatorType.Circular -> R.layout.item_countdown_indicator_preference_circular
                TotpIndicatorType.CircularSolid -> R.layout.item_countdown_indicator_preference_circular_solid
                TotpIndicatorType.CircularText -> R.layout.item_countdown_indicator_preference_circular_text
                TotpIndicatorType.Text -> R.layout.item_countdown_indicator_preference_text
                TotpIndicatorType.Row -> R.layout.item_countdown_indicator_preference_row
                TotpIndicatorType.Background -> R.layout.item_countdown_indicator_preference_background
            }
            // It seems like using the [convertView] breaks the selected view and it's displayed with the
            // default (first) layout, ignoring the convertView fixes it.
            // This list is short and won't be expanded further into development so it's safe to ignore.
            val view = layoutInflater.inflate(layoutRes, parent, false)
            val titleView = view.findViewById<View>(R.id.title) as? AppCompatCheckedTextView

            view.findViewById<View>(R.id.totp_background_indicator)?.let {
                it.background.level = 40 * 10000 / 60   //40s out of 60
            }

            titleView?.let {
                it.isChecked = checkedItem == position
            }

            return view
        }
    }

    companion object {
        private const val SAVE_STATE_INDEX = "CountdownIndicatorPreferenceDialog.index"
    }
}