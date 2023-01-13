package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.hasErrors
import dev.notrobots.androidstuff.extensions.setError
import dev.notrobots.androidstuff.extensions.setErrorWhen
import dev.notrobots.androidstuff.extensions.setMaxLength
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.TAG_NAME_MAX_LENGTH
import dev.notrobots.authenticator.databinding.DialogAddTagBinding
import dev.notrobots.authenticator.db.TagDao
import dev.notrobots.authenticator.extensions.setFragmentResult
import dev.notrobots.authenticator.models.MaterialDialogBuilder
import dev.notrobots.authenticator.models.Tag
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddOrEditTagDialog(
    private var tag: Tag? = null
) : DialogFragment() {
    @Inject
    protected lateinit var tagDao: TagDao

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(SAVED_STATE_TAG, tag)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = bindView<DialogAddTagBinding>(requireContext())

        savedInstanceState?.let {
            tag = it.getSerializable(SAVED_STATE_TAG) as? Tag
        }

        binding.nameLayout.setMaxLength(TAG_NAME_MAX_LENGTH)
        binding.nameLayout.setErrorWhen(R.string.error_empty_field) { s -> s.isBlank() }

        tag?.let {
            binding.name.setText(it.name)
        }

        return MaterialDialogBuilder(requireContext())
            .setTitle(
                if (tag == null) {
                    R.string.label_add_tag
                } else {
                    R.string.label_edit_tag
                }
            )
            .setView(binding.root)
            .setPositiveButton(R.string.label_ok) { dialog, _ ->
                val name = (binding.name.text ?: "").toString().trim()

                if (!binding.nameLayout.hasErrors) {
                    requireActivity().lifecycleScope.launch {
                        if (tag != null) {
                            tag?.let {tag ->
                                if (tag.name == name) {
                                    dialog.dismiss()
                                } else if (tagDao.exists(name)) {
                                    binding.nameLayout.setError(R.string.error_tag_already_exists)
                                } else {
                                    tag.name = name
                                    tagDao.update(tag)
                                    dialog.dismiss()
                                    setFragmentResult(REQUEST_EDIT_TAG, bundleOf())
                                }
                            }
                        } else {
                            if (tagDao.exists(name)) {
                                binding.nameLayout.setError(R.string.error_tag_already_exists)
                            } else {
                                val tag = Tag(name)

                                tagDao.insert(tag)
                                dialog.dismiss()
                                setFragmentResult(REQUEST_ADD_TAG, bundleOf())
                            }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.label_cancel, null)
            .create()
    }

    companion object {
        private const val SAVED_STATE_TAG = "AddOrEditTagDialog.tag"
        const val REQUEST_ADD_TAG = "AddOrEditTagDialog.ADD_TAG"
        const val REQUEST_EDIT_TAG = "AddOrEditTagDialog.EDIT_TAG"
    }
}