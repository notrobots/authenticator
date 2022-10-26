package dev.notrobots.authenticator.widget.preference

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.DialogPreference
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.ui.backupmanager.BackupFrequency
import org.json.JSONObject

class BackupFrequencyPreference(
    context: Context?,
    attrs: AttributeSet?,
    defStyleAttr: Int = R.attr.preferenceStyle,
    defStyleRes: Int = defStyleAttr
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {
    var backupFrequency: BackupFrequency? = null
        set(value) {
            field = value

            if (value != null) {
                persistString(value.toJson().toString(0))
            } else {
                persistString(DEFAULT_VALUE)
            }
        }

    init {
//        isPersistent = false
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int,) : this(context, attrs, defStyleAttr, 0)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0, 0)
    constructor(context: Context) : this(context, null, 0, 0)

//    override fun getDialogLayoutResource(): Int {
//        return R.layout.preference_dialog_backup_frequency
//    }

//    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
//        return a?.getString(index) ?: DEFAULT_VALUE
//    }

    override fun onSetInitialValue(defaultValue: Any?) {
        super.onSetInitialValue(defaultValue)

        val value = getPersistedString((defaultValue as? String) ?: DEFAULT_VALUE)

        backupFrequency = if (value.isBlank()) {
            BackupFrequency()
        } else {
            BackupFrequency.fromJson(value)
        }
    }
    companion object {
        const val DEFAULT_VALUE = ""
    }
}