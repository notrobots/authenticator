package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.extensions.getQuantityText
import dev.notrobots.authenticator.extensions.setFragmentResult
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.MaterialDialogBuilder
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeleteAccountDialog(
    private var accounts: List<Account> = emptyList()
) : DialogFragment() {
    @Inject
    protected lateinit var accountDao: AccountDao

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putSerializable(SAVED_STATE_ACCOUNT_COUNT, ArrayList(accounts))
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        savedInstanceState?.let {
            accounts = savedInstanceState.getSerializable(SAVED_STATE_ACCOUNT_COUNT) as ArrayList<Account>
        }

        val accountCount = accounts.size
        val title = resources.getQuantityText(
            R.plurals.label_delete_account_title,
            accountCount,
            accountCount
        )
        val message = resources.getQuantityText(
            R.plurals.label_delete_account_description,
            accountCount,
            accountCount
        )

        return MaterialDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton(R.string.label_cancel, null)
            .setPositiveButton(title) { d, i ->
                requireActivity().lifecycleScope.launch {
                    accountDao.delete(accounts)
                }
                setFragmentResult<DeleteAccountDialog>()
                d.dismiss()
            }
            .create()
    }

    companion object {
        private const val SAVED_STATE_ACCOUNT_COUNT = "DeleteAccountDialog.accountCount"
    }
}