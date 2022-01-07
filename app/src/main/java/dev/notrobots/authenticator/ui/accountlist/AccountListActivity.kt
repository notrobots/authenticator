package dev.notrobots.authenticator.ui.accountlist

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.core.content.edit
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.BaseActivity
import dev.notrobots.androidstuff.extensions.copyToClipboard
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.androidstuff.util.showInfo
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.databinding.ActivityAccountBinding
import dev.notrobots.authenticator.databinding.ActivityAccountListBinding
import dev.notrobots.authenticator.dialogs.*
import dev.notrobots.authenticator.extensions.absoluteRangeTo
import dev.notrobots.authenticator.google.CountdownIndicator
import dev.notrobots.authenticator.google.TotpClock
import dev.notrobots.authenticator.google.TotpCountdownTask
import dev.notrobots.authenticator.google.TotpCounter
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.ui.account.AccountActivity
import dev.notrobots.authenticator.ui.backup.BackupActivity
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.notrobots.authenticator.ui.backupimportresult.ImportResultActivity
import kotlinx.android.synthetic.main.activity_account_list.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class AccountListActivity : BaseActivity() {
    private val viewModel by viewModels<AccountListViewModel>()
    private lateinit var adapter: AccountListAdapter
    private lateinit var recyclerViewExpandableItemManager: RecyclerViewExpandableItemManager
    private lateinit var recyclerViewDragDropManager: RecyclerViewDragDropManager
    private lateinit var adapterWrapper: RecyclerView.Adapter<*>
    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(this)
    }
    private var totpCountdownTask: TotpCountdownTask? = null

    /** Counter used for generating TOTP verification codes.  */
    private val totpCounter: TotpCounter = TotpCounter(30)

    /** Clock used for generating TOTP verification codes.  */
    private val totpClock: TotpClock by lazy {
        TotpClock(this)
    }
    private val scanBarcode = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data != null) {
                val uri = Uri.parse(it.data!!.getStringExtra(BarcodeScannerActivity.EXTRA_QR_DATA) ?: "")

                try {
                    if (AccountExporter.isBackup(uri)) {
                        val data = AccountExporter().import(uri)    //FIXME: This throws an unreadable exception for the final user

                        startActivity(ImportResultActivity::class) {
                            putExtra(ImportResultActivity.EXTRA_DATA, data)
                        }
                    } else {
                        val account = AccountExporter.parseAccount(uri)  //TODO: Single Group import?

                        addOrReplaceAccount(account)
                    }
                } catch (e: Exception) {
                    showInfo(this, "Error", e.message)
                }
            }
        }
    }
    private var actionMode: ActionMode? = null

    private val accountListAdapterListener = object : AccountListAdapter.Listener {
        override fun onItemClick(account: Account, id: Long, adapter: AccountListAdapter) {
            if (actionMode != null) {   //TODO: Handle the selection in the adapter and notify that the item has been selected
                if (!account.isSelected && adapter.selectedItemCount == 0) {
                    actionMode?.finish()
                }
                actionMode?.title = adapter.selectedItemCount.toString()
            } else {
                copyToClipboard(OTPGenerator.generate(account))
                makeToast("Copied!")
            }
        }

        override fun onItemLongClick(account: Account, id: Long, adapter: AccountListAdapter): Boolean {
            if (actionMode == null) {
                binding.toolbarLayout.toolbar.startActionMode(accountActionMode)
            }

            return true
        }

        override fun onItemEdit(account: Account, id: Long, adapter: AccountListAdapter) {
            startActivity(AccountActivity::class) {
                putExtra(AccountActivity.EXTRA_ACCOUNT, account)
            }
            actionMode?.finish()
        }

        override fun onItemMoved(updatedRows: Map<Int, Int>) {
            lifecycleScope.launch {
                viewModel.accountDao.update(adapter.accounts)

//                for (row in updatedRows) {    //TODO: Do this
//                    viewModel.accountDao.update(adapter.getAccount(row.key, row.value))
//                }
            }
        }

        override fun onItemSelected(account: Account, groupPosition: Int, childPosition: Int) {
            val packedPosition = RecyclerViewExpandableItemManager.getPackedPositionForChild(groupPosition, childPosition)
            val flatPosition = recyclerViewExpandableItemManager.getFlatPosition(packedPosition)
            val viewHolder = list_accounts.findViewHolderForAdapterPosition(flatPosition)

            viewHolder?.itemView?.isSelected = account.isSelected
        }

        override fun onGroupClick(group: AccountGroup, id: Long, adapter: AccountListAdapter) {
            if (actionMode != null) {
                if (!group.isSelected && adapter.selectedItemCount == 0) {
                    actionMode?.finish()
                }
                actionMode?.title = this@AccountListActivity.adapter.selectedItemCount.toString()
            }
        }

        override fun onGroupLongClick(group: AccountGroup, id: Long, adapter: AccountListAdapter): Boolean {
            if (actionMode == null) {
                binding.toolbarLayout.toolbar.startActionMode(accountActionMode)
            }

            return true
        }

        override fun onGroupEdit(group: AccountGroup, id: Long, adapter: AccountListAdapter) {
            val dialog = AddAccountGroupDialog()

            dialog.text = group.name
            dialog.onConfirmListener = { name ->
                if (name == group.name) {
                    dialog.dismiss()
                } else {
                    lifecycleScope.launch {
                        try {
                            group.name = name   //FIXME: Isn't this changing the name even if there's an exception
                            viewModel.addGroup(group)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            dialog.error = e.message
                        }
                    }
                }
            }
            dialog.show(supportFragmentManager, null)
        }

        override fun onGroupMoved(fromGroupPosition: Int, toGroupPosition: Int) {
            lifecycleScope.launch {
                for (i in fromGroupPosition absoluteRangeTo toGroupPosition) {
                    viewModel.accountGroupDao.update(adapter.getGroup(i))
                }
            }
        }
    }
    private val accountActionMode = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(R.menu.menu_account_list_item, menu)   //TODO: Merge the two menus in one single xml file
            actionMode = mode
            actionMode?.title = adapter.selectedItemCount.toString()
            adapter.editMode = AccountListAdapter.EditMode.Item
            adapter.notifyDataSetChanged()
            recyclerViewExpandableItemManager.expandAll()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_account_remove -> {
                    if (adapter.selectedItemCount > 0) {
                        val dialog = DeleteGroupDialog(adapter.selectedGroupCount, adapter.selectedAccountCount) {
                            deleteGroups(adapter.selectedGroups)
                            deleteAccounts(adapter.selectedAccounts)
                            actionMode?.finish()
                        }
                        dialog.show(supportFragmentManager, null)
                    } else {
                        makeToast("No items selected")
                    }
                }
                R.id.menu_account_selectall -> {
                    if (adapter.hasSelectableItems) {
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
            adapter.editMode = AccountListAdapter.EditMode.Disabled
            adapter.clearSelected()
            restoreGroupsExpandState()
        }
    }
    private val binding by viewBindings<ActivityAccountListBinding>()

    //region Activity lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)

        doubleBackPressToExitEnabled = true

        btn_add_account_qr.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)

            scanBarcode.launch(intent)
            btn_add_account.close(true)
        }
        btn_add_account_url.setOnClickListener {
            val dialog = AccountURLDialog()

            dialog.onConfirmListener = {
                val uri = Uri.parse(it)

                try {
                    if (AccountExporter.isBackup(uri)) {
                        val data = AccountExporter().import(uri)

                        startActivity(ImportResultActivity::class) {
                            putExtra(ImportResultActivity.EXTRA_DATA, data)
                        }
                    } else {
                        val account = AccountExporter.parseAccount(uri)

                        addOrReplaceAccount(account)
                    }
                    dialog.dismiss()
                } catch (e: Exception) {
                    dialog.error = e.message
                }
            }
            dialog.show(supportFragmentManager, null)
            btn_add_account.close(true)
        }
        btn_add_account_custom.setOnClickListener {
            startActivity(AccountActivity::class)
            btn_add_account.close(true)
        }
        btn_add_account_group.setOnClickListener {
            val dialog = AddAccountGroupDialog()

            dialog.onConfirmListener = {
                val group = AccountGroup(it)

                lifecycleScope.launch {
                    try {
                        viewModel.addGroup(group)
                        dialog.dismiss()
                    } catch (e: Exception) {
                        dialog.error = e.message
                    }
                }
            }
            dialog.show(supportFragmentManager, null)
            btn_add_account.close(true)
        }

        setupListAdapter()
        createDefaultGroup()

        viewModel.groupsWithAccount.observe(this) {
            for (groupWithAccount in it) {
                groupWithAccount.accounts.sortBy { it.order }
            }

            adapter.setItems(it)
        }
    }

    override fun onStart() {
        super.onStart()

        if (totpCountdownTask != null) {
            totpCountdownTask!!.stop()
            totpCountdownTask = null
        }

        totpCountdownTask = TotpCountdownTask(
            totpCounter,
            totpClock,
            100
        )
        totpCountdownTask!!.setListener(object : TotpCountdownTask.Listener {
            override fun onTotpCountdown(millisRemaining: Long) {
                if (isFinishing) {
                    // No need to reach to this even because the Activity is finishing anyway
                    return
                }
                setTotpCountdownPhaseFromTimeTillNextValue(millisRemaining)
            }

            override fun onTotpCounterValueChanged() {
                if (isFinishing) {
                    // No need to reach to this even because the Activity is finishing anyway
                    return
                }

                adapter.notifyDataSetChanged()
            }
        })
        totpCountdownTask!!.startAndNotifyListener()
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_account_list_toggle_pins).title = if (adapter.showPins) {
            "Hide pins"
        } else {
            "Show pins"
        }

        menu.findItem(R.id.menu_account_list_toggle_icons).title = if (adapter.showIcons) {
            "Hide icons"
        } else {
            "Show icons"
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> {
                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()
                    viewModel.accountGroupDao.deleteAll()
                    viewModel.accountGroupDao.insert(AccountGroup.DEFAULT_GROUP)
                }
            }
            R.id.menu_refresh -> {
                adapter.notifyDataSetChanged()
            }
            R.id.menu_add_test -> {
                val groups = listOf(
                    AccountGroup("Group 1").apply { id = 2; order = 0 },
                    AccountGroup("Group 2").apply { id = 3; order = 1 },
                    AccountGroup("Group 3").apply { id = 4; order = 2 }
                )
                val accounts = listOf(
                    Account("Account 1", "22334455").apply { groupId = 2 },
                    Account("Account 2", "22334466").apply { groupId = 2 },
                    Account("Account 3", "22332277").apply { groupId = 3 },
                    Account("Account 4", "22334455").apply { groupId = 3 },
                    Account("Account 5", "22444455").apply { },
                    Account("Account 6", "22774477").apply { }
                )
                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()
                    viewModel.accountGroupDao.deleteAll()

                    groups.forEach { viewModel.addGroup(it) }
                    accounts.forEach { viewModel.addAccount(it) }

                    viewModel.accountGroupDao.insert(AccountGroup.DEFAULT_GROUP)
                }
            }
            R.id.menu_add_test_2 -> {
                val groups = listOf(
                    AccountGroup("Work").apply { id = 2; order = 0 },
                    AccountGroup("Personal").apply { id = 3; order = 1 }
                )
                val accounts = listOf(
                    Account("Max", "22334455").apply { label = "Twitter"; groupId = 2 },
                    Account("Max", "22334466").apply { label = "Steam"; groupId = 2 },
                    Account("Max", "22332277").apply { label = "Amazon"; groupId = 2 },
                    Account("Max", "22334455").apply { label = "EGS"; groupId = 3 },
                    Account("Max", "22444455").apply { label = "Github"; groupId = 3 },
                    Account("JohnDoe", "22774477"),
                    Account("MarioRossi", "77334455"),
                    Account("JaneDoe", "22223355")
                )
                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()
                    viewModel.accountGroupDao.deleteAll()

                    groups.forEach { viewModel.addGroup(it) }
                    accounts.forEach { viewModel.addAccount(it) }

                    viewModel.accountGroupDao.insert(AccountGroup.DEFAULT_GROUP)
                }
            }
            R.id.menu_account_list_edit -> {
                binding.toolbarLayout.toolbar.startActionMode(accountActionMode)
            }
            R.id.menu_account_list_toggle_pins -> {
                val showPins = preferences.getBoolean(Preferences.SHOW_PINS, true)

                adapter.showPins = !showPins
                preferences.edit {
                    putBoolean(Preferences.SHOW_PINS, !showPins)
                }
            }
            R.id.menu_account_list_toggle_icons -> {
                val showIcons = preferences.getBoolean(Preferences.SHOW_ICONS, true)

                adapter.showIcons = !showIcons
                preferences.edit {
                    putBoolean(Preferences.SHOW_ICONS, !showIcons)
                }
            }
            R.id.menu_account_list_backup -> {
                startActivity(BackupActivity::class)
            }
        }

        return true
    }

    //endregion

    private fun setTotpCountdownPhaseFromTimeTillNextValue(millisRemaining: Long) {
        setTotpCountdownPhase(millisRemaining.toDouble() / TimeUnit.SECONDS.toMillis(totpCounter.timeStep))
    }

    private fun setTotpCountdownPhase(phase: Double) {
        if (actionMode == null) {
            for (child in list_accounts.children) {
                val indicator = child.findViewById<CountdownIndicator>(R.id.pb_phase)

                indicator?.setPhase(phase)
            }
        }
    }

    private fun createDefaultGroup() {
        lifecycleScope.launch {
            val defaultGroup = viewModel.accountGroupDao.getGroup(Account.DEFAULT_GROUP_ID)

            if (defaultGroup == null) {
                viewModel.accountGroupDao.insert(AccountGroup.DEFAULT_GROUP)
                logd("Default group added")
            } else {
                logd("Default group already added")
            }
        }
    }

    private fun setupListAdapter() {
        adapter = AccountListAdapter()
        adapter.setListener(accountListAdapterListener)

        recyclerViewDragDropManager = RecyclerViewDragDropManager()
        recyclerViewDragDropManager.attachRecyclerView(list_accounts)
        recyclerViewDragDropManager.setInitiateOnTouch(true)
        recyclerViewDragDropManager.isCheckCanDropEnabled = true
        recyclerViewExpandableItemManager = RecyclerViewExpandableItemManager(null)
        recyclerViewExpandableItemManager.setOnGroupCollapseListener { groupPosition, _, _ ->
            adapter.groups[groupPosition].isExpanded = false
        }
        recyclerViewExpandableItemManager.setOnGroupExpandListener { groupPosition, _, _ ->
            adapter.groups[groupPosition].isExpanded = true
        }
        recyclerViewExpandableItemManager.attachRecyclerView(list_accounts)

        adapterWrapper = recyclerViewExpandableItemManager.createWrappedAdapter(adapter)
        adapterWrapper = recyclerViewDragDropManager.createWrappedAdapter(adapterWrapper)

        list_accounts.adapter = adapterWrapper
        list_accounts.layoutManager = LinearLayoutManager(this)
    }

    /**
     * Restores the expand states of each group
     */
    private fun restoreGroupsExpandState() {
        for ((index, group) in adapter.groups.withIndex()) {
            if (group.isExpanded) {
                recyclerViewExpandableItemManager.expandGroup(index)
            } else {
                recyclerViewExpandableItemManager.collapseGroup(index)
            }
        }
    }

    /**
     * Adds or replace the given [account].
     */
    private fun addOrReplaceAccount(account: Account) {
        lifecycleScope.launch {
            // If the count is greater than 0, that means there's one other account
            // with the same name and issuer, ask the user if they want to overwrite it
            if (viewModel.checkIfAccountExists(account)) {
                val dialog = ReplaceAccountDialog() //TODO Let the user keep both accounts

                dialog.onReplaceListener = {
                    lifecycleScope.launch {
                        // If the given account has the default id, we need to grab the id
                        // corresponding to the given account's name, issuer and label and set it
                        // to the given account so that it can be updated
                        if (account.id == BaseAccount.DEFAULT_ID) {
                            val id = viewModel.accountDao.getAccount(account.name, account.label, account.issuer).id

                            account.id = id
                        }

                        viewModel.accountDao.update(account)
                    }
                    logd("Replacing existing account: $account")
                }
                dialog.show(supportFragmentManager, null)
            }
            // No account with the same name and issuer was found in the database,
            // insert the given account
            else {
                viewModel.addAccount(account)
            }
        }
    }

    /**
     * Deletes the given [groups] and its related accounts
     */
    private fun deleteGroups(groups: List<AccountGroup>) {
        lifecycleScope.launch {
            adapter.removeGroups(groups)
            adapter.reorderGroups()
            adapter.notifyDataSetChanged()
            viewModel.accountGroupDao.deleteGroupWithAccounts(groups)
            viewModel.accountGroupDao.update(adapter.groups)
        }
    }

    /**
     * Deletes the given [accounts]
     */
    private fun deleteAccounts(accounts: List<Account>) {
        lifecycleScope.launch {
            val groupedAccounts = accounts.groupBy { it.groupId }

            for (group in groupedAccounts) {
                val groupPosition = adapter.getGroupPosition(group.key)

                adapter.removeAccounts(groupPosition, group.value)
                adapter.reorderAccounts(groupPosition)
            }

            adapter.notifyDataSetChanged()
            viewModel.accountDao.delete(accounts)
            viewModel.accountDao.update(adapter.accounts)
        }
    }
}