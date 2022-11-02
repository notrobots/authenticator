package dev.notrobots.authenticator.ui.taglist

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.showChoice
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityTagListBinding
import dev.notrobots.authenticator.models.AddOrEditTagDialog
import dev.notrobots.authenticator.models.BaseDialog
import dev.notrobots.authenticator.models.Tag
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TagListActivity : AuthenticatorActivity() {
    private val viewModel by viewModels<TagListViewModel>()
    private val binding by viewBindings<ActivityTagListBinding>()
    private val toolbar by lazy {
        binding.toolbarLayout.toolbar
    }
    private lateinit var adapter: TagListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        finishOnBackPressEnabled = true

        setupListAdapter()

        binding.addTag.setOnClickListener {
            AddOrEditTagDialog(this, lifecycleScope, viewModel.tagDao)
        }

        viewModel.tags.observe(this) {
            adapter.setItems(it)
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
                BaseDialog(
                    this@TagListActivity,
                    "Delete tag",
                    "This will remove the tag \"${tag.name}\".\n\nThis action cannot be undo.",
                    "Delete",
                    {
                        lifecycleScope.launch {
                            viewModel.tagDao.delete(tag)
                            it.dismiss()
                            adapter.notifyItemChanged(position)
                        }
                    },
                    "Cancel",
                    {
                        it.dismiss()
                    }
                )
            }

            override fun onEdit(tag: Tag, id: Long, position: Int) {
                AddOrEditTagDialog(this@TagListActivity, lifecycleScope, viewModel.tagDao, tag) {
                    adapter.notifyItemChanged(position)
                }
            }
        })

        binding.tagList.layoutManager = LinearLayoutManager(this)
        binding.tagList.setEmptyView(binding.emptyView)
    }
}