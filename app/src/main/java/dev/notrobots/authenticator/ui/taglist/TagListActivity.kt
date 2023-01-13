package dev.notrobots.authenticator.ui.taglist

import android.os.Bundle
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityTagListBinding
import dev.notrobots.authenticator.dialogs.AddOrEditTagDialog
import dev.notrobots.authenticator.dialogs.DeleteTagDialog
import dev.notrobots.authenticator.models.Tag

@AndroidEntryPoint
class TagListActivity : AuthenticatorActivity() {
    private val viewModel by viewModels<TagListViewModel>()
    private val binding by viewBindings<ActivityTagListBinding>()
    private val toolbar by lazy {
        binding.toolbarLayout.toolbar
    }
    private lateinit var adapter: TagListAdapter
    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        finishOnBackPressEnabled = true

        setupListAdapter()

        binding.addTag.setOnClickListener {
            AddOrEditTagDialog()
                .show(supportFragmentManager, null)
        }

        viewModel.tags.observe(this) {
            adapter.setItems(it)
        }

        supportFragmentManager.setFragmentResultListener(AddOrEditTagDialog.REQUEST_EDIT_TAG, this) { _, _ ->
            //TODO: This way of setting a listener to a DialogFragment is bad since passing data can be hard

            adapter.notifyDataSetChanged()  //FIXME: Only notify the changed item
        }
    }

    override fun onResume() {
        super.onResume()

        binding.tagList.adapter = adapter
    }

    override fun onPause() {
        super.onPause()

        binding.tagList.adapter = null
    }

    private fun setupListAdapter() {
        adapter = TagListAdapter()
        adapter.setListener(object : TagListAdapter.Listener {
            override fun onDelete(tag: Tag, id: Long, position: Int) {
                DeleteTagDialog(tag)
                    .show(supportFragmentManager, null)
            }

            override fun onEdit(tag: Tag, id: Long, position: Int) {
                AddOrEditTagDialog(tag)
                    .show(supportFragmentManager, null)
            }
        })

        binding.tagList.layoutManager = LinearLayoutManager(this)
        binding.tagList.setEmptyView(binding.emptyView)
    }
}