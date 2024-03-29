package dev.notrobots.authenticator.ui.accountlist

import android.app.Activity
import android.app.SearchManager
import android.app.job.JobScheduler
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.h6ah4i.android.widget.advrecyclerview.animator.DraggableItemAnimator
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.*
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.data.TOTP_INDICATOR_UPDATE_DELAY
import dev.notrobots.authenticator.databinding.ActivityAccountListBinding
import dev.notrobots.authenticator.databinding.ItemFilterTagBinding
import dev.notrobots.authenticator.databinding.ViewToolbarSearchBinding
import dev.notrobots.authenticator.dialogs.*
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.ui.account.AccountActivity
import dev.notrobots.authenticator.ui.backupimport.ImportActivity
import dev.notrobots.authenticator.ui.backupmanager.BackupManagerActivity
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.notrobots.authenticator.ui.settings.SettingsActivity
import dev.notrobots.authenticator.ui.taglist.TagListActivity
import dev.notrobots.authenticator.util.BackupManager
import dev.notrobots.authenticator.util.BackupUtil
import dev.notrobots.authenticator.util.OTPGenerator
import dev.notrobots.authenticator.util.adapterOf
import dev.notrobots.authenticator.widget.BottomSheetListView
import dev.notrobots.preferences2.*
import kotlinx.android.synthetic.main.activity_account_list.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AccountListActivity : AuthenticatorActivity() {
    private val viewModel by viewModels<AccountListViewModel>()
    private val binding by viewBindings<ActivityAccountListBinding>()
    private lateinit var adapter: AccountListAdapter
    private lateinit var recyclerViewDragDropManager: RecyclerViewDragDropManager
    private lateinit var adapterWrapper: RecyclerView.Adapter<*>
    private val toolbar by lazy {
        binding.toolbarLayout.toolbar
    }
    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private val barcodeScanner = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK && it.data != null) {
            val data = it.data!!.getStringArrayListExtra(BarcodeScannerActivity.EXTRA_QR_LIST) ?: emptyList<String>()

            try {
                val backupData = BackupManager.importList(data)

                lifecycleScope.launch {
                    BackupUtil.importBackupData(this@AccountListActivity, backupData, viewModel.accountDao)
                }
            } catch (e: Exception) {
                showInfo(R.string.label_error, e.message)
            }
        }
    }
    private val listAdapterListener = object : AccountListAdapter.Listener {
        override fun onItemClick(account: Account, position: Int, id: Long, adapter: AccountListAdapter) {
            copyToClipboard(OTPGenerator.generate(account, totpClock))
            makeToast("Copied!")
        }

        override fun onItemLongClick(account: Account, position: Int, id: Long, adapter: AccountListAdapter): Boolean {
            toolbar.startActionMode(editActionModeCallback)

            return true
        }

        override fun onItemEditClick(account: Account, position: Int, id: Long, adapter: AccountListAdapter) {
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

        override fun onItemSelectionChange(account: Account, position: Int, id: Long, adapter: AccountListAdapter) {
            actionMode?.title = adapter.selectedItemCount.toString()
        }

        override fun onShareAccount(account: Account, position: Int, id: Long, adapter: AccountListAdapter) {
            val qrCode = QRCode(Account.toUri(account))
            val dialog = QRCodeImageDialog(qrCode, getString(R.string.accessibility_qr_code))

            dialog.show(supportFragmentManager, null)
        }
    }
    private var actionMode: ActionMode? = null
    private val editActionModeCallback = object : ActionMode.Callback {
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
                        DeleteAccountDialog(adapter.selectedItems)
                            .show(supportFragmentManager, null)
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
    private val searchActionModeCallback = object : ActionMode.Callback {
        private var filter: AccountListAdapter.AccountFilter? = null

        override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
            val viewBinding = ViewToolbarSearchBinding.inflate(layoutInflater)
            val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
            val searchView = viewBinding.searchView

            actionMode = mode
            actionMode?.customView = viewBinding.root
            filter = adapter.filter as AccountListAdapter.AccountFilter

            binding.emptyViewText.setText(R.string.empty_view_no_results)
            binding.btnAddAccount.visibility = View.INVISIBLE
            searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName))
            searchView.setIconifiedByDefault(false)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    filter?.filter(newText)
                    return true
                }
            })
            searchView.requestFocus()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            filter?.reset()
            filter = null
            binding.btnAddAccount.visibility = View.VISIBLE
            binding.emptyViewText.setText(R.string.empty_view_no_accounts)
        }
    }
    private var tagFilterMenuItem: MenuItem? = null
    private var tagCount = 0

    @Inject
    protected lateinit var totpClock: TotpClock

    //region Activity lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(toolbar)
        doubleBackPressToExitEnabled = true

        setupFAB()
        setupListAdapter()

        viewModel.sortMode(preferences.getSortMode<SortMode>())
        viewModel.sortMode.observe(this) {
            adapter.sortMode = it
        }
        viewModel.accounts.observe(this) {
            adapter.setItems(it)
        }
        viewModel.tags.observe(this) {
            tagCount = it.size

            if (it.isEmpty()) {
                viewModel.tagIdFilter(-1)
                preferences.putTagIdFilter(-1)
            }
        }
        viewModel.isFilterActive.observe(this) {
            val icon = if (it) R.drawable.ic_filter_list_off else R.drawable.ic_filter

            tagFilterMenuItem?.icon = ContextCompat.getDrawable(this, icon)
        }
        viewModel.tagIdFilter(preferences.getTagIdFilter())

        checkLocalBackupPermissions()

        supportFragmentManager.setFragmentResultListener<DeleteAccountDialog>(this) { _, _ ->
            actionMode?.finish()
        }

        supportFragmentManager.setFragmentResultListener(AddOrEditTagDialog.REQUEST_ADD_TAG, this) { _, _ ->
            makeToast("Tag added")
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()

        binding.listAccounts.adapter = adapterWrapper
        adapter.totpTimer?.start()
        viewModel.tagIdFilter(preferences.getTagIdFilter())
        updateAdapterPreferences()
    }

    override fun onPause() {
        super.onPause()

        adapter.totpTimer?.stop()
        binding.listAccounts.adapter = null
    }

    override fun onStop() {
        super.onStop()

        adapter.totpTimer?.stop()
        binding.listAccounts.adapter = null
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
        tagFilterMenuItem = menu.findItem(R.id.menu_account_list_filter)

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        when (preferences.getSortMode<SortMode>()) {
            SortMode.Custom -> menu.findItem(R.id.menu_account_list_sort_custom).isChecked = true
            SortMode.NameAscending -> menu.findItem(R.id.menu_account_list_sort_name_az_asc).isChecked = true
            SortMode.NameDescending -> menu.findItem(R.id.menu_account_list_sort_name_az_desc).isChecked = true
            SortMode.LabelAscending -> menu.findItem(R.id.menu_account_list_sort_label_az_asc).isChecked = true
            SortMode.LabelDescending -> menu.findItem(R.id.menu_account_list_sort_label_az_desc).isChecked = true
            SortMode.IssuerAscending -> menu.findItem(R.id.menu_account_list_sort_issuer_az_asc).isChecked = true
            SortMode.IssuerDescending -> menu.findItem(R.id.menu_account_list_sort_issuer_az_desc).isChecked = true
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val updateSortModeAndCheckItem = { sortMode: SortMode ->
            preferences.putSortMode(sortMode)
            viewModel.sortMode(sortMode)
            item.isChecked = true
        }

        when (item.itemId) {
            R.id.menu_account_list_filter -> {
                if (tagCount > 0) {
                    showTagFilterDialog()
                }
            }
            R.id.menu_account_list_edit -> {
                toolbar.startActionMode(editActionModeCallback)
            }
            R.id.menu_account_list_search -> {
                toolbar.startActionMode(searchActionModeCallback)
            }
            R.id.menu_account_list_sort_name_az_asc -> updateSortModeAndCheckItem(SortMode.NameAscending)
            R.id.menu_account_list_sort_name_az_desc -> updateSortModeAndCheckItem(SortMode.NameDescending)
            R.id.menu_account_list_sort_label_az_asc -> updateSortModeAndCheckItem(SortMode.LabelAscending)
            R.id.menu_account_list_sort_label_az_desc -> updateSortModeAndCheckItem(SortMode.LabelDescending)
            R.id.menu_account_list_sort_issuer_az_asc -> updateSortModeAndCheckItem(SortMode.IssuerAscending)
            R.id.menu_account_list_sort_issuer_az_desc -> updateSortModeAndCheckItem(SortMode.IssuerDescending)
            R.id.menu_account_list_sort_custom -> updateSortModeAndCheckItem(SortMode.Custom)
            R.id.menu_account_list_backup_export -> {
                lifecycleScope.launch {
                    val accounts = viewModel.accountDao.getAccounts()

                    if (accounts.isNotEmpty()) {
                        requestExport(
                            preferences.getExportLock(),
                            isDeviceSecured()
                        )
                    } else {
                        makeSnackBar("Nothing to export", binding.root)
                    }
                }
            }
            R.id.menu_account_list_backup_import -> startActivity(ImportActivity::class)
            R.id.menu_account_list_backup_manager -> startActivity(BackupManagerActivity::class)
            R.id.menu_account_list_tags -> startActivity(TagListActivity::class)
            R.id.menu_account_list_settings -> startActivity(SettingsActivity::class)
            R.id.menu_clear -> {
                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()
                    viewModel.tagDao.deleteAll()
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
                    accounts.forEach { viewModel.insertAccount(it) }
                }
            }
            R.id.menu_add_test_4 -> {
                val accounts = listOf(
                    Account("F Account", "22334455").apply { label = "AAAA"; issuer = "google.com" },
                    Account("E Account", "33442255").apply { label = "BBBB"; type = OTPType.HOTP },
                    Account("D Account", "66334422").apply { label = "CCCC"; issuer = "github" },
                    Account("C Account", "55443344").apply { label = "DDDD" },
                    Account("B Account", "22335544").apply { label = "EEEE"; type = OTPType.HOTP },
                    Account("A Account", "33445566").apply { }
                )

                lifecycleScope.launch {
                    accounts.forEach { viewModel.insertAccount(it) }
                }
            }
            R.id.menu_add_test_tags -> {
                val tags = listOf(
                    Tag("Work"),
                    Tag("School"),
                    Tag("Gaming"),
                    Tag("Finance"),
                    Tag("XXX"),
                )

                lifecycleScope.launch {
                    viewModel.tagDao.insert(tags)
                }
            }
            R.id.menu_account_list_clear_jobs -> {
                val jobScheduler = getSystemService<JobScheduler>()

                jobScheduler?.cancelAll()
                makeToast("All jobs cancelled")
            }
            R.id.menu_theme_dialog -> {
                CustomThemeDialog(
                    preferences.getCustomAppTheme(),
                    preferences.getCustomAppThemeNightMode(),
                    preferences.getCustomAppThemeTrueBlack()
                ).show(supportFragmentManager, null)
            }
            R.id.menu_prev_theme -> {
                val theme = preferences.getCustomAppTheme<CustomAppTheme>()
                var newThemeIdx = theme.ordinal - 1

                if (newThemeIdx == -1) {
                    newThemeIdx = CustomAppTheme.values().size - 1
                }

                val newTheme = CustomAppTheme.values()[newThemeIdx]

                preferences.putCustomAppTheme(newTheme)
                makeToast("Current theme: $newTheme")
                updateTheme(true)
            }
            R.id.menu_next_theme -> {
                val theme = preferences.getCustomAppTheme<CustomAppTheme>()
                val newThemeIdx = (theme.ordinal + 1) % CustomAppTheme.values().size
                val newTheme = CustomAppTheme.values()[newThemeIdx]

                preferences.putCustomAppTheme(newTheme)
                makeToast("Current theme: $newTheme")
                updateTheme(true)
            }
        }

        return true
    }

    //endregion

    /**
     * Sets up the the [RecyclerView] that shows the accounts and its adapter.
     */
    private fun setupListAdapter() {
        val animator = DraggableItemAnimator()
        val layoutManager = LinearLayoutManager(this)
        val dividerItemDecoration = DividerItemDecoration(this, layoutManager.orientation)

        adapter = AccountListAdapter()
        adapter.totpClock = totpClock
        adapter.setListener(listAdapterListener)
        updateAdapterPreferences()
        adapter.totpTimer = TotpTimer(TOTP_INDICATOR_UPDATE_DELAY)
        adapter.totpTimer?.setListener(object : TotpTimer.Listener {
            override fun onTick(currentTime: Long) {
                if (!adapter.editMode && !adapter.isEmpty) {
                    refreshPinAndTimerForVisibleAccounts(layoutManager)
                }
            }
        })

        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        recyclerViewDragDropManager.attachRecyclerView(list_accounts)
        recyclerViewDragDropManager.setInitiateOnTouch(true)
        recyclerViewDragDropManager.isCheckCanDropEnabled = true
//        recyclerViewDragDropManager.setDraggingItemShadowDrawable(
//            ContextCompat.getDrawable(this, R.drawable.material_shadow_z3) as NinePatchDrawable?
//        )

        adapterWrapper = recyclerViewDragDropManager.createWrappedAdapter(adapter)
        binding.listAccounts.layoutManager = layoutManager
        binding.listAccounts.itemAnimator = animator
        binding.listAccounts.addItemDecoration(dividerItemDecoration)
        binding.listAccounts.setEmptyView(binding.emptyView)

//        if (supportsViewElevation()) {
//            // Lollipop or later has native drop shadow feature. ItemShadowDecorator is not required.
//        } else {
//            mRecyclerView.addItemDecoration(ItemShadowDecorator((ContextCompat.getDrawable(requireContext(), R.drawable.material_shadow_z1) as NinePatchDrawable?)!!))
//        }
//        binding.listAccounts.addItemDecoration(SimpleListDividerDecorator(ContextCompat.getDrawable(requireContext(), R.drawable.list_divider_h), true))
    }

    /**
     * Refreshes the pins and timers for all the visible accounts in the list without redrawing the rows.
     */
    private fun refreshPinAndTimerForVisibleAccounts(layoutManager: LinearLayoutManager) {
        val first = layoutManager.findFirstVisibleItemPosition()
        val last = layoutManager.findLastVisibleItemPosition()

        for (i in first..last) {
            val holder = binding.listAccounts.findViewHolderForAdapterPosition(i)
            val account = adapter.getItemOrNull(i)

            if (holder is AccountListAdapter.TimerAccountViewHolder && account != null) {
                adapter.refreshPin(holder, account)
                adapter.refreshTimer(holder, account)
            }
        }
    }

    /**
     * Sets up the Floating Action Button.
     */
    private fun setupFAB() {
        binding.btnAddAccountQr.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)

            barcodeScanner.launch(intent)
            btn_add_account.close(true)
        }
        binding.btnAddAccountUrl.setOnClickListener {
            AccountUriDialog(R.string.label_add_account)
                .show(supportFragmentManager, null)

            btn_add_account.close(true)
        }
        binding.btnAddAccountCustom.setOnClickListener {
            startActivity(AccountActivity::class)
            btn_add_account.close(true)
        }
        binding.btnAddAccountTag.setOnClickListener {
            AddOrEditTagDialog()
                .show(supportFragmentManager, null)
        }
    }

    /**
     * Shows the tag filter BottomSheetDialog.
     */
    private fun showTagFilterDialog() {
        val sheet = BottomSheetDialog(this)
        val listView = BottomSheetListView(this)

        listView.choiceMode = BottomSheetListView.CHOICE_MODE_SINGLE
        sheet.setTitle("Filter Tag")

        //TODO: Redesign the layout

        viewModel.tags.value?.let {
            val list = mutableListOf<Tag?>(null)
            list.addAll(it)

            var selectedItem = list.indexOfFirstOrNull {
                it?.tagId == viewModel.tagIdFilter()
            } ?: 0
            val adapter = adapterOf<Tag?, ItemFilterTagBinding>(this, list) { tag, pos, binding ->
                binding.text.text = tag?.name ?: "No Filter"
            }

            listView.adapter = adapter
            listView.setItemChecked(selectedItem, true)
            listView.setOnItemClickListener { _, _, pos, _ ->
                val item = list[pos]
                val id = item?.tagId ?: -1

                if (selectedItem != pos) {
                    selectedItem = pos
                    listView.setItemChecked(selectedItem, true)
                }

                viewModel.tagIdFilter(id)
                preferences.putTagIdFilter(id)
                sheet.dismiss()
            }
        }

        sheet.setTitle("Filter tags")
        sheet.setContentView(listView)
        sheet.show()
    }

    /**
     * Checks if the write permissions for the local backup path were revoked
     * by the user while the app was closed.
     */
    private fun checkLocalBackupPermissions() {
        val path = preferences.getLocalBackupPath().toUri()
        val enabled = preferences.getLocalBackupEnabled()

        if (enabled && !contentResolver.isPersistedPermissionGranted(path)) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Permission revoked")
                .setMessage("Permission to write backups to $path was revoked.\n\nAuthenticator won't be able to perform backups")
                .setPositiveButton("Launch backup manager") { _, _ ->
                    startActivity(BackupManagerActivity::class)
                }
                .setNegativeButton("Disable backup") { _, _ ->
                    preferences.putLocalBackupEnabled(false)
                }
                .setNeutralButton("Do nothing", null)
                .create()
                .show()
        }
    }

    /**
     * Updates some of the adapter's settings based on the user's preferences.
     */
    private fun updateAdapterPreferences() {
        adapter.collapseIcons = preferences.getCollapseIcons()
        adapter.collapsePins = preferences.getCollapsePins()
        adapter.totpIndicatorType = preferences.getTotpIndicator()
        adapter.clearTextEnabled = !preferences.getHidePins()
        adapter.hidePinsOnChange = preferences.getHidePinsOnChange()
        adapter.totpIndicatorType = preferences.getTotpIndicator()
        adapter.pinTextSize = preferences.getPinTextSize()
    }
}