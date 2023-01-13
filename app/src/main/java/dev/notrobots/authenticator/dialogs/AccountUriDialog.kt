package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.setClearErrorOnType
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.DialogAccountUriBinding
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountTagCrossRefDao
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.models.MaterialDialogBuilder
import dev.notrobots.authenticator.util.BackupManager
import dev.notrobots.authenticator.util.BackupUtil
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AccountUriDialog(
    private var title: Int = 0,
) : DialogFragment() {
    private var binding: DialogAccountUriBinding? = null
    private var error: String? = null
        set(value) {
            field = value
            binding?.layoutAccountUrl?.error = value
        }

    @Inject
    protected lateinit var accountDao: AccountDao

    @Inject
    protected lateinit var tagDao: TagDao

    @Inject
    protected lateinit var accountTagCrossRefDao: AccountTagCrossRefDao

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(SAVED_STATE_TITLE, title)
        outState.putString(SAVED_STATE_ERROR, error)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = bindView(layoutInflater)
        binding?.layoutAccountUrl?.error = error
        binding?.layoutAccountUrl?.setClearErrorOnType()

        savedInstanceState?.let {
            title = savedInstanceState.getInt(SAVED_STATE_TITLE)
            error = savedInstanceState.getString(SAVED_STATE_ERROR)
        }

        return MaterialDialogBuilder(requireContext())
            .setTitle(title)
            .setView(binding?.root)
            .setPositiveButton(R.string.label_ok) { d, _ ->
                val text = binding?.textAccountUrl?.text.toString()

                if (text.isBlank()) {
                    error = getString(R.string.error_empty_field)
                } else {
                    try {
                        val data = BackupManager.importText(text)

                        requireActivity().lifecycleScope.launch {
                            //XXX Is the accountDao still alive inside the coroutine?
                            BackupUtil.importBackupData(requireActivity(), data, accountDao)
                        }
                        d.dismiss()
                    } catch (e: Exception) {
                        error = e.message
                    }
                }
            }
            .setNeutralButton(R.string.label_cancel, null)
            .create()
    }

    companion object {
        private const val SAVED_STATE_TITLE = "AccountUriDialog.title"
        private const val SAVED_STATE_ERROR = "AccountUriDialog.error"
    }
}