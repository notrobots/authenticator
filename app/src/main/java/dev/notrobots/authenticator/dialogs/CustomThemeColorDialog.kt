package dev.notrobots.authenticator.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.util.bindView
import dev.notrobots.androidstuff.widget.BindableViewHolder
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.DialogCustomThemeColorBinding
import dev.notrobots.authenticator.databinding.ItemCustomThemeColorBinding
import dev.notrobots.authenticator.extensions.first
import dev.notrobots.authenticator.extensions.setFragmentResult
import dev.notrobots.authenticator.models.CustomAppTheme
import dev.notrobots.authenticator.models.MaterialDialogBuilder
import dev.notrobots.authenticator.util.viewModel
import dev.notrobots.preferences2.util.parseEnum

class CustomThemeColorDialog(
    private var theme: CustomAppTheme = Enum.first()
) : DialogFragment() {
    private lateinit var binding: DialogCustomThemeColorBinding
    private val adapter = ColorAdapter()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(SAVED_STATE_THEME, theme.name)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        binding = bindView(context) //TODO: android-stuff: bindView should take an optional ViewGroup
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SAVED_STATE_THEME)) {
                theme = parseEnum(savedInstanceState.getString(SAVED_STATE_THEME))
            }
        }

        adapter.setOnItemClickListener(object : ColorAdapter.OnItemClickListener {
            override fun onClick(item: CustomAppTheme) {
                theme = item
                adapter.setChecked(theme)
                requireContext().makeToast(theme)
            }
        })
        adapter.setChecked(theme)
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
        binding.recyclerView.adapter = adapter

        return MaterialDialogBuilder(requireContext())
            .setTitle(R.string.label_theme_color)
            .setIcon(R.drawable.ic_brush)
            .setView(binding.root)
            .setPositiveButton(R.string.label_ok) { d, _ ->
                d.dismiss()
                setFragmentResult<CustomThemeColorDialog>(
                    bundleOf(
                        EXTRA_THEME to theme
                    )
                )
            }
            .setNegativeButton(R.string.label_cancel, null)
            .create()
    }

    class ColorAdapter : RecyclerView.Adapter<ColorAdapter.ViewHolder>() {
        private val items = CustomAppTheme.values()
        private var onItemClickListener: OnItemClickListener? = null
        var checkedItem: CustomAppTheme? = null
            private set

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(parent)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val binding = holder.binding
            val theme = items[position]

            binding.colorView.setBackgroundColorRes(theme.primaryColor)
            binding.colorView.setImageResource(if (checkedItem == theme) R.drawable.ic_round_check else 0)
            binding.colorView.setOnClickListener {
                val lastChecked = items.indexOf(checkedItem)

                checkedItem = theme
                binding.colorView.setImageResource(R.drawable.ic_round_check)
                onItemClickListener?.onClick(theme)
                notifyItemChanged(lastChecked)
            }
        }

        override fun getItemCount() = items.size

        fun setChecked(theme: CustomAppTheme) {
            val lastChecked = items.indexOf(checkedItem)
            val checked = items.indexOf(theme)

            checkedItem = theme
            notifyItemChanged(lastChecked)
            notifyItemChanged(checked)
        }

        fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
            this.onItemClickListener = onItemClickListener
        }

        class ViewHolder(parent: ViewGroup) : BindableViewHolder<ItemCustomThemeColorBinding>(
            ItemCustomThemeColorBinding::class,
            parent
        )

        interface OnItemClickListener {
            fun onClick(item: CustomAppTheme)
        }
    }

    companion object {
        private const val SAVED_STATE_THEME = "CustomThemeColorDialog.theme"
        const val EXTRA_THEME = "CustomThemeColorDialog.theme"
    }
}