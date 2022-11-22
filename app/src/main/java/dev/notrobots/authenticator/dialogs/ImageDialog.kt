package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import androidx.core.view.marginTop
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.authenticator.databinding.DialogImageBinding

class ImageDialog(
    val bitmap: Bitmap,
    val contentDescription: String? = null  //TODO: Should be required
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = bindView<DialogImageBinding>(requireContext())

        binding.image.setImageBitmap(bitmap)
        binding.image.contentDescription = contentDescription

        return MaterialAlertDialogBuilder(requireContext())
//            .setTitle("\t")
            .setView(binding.root)
            .setPositiveButton("Close", null)
            .create()
            .apply {
                show()
            }
    }
}