package dev.notrobots.authenticator.ui.accountlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.BaseActivity
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityAccountListBinding
import dev.notrobots.authenticator.dialogs.*
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.ui.account.AccountActivity
import dev.notrobots.authenticator.ui.backupexport.ExportActivity
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import dev.notrobots.authenticator.ui.backupimportresult.ImportResultActivity
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.notrobots.authenticator.util.AccountExporter
import dev.notrobots.authenticator.util.OTPGenerator
import kotlinx.android.synthetic.main.activity_account_list.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountListActivity : BaseActivity() {
    private val viewModel by viewModels<AccountListViewModel>()
    private val binding by viewBindings<ActivityAccountListBinding>()
    private lateinit var adapter: AccountListAdapter
    private lateinit var recyclerViewDragDropManager: RecyclerViewDragDropManager
    private lateinit var adapterWrapper: RecyclerView.Adapter<*>
    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val scanBarcode = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data != null) {
                val data = it.data!!.getStringArrayListExtra(BarcodeScannerActivity.EXTRA_QR_LIST) ?: listOf<String>()

                try {
                    import(data.first())
                } catch (e: Exception) {
                    showInfo(R.string.label_error, e.message)
                }
            }
        }
    }
    private val listAdapterListener = object : AccountListAdapter.Listener {
        override fun onItemClick(account: Account, id: Long, adapter: AccountListAdapter) {
            if (adapter.editMode) {
                if (!account.isSelected && adapter.selectedItemCount == 0) {
                    actionMode?.finish()
                    return
                }
                actionMode?.title = adapter.selectedItemCount.toString()
            } else {
                copyToClipboard(OTPGenerator.generate(account)) //TODO: Keep the value cached
                makeToast("Copied!")
            }
        }

        override fun onItemLongClick(account: Account, id: Long, adapter: AccountListAdapter): Boolean {
            if (actionMode == null) {
                binding.toolbarLayout.toolbar.startActionMode(actionModeCallback)
            }

            return true
        }

        override fun onItemEditClick(account: Account, id: Long, adapter: AccountListAdapter) {
            startActivity(AccountActivity::class) {
                putExtra(AccountActivity.EXTRA_ACCOUNT, account)
            }
            actionMode?.finish()
        }

        override fun onItemMoved(fromPosition: Int, toPosition: Int) {
            lifecycleScope.launch {
                viewModel.accountDao.update(adapter.items)
            }
        }
    }
    private var actionMode: ActionMode? = null
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(R.menu.menu_account_list_edit, menu)
            actionMode = mode
            actionMode?.title = adapter.selectedItemCount.toString()
            adapter.editMode = true
            adapter.notifyDataSetChanged()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_account_remove -> {
                    if (adapter.selectedItemCount > 0) {
                        DeleteAccountDialog(adapter.selectedItemCount, supportFragmentManager) {
                            lifecycleScope.launch {
                                viewModel.accountDao.delete(adapter.selectedItems)
                            }
                            actionMode?.finish()
                        }
                    } else {
                        makeToast("No items selected")
                    }
                }
                R.id.menu_account_selectall -> {
                    if (adapter.items.isNotEmpty()) {
                        adapter.selectAll()
                        actionMode?.title = adapter.selectedItemCount.toString()
                    } else {
                        makeToast("Nothing to select")
                    }
                }
            }

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            adapter.editMode = false
            adapter.clearSelected()
        }
    }

    //region Activity lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)
        doubleBackPressToExitEnabled = true

        binding.btnAddAccountQr.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)

            scanBarcode.launch(intent)
            btn_add_account.close(true)
        }
        binding.btnAddAccountUrl.setOnClickListener {
            AccountUriDialog(supportFragmentManager, R.string.label_add_account) { data, dialog ->
                try {
                    import(data)
                    dialog.dismiss()
                } catch (e: Exception) {
                    dialog.error = e.message
                }
            }
            btn_add_account.close(true)
        }
        binding.btnAddAccountCustom.setOnClickListener {
            startActivity(AccountActivity::class)
            btn_add_account.close(true)
        }

        setupListAdapter()

        viewModel.sortMode.value = preferences.getSortMode()
        viewModel.sortMode.observe(this) {
            adapter.sortMode = it
        }
        viewModel.accounts.observe(this) {
            adapter.setItems(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        actionMode?.finish()
    }

    //endregion

    override fun onBackPressed() {
        if (actionMode != null) {
            actionMode!!.finish()
        } else {
            super.onBackPressed()
        }
    }

    //region Options menu

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_account_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_account_list_edit -> {
                binding.toolbarLayout.toolbar.startActionMode(actionModeCallback)
            }
            R.id.menu_account_list_overflow -> {
                showOptionsMenu()
            }
            R.id.menu_clear -> {
                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()
                }
            }
            R.id.menu_refresh -> {
                adapter.notifyDataSetChanged()
            }
            R.id.menu_add_test -> {
                val accounts = listOf(
                    Account("Account 1", "22334455").apply { },
                    Account("Account 2", "22334466").apply { },
                    Account("Account 3", "22332277").apply { },
                    Account("Account 4", "22334455").apply { },
                    Account("Account 5", "22444455").apply { },
                    Account("Account 6", "22774477").apply { }
                )
                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()

                    accounts.forEach { viewModel.insertAccount(it) }
                }
            }
            R.id.menu_add_test_2 -> {
                val accounts = listOf(
                    Account("Max", "22334455").apply { label = "Twitter"; },
                    Account("Max", "22334466").apply { label = "Steam"; },
                    Account("Max", "22332277").apply { label = "Amazon"; },
                    Account("Max", "22334455").apply { label = "EGS"; },
                    Account("Max", "22444455").apply { label = "Github"; },
                    Account("JohnDoe", "22774477"),
                    Account("MarioRossi", "77334455"),
                    Account("JaneDoe", "22223355")
                )

                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()

                    accounts.forEach { viewModel.insertAccount(it) }
                }
            }
            R.id.menu_add_test_3 -> {
                val accounts = listOf(
                    Account("google@gmail.com", "22334455").apply { label = "Google"; issuer = "google.com" },
                    Account("google@gmail.com", "33442255").apply { issuer = "discord"; type = OTPType.HOTP },
                    Account("google@gmail.com", "66334422").apply { label = "Github"; issuer = "github" },
                    Account("Account name", "22335544").apply { label = "Account label" }
                )

                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()

                    accounts.forEach { viewModel.insertAccount(it) }
                }
            }
        }

        return true
    }

    /**
     * Shows the custom options menu on the top right corner of the screen
     */
    private fun showOptionsMenu(): PopupWindow {
        val popup = AccountListOptionsMenu(
            this,
            viewModel.sortMode.value ?: SortMode.Custom,
            adapter.showIcons,
            adapter.showPins
        )

        popup.setListener(object : AccountListOptionsMenu.Listener {
            override fun onExport() {
                if (viewModel.accounts.value?.isNotEmpty() == true) {
                    startActivity(ExportActivity::class)
                } else {
                    makeSnackBar("Nothing to export", binding.root)
                }
            }

            override fun onImport() {
                startActivity(ImportActivity::class)
            }
        })
        popup.setOnDismissListener {
            preferences.setSortMode(popup.sortMode)
            preferences.setShowIcons(popup.showIcons)
            preferences.setShowPins(popup.showPins)

            adapter.showIcons = popup.showIcons
            adapter.showPins = popup.showPins
            viewModel.sortMode.value = popup.sortMode
        }
        popup.show(binding.toolbarLayout.toolbar)

        return popup
    }

    //endregion

    /**
     * Tries to import the given [data] and throws an Exception if there any errors.
     */
    private fun import(data: String) {
        val importedData = AccountExporter.import(data)     //FIXME: This throws an unreadable exception for the final user

        if (importedData.accounts.size > 1 || AccountExporter.isBackup(data)) {
            startActivity(ImportResultActivity::class) {
                putExtra(ImportResultActivity.EXTRA_DATA, arrayListOf(importedData))
            }
            logd("QR: Importing backup of size: ${importedData.accounts}")
        } else {
            val account = AccountExporter.parseAccount(data)

            addOrReplaceAccount(account)
            logd("QR: Importing single account")
        }
    }

    /**
     * Sets up the the [RecyclerView] that shows the accounts and its adapter
     */
    private fun setupListAdapter() {
        val animator = DraggableItemAnimator()
        val layoutManager = LinearLayoutManager(this)

        adapter = AccountListAdapter()
        adapter.setListener(listAdapterListener)
        adapter.showIcons = preferences.getShowIcons(true)
        adapter.showPins = preferences.getShowPins(true)

        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        recyclerViewDragDropManager.attachRecyclerView(list_accounts)
        recyclerViewDragDropManager.setInitiateOnTouch(true)
        recyclerViewDragDropManager.isCheckCanDropEnabled = true
//        recyclerViewDragDropManager.setDraggingItemShadowDrawable(
//            ContextCompat.getDrawable(this, R.drawable.material_shadow_z3) as NinePatchDrawable?
//        )

        adapterWrapper = recyclerViewDragDropManager.createWrappedAdapter(adapter)
        binding.listAccounts.layoutManager = layoutManager
        binding.listAccounts.adapter = adapterWrapper
        binding.listAccounts.itemAnimator = animator
        binding.listAccounts.setEmptyView(binding.emptyView)

//        if (supportsViewElevation()) {
//            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
//        } else {
//            mRecyclerView.addItemDecoration(ItemShadowDecorator((ContextCompat.getDrawable(requireContext(), R.drawable.material_shadow_z1) as NinePatchDrawable?)!!))
//        }
//        binding.listAccounts.addItemDecoration(SimpleListDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.list_divider_h), true))
    }

    /**
     * Adds or replace the given [account].
     */
    private fun addOrReplaceAccount(account: Account) {
        lifecycleScope.launch {
            if (viewModel.accountDao.exists(account)) {
                ReplaceAccountDialog(supportFragmentManager, object : ReplaceAccountDialog.Listener {
                    override fun onReplace() {
                        lifecycleScope.launch {
                            // If the given account has the default id, we need to grab the id
                            // corresponding to the given account's name, issuer and label and set it
                            // to the given account so that it can be updated
                            if (account.id == 0L) {
                                viewModel.updateAccount(account)
                            }
                        }
                        logd("Replacing existing account: $account")
                    }

                    override fun onKeepBoth() {
                        lifecycleScope.launch {
                            viewModel.insertAccountWithSameName(account)
                        }
                        logd("Keeping both accounts: $account")
                    }
                })
            } else {
                viewModel.insertAccount(account)
            }
        }
    }
}