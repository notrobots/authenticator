package dev.notrobots.authenticator.ui.accountlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.core.util.rangeTo
import androidx.core.view.children
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.android.widget.advrecyclerview.draggable.RecyclerViewDragDropManager
import com.h6ah4i.android.widget.advrecyclerview.expandable.RecyclerViewExpandableItemManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.copyToClipboard
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.util.logd
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.BaseActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.dialogs.*
import dev.notrobots.authenticator.extensions.absoluteRangeTo
import dev.notrobots.authenticator.google.CountdownIndicator
import dev.notrobots.authenticator.google.TotpClock
import dev.notrobots.authenticator.google.TotpCountdownTask
import dev.notrobots.authenticator.google.TotpCounter
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.ui.account.AccountActivity
import dev.notrobots.authenticator.ui.backup.BackupActivity
import dev.notrobots.authenticator.ui.backupexport.ExportActivity
import dev.notrobots.authenticator.ui.backupexport.ExportConfigActivity
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import kotlinx.android.synthetic.main.activity_account_list.*
import kotlinx.coroutines.launch
import java.util.concurrent.Flow
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
    private val accountExporter = AccountExporter()

    /** Counter used for generating TOTP verification codes.  */
    private val totpCounter: TotpCounter = TotpCounter(30)

    /** Clock used for generating TOTP verification codes.  */
    private val totpClock: TotpClock by lazy {
        TotpClock(this)
    }
    private val scanBarcode = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data != null) {
                val uri = it.data!!.getStringExtra(BarcodeScannerActivity.EXTRA_QR_DATA) ?: ""
                val accounts = accountExporter.import(uri)  //TODO: Try-catch

                if (accounts.size > 1) {        //TODO: Show a special dialog that tells the user a backup is being imported
                    logd("Importing backup")
                }

                for (account in accounts) {
                    try {
                        addOrReplaceAccount(account)
                    } catch (e: Exception) {
                        //FIXME: This dialog sucks ass
                        val dialog = ErrorDialog()

                        dialog.setErrorMessage(e.message)
                        dialog.show(supportFragmentManager, null)
                    }
                }
            }
        }
    }
    private var actionMode: ActionMode? = null

    private val accountListAdapterListener = object : AccountListAdapter.Listener {
        override fun onItemClick(account: Account, id: Long, adapter: AccountListAdapter) {
            if (actionMode != null) {
                if (!account.isSelected && adapter.selectedAccounts.isEmpty()) {
                    actionMode?.finish()
                }
                actionMode?.title = adapter.selectedAccounts.size.toString()
            } else {
                copyToClipboard(OTPGenerator.generate(account))
                makeToast("Copied!")
            }
        }

        override fun onItemLongClick(account: Account, id: Long, adapter: AccountListAdapter): Boolean {
            if (actionMode == null) {
                startSupportActionMode(accountActionMode)
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

//                for (row in updatedRows) {
//                    viewModel.accountDao.update(adapter.getAccount(row.key, row.value))
//                }
            }
        }

        override fun onGroupClick(group: AccountGroup, id: Long, adapter: AccountListAdapter) {
            if (actionMode != null) {
                if (!group.isSelected && adapter.selectedGroups.isEmpty()) {
                    actionMode?.finish()
                }

                actionMode?.title = this@AccountListActivity.adapter.selectedGroups.size.toString()
            }
        }

        override fun onGroupLongClick(group: AccountGroup, id: Long, adapter: AccountListAdapter): Boolean {
            if (actionMode == null) {
                startSupportActionMode(groupActionMode)
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
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.menu_account_list_item, menu)
            actionMode = mode
            actionMode?.title = adapter.selectedAccounts.size.toString()
            adapter.editMode = AccountListAdapter.EditMode.Item
            adapter.notifyDataSetChanged()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            return true
        }

        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.menu_account_remove -> {
                    if (adapter.selectedAccounts.isNotEmpty()) {
                        val accounts = adapter.selectedAccounts
                        val dialog = DeleteAccountDialog(accounts)

                        dialog.onConfirmListener = {
                            lifecycleScope.launch {
                                viewModel.accountDao.delete(accounts)
                            }
                            actionMode?.finish()
                        }
                        dialog.show(supportFragmentManager, null)
                    } else {
                        makeToast("No accounts selected")
                    }
                }
                R.id.menu_account_export -> {
                    if (adapter.selectedAccounts.isNotEmpty()) {
                        val accounts = ArrayList(adapter.selectedAccounts)

                        startActivity(ExportActivity::class) {
                            putExtra(ExportConfigActivity.EXTRA_ACCOUNT_LIST, accounts)
                        }
                        actionMode?.finish()
                    } else {
                        makeToast("No accounts selected")
                    }
                }
                R.id.menu_account_selectall -> {
                    if (adapter.accounts.isNotEmpty()) {
                        adapter.selectAllAccounts()
                        actionMode?.title = adapter.selectedAccounts.size.toString()
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
            adapter.clearSelectedAccounts()
        }
    }
    private val groupActionMode = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(R.menu.menu_account_list_group, menu)
            actionMode = mode
            actionMode?.title = adapter.selectedGroups.size.toString()
            adapter.editMode = AccountListAdapter.EditMode.Group
            adapter.notifyDataSetChanged()

            collapseGroups()

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_group_selectall -> {
                    if (adapter.groups.isNotEmpty()) {
                        adapter.selectAllGroups()
                        actionMode?.title = adapter.selectedGroups.size.toString()
                    } else {
                        makeToast("Nothing to select")
                    }
                }
                R.id.menu_group_remove -> { //TODO: Let the user keep the accounts, either unpack the group (move to default) or move to a specific group
                    val selectedGroups = adapter.selectedGroups
                    val selectedGroupIDs = selectedGroups.map { it.id }
                    val accounts = adapter.accounts.filter { it.groupId in selectedGroupIDs }   //TODO: Use viewModel
                    val dialog = DeleteGroupDialog(selectedGroups.size, accounts.size)

                    dialog.onConfirmListener = {
                        lifecycleScope.launch {
                            viewModel.accountDao.delete(accounts)
                            viewModel.accountGroupDao.delete(selectedGroups)
                        }
                    }
                    dialog.show(supportFragmentManager, null)
                }
                R.id.menu_group_unpack -> {

                }
            }

            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            adapter.editMode = AccountListAdapter.EditMode.Disabled
            adapter.clearSelectedGroups()

            resetGroupsExpandState()
        }
    }

    //region Activity lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_list)

        btn_add_account_qr.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)

            scanBarcode.launch(intent)
            btn_add_account.close(true)
        }
        btn_add_account_url.setOnClickListener {
            val dialog = AccountURLDialog()

            dialog.onConfirmListener = {
                try {
                    addOrReplaceAccount(accountExporter.import(it).first())
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

    override fun onPause() {
        super.onPause()

//        lifecycleScope.launch {
//            viewModel.accountDao.update(adapter.accounts)
//            viewModel.accountGroupDao.update(adapter.groups)
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
        actionMode?.finish()
    }

    //endregion

    override fun isDoubleBackPressToExitEnabled(): Boolean {
        return true
    }

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
                    Account("Account 5", "22444455").apply { groupId = 4 },
                    Account("Account 6", "22774477").apply { groupId = 4 },
                    Account("Account 7", "77334455").apply { },
                    Account("Account 8", "22223355").apply { }
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
                startSupportActionMode(accountActionMode)
            }
            R.id.menu_account_list_edit_group -> {
                startSupportActionMode(groupActionMode)
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

    private fun collapseGroups() {
        recyclerViewExpandableItemManager.collapseAll()
    }

    private fun resetGroupsExpandState() {
        for ((index, group) in adapter.groups.withIndex()) {
            val expandState = recyclerViewExpandableItemManager.isGroupExpanded(index)

            if (group.isExpanded && !expandState) {
                recyclerViewExpandableItemManager.expandGroup(index)
            } else if (expandState) {
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
                //TODO: Find out why the exception can be caught here but not if it's thrown directly from the coroutine
                viewModel.addAccount(account)
            }
        }
    }
}