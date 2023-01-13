package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.models.MaterialDialogBuilder
import dev.notrobots.authenticator.models.Tag
import dev.notrobots.preferences2.getTagIdFilter
import dev.notrobots.preferences2.putTagIdFilter
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DeleteTagDialog(
    private var tag: Tag? = null
) : DialogFragment() {
    @Inject
    protected lateinit var tagDao: TagDao

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

        return MaterialDialogBuilder(requireContext())
            .setTitle("Delete tag")
            .setMessage("This will remove the tag \"${tag?.name}\".\n\nThis action cannot be undo.")
            .setPositiveButton(R.string.label_delete) { d, i ->
                requireActivity().lifecycleScope.launch {
                    tag?.let {
                        preferences.putTagIdFilter(-1)
                        tagDao.delete(it)
                    }
                }
                d.dismiss()
            }
            .setNegativeButton(R.string.label_cancel, null)
            .create()
    }
}