package dev.notrobots.authenticator.ui.accountlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.content.edit
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.copyToClipboard
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.util.*
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.BaseActivity
import dev.notrobots.authenticator.data.Preferences
import dev.notrobots.authenticator.dialogs.*
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.google.CountdownIndicator
import dev.notrobots.authenticator.google.TotpClock
import dev.notrobots.authenticator.google.TotpCountdownTask
import dev.notrobots.authenticator.google.TotpCounter
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import dev.notrobots.authenticator.models.BaseAccount
import dev.notrobots.authenticator.models.OTPProvider
import dev.notrobots.authenticator.ui.account.AccountActivity
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.notrobots.authenticator.ui.export.ExportActivity
import dev.notrobots.authenticator.ui.export.ExportConfigActivity
import kotlinx.android.synthetic.main.activity_account_list.*
import kotlinx.android.synthetic.main.dialog_account_url.view.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

@AndroidEntryPoint
class AccountListActivity : BaseActivity() {
    private val viewModel by viewModels<AccountListViewModel>()
    private val adapter by lazy {
        val config = ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(false)
            .build()

        ConcatAdapter(config)
    }
    private val touchHelper by lazy {
        ItemTouchHelper(touchHelperCallback)
    }
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
                val url = it.data!!.getStringExtra(BarcodeScannerActivity.EXTRA_QR_DATA) ?: ""

                try {
                    addOrReplaceAccount(Account.parse(url))
                } catch (e: Exception) {
                    //FIXME: This dialog sucks ass
                    val dialog = ErrorDialog()

                    dialog.setErrorMessage(e.message)
                    dialog.show(supportFragmentManager, null)
                }
            }
        }
    }

    private val touchHelperCallback = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            val adapterIndex = adapter.adapters.indexOf(viewHolder.bindingAdapter)

            // Disable movement for the group header if the the group edit mode is not enabled
            if (viewHolder.bindingAdapterPosition == 0 && adapter.editMode != AccountListAdapter.EditMode.Group) {
                return 0
            }

            // Disable movement for the group at the bottom but not for its items
            if (adapterIndex == adapter.adapters.lastIndex && adapter.editMode == AccountListAdapter.EditMode.Group) {
                return 0
            }

            return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

        override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
            val sourceAdapter = viewHolder.bindingAdapter as AccountListAdapter
            val targetAdapter = target.bindingAdapter as AccountListAdapter
            val targetAdapterIndex = adapter.adapters.indexOf(targetAdapter)
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition

            // Disable movement if the current group is dragged below the default group,
            // that is at the bottom of the list
            if (targetAdapterIndex == adapter.adapters.lastIndex && adapter.editMode == AccountListAdapter.EditMode.Group) {
                return false
            }

            // If the current moved view is the header
            // swap the adapters inside the main ConcatAdapter
            if (from == 0) {
                val sourceIndex = adapter.adapters.indexOf(sourceAdapter)
                val targetIndex = adapter.adapters.indexOf(targetAdapter)

                adapter.swap(sourceIndex, targetIndex)
            } else {
                // Items can't be swapped with the header
                // or dragged to another group
                if (to == 0 || sourceAdapter != targetAdapter) {
                    return false
                }

                sourceAdapter.swap(from, to)
            }

            return true
        }
    }
    private var actionMode: ActionMode? = null

    private val accountActionMode = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            menuInflater.inflate(R.menu.menu_account_list_item, menu)
            actionMode = mode
            actionMode?.title = adapter.selectedAccounts.size.toString()
            adapter.setEditMode(AccountListAdapter.EditMode.Item)
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
                        val intent = Intent(this@AccountListActivity, ExportActivity::class.java)

                        intent.putExtra(ExportConfigActivity.EXTRA_ACCOUNT_LIST, accounts)

                        startActivity(intent)
                        actionMode?.finish()
                    } else {
                        makeToast("No accounts selected")
                    }
                }
                R.id.menu_account_selectall -> {
                    if (adapter.accounts.isNotEmpty()) {
                        for (account in adapter.accounts) {
                            account.isSelected = true
                        }
                        actionMode?.title = adapter.selectedAccounts.size.toString()
                        adapter.notifyDataSetChanged()
                    } else {
                        makeToast("Nothing to select")
                    }
                }
            }
//
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            adapter.setEditMode(AccountListAdapter.EditMode.Disabled)
            adapter.clearSelectedAccounts()
        }
    }
    private val groupActionMode = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            menuInflater.inflate(R.menu.menu_account_list_group, menu)
            actionMode = mode
            actionMode?.title = adapter.selectedGroups.size.toString()
            adapter.setEditMode(AccountListAdapter.EditMode.Group)
            adapter.notifyAllDataSetChanged()   //FIXME: This or the regular notify??

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu?): Boolean {
            return false
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_group_selectall -> {
                    adapter.selectAllGroups()
                }
                R.id.menu_group_remove -> { //TODO: Let the user keep the accounts, either unpack the group (move to default) or move to a specific group
                    val selectedGroups = adapter.selectedGroups
                    val selectedGroupIDs = selectedGroups.map { it.id }
                    val accounts = adapter.groupsWithAccounts.flatMap { it.accounts.filter { it.groupId in selectedGroupIDs } }
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
            adapter.setEditMode(AccountListAdapter.EditMode.Disabled)
            adapter.clearSelectedGroups()
        }
    }
//    private var showPins = true

    //region Activity lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_list)

        list_accounts.adapter = adapter
        list_accounts.layoutManager = LinearLayoutManager(this)

        btn_add_account_qr.setOnClickListener {
            val intent = Intent(this, BarcodeScannerActivity::class.java)

            scanBarcode.launch(intent)
            btn_add_account.close(true)
        }
        btn_add_account_url.setOnClickListener {
            val dialog = AccountURLDialog()

            dialog.onConfirmListener = {
                try {
                    addOrReplaceAccount(Account.parse(it))
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

        createDefaultGroup()
        viewModel.groupsWithAccount.observe(this) {
            //FIXME: This needs to be optimized
            val adapters = it.map {
                AccountListAdapter(it).apply {
                    touchHelper = this@AccountListActivity.touchHelper
                    showPins = preferences.getBoolean(Preferences.SHOW_PINS, true)
                    setListener(object : AccountListAdapter.Listener {
                        override fun onClick(account: Account, position: Int, id: Long) {
                            if (actionMode != null) {
                                account.toggleSelected()

                                if (!account.isSelected && adapter.selectedAccounts.isEmpty()) {
                                    actionMode?.finish()
                                }

                                notifyItemChanged(position)
                                actionMode?.title = adapter.selectedAccounts.size.toString()
                            } else {
                                copyToClipboard(OTPProvider.generate(account))
                                makeToast("Copied!")
                            }
                        }

                        override fun onLongClick(account: Account, position: Int, id: Long): Boolean {
                            if (actionMode == null) {
                                account.isSelected = true
                                notifyItemChanged(position)
                                startSupportActionMode(accountActionMode)
                            }

                            return true
                        }

                        override fun onEdit(account: Account, position: Int, id: Long) {
                            startActivity(AccountActivity::class) {
                                putExtra(AccountActivity.EXTRA_ACCOUNT, account)
                            }
                            actionMode?.finish()
                        }

                        override fun onClick(group: AccountGroup, position: Int, id: Long) {
                            if (adapter.editMode == AccountListAdapter.EditMode.Group) {
                                actionMode?.title = adapter.selectedGroups.size.toString()
                            }
                        }

                        override fun onLongClick(group: AccountGroup, position: Int, id: Long): Boolean {
                            //TODO: Edit groups

                            return true
                        }

                        override fun onEdit(group: AccountGroup, position: Int, id: Long) {
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

                        override fun onCounterIncrement(view: TextView, account: Account, position: Int, id: Long) {
                            account.counter++
                            view.text = OTPProvider.generate(account)
                        }
                    })
                }
            }

            adapter.clearAdapters()
            adapter.addAllAdapters(adapters)
        }
        touchHelper.attachToRecyclerView(list_accounts)
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

        lifecycleScope.launch {
            viewModel.accountDao.update(adapter.accounts)
            viewModel.accountGroupDao.update(adapter.groups)
        }
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
                adapter.notifyAllDataSetChanged()
            }
            R.id.menu_add_test -> {
                val groups = listOf(
                    AccountGroup("Work").apply { id = 2; order = 0 },
                    AccountGroup("Personal").apply { id = 3; order = 1 },
                    AccountGroup("Others").apply { id = 4; order = 2 }
                )
                val accounts = listOf(
                    Account("Max", "22334455").apply { label = "Twitter"; groupId = 3 },
                    Account("Max", "22334466").apply { label = "Steam"; groupId = 3 },
                    Account("Max", "22332277").apply { label = "Amazon"; groupId = 3 },
                    Account("Max", "22334455").apply { label = "EGS"; groupId = 3 },
                    Account("Max", "22444455").apply { label = "Github"; groupId = 2 },
                    Account("JohnDoe", "22774477").apply { groupId = 4 },
                    Account("MarioRossi", "77334455").apply { groupId = 4 },
                    Account("JaneDoe", "22223355")
                )
                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()
                    viewModel.accountGroupDao.deleteAll()

                    groups.forEach { viewModel.accountGroupDao.insert(it) }
                    accounts.forEach { viewModel.accountDao.insert(it) }

                    viewModel.accountGroupDao.insert(AccountGroup.DEFAULT_GROUP)
                }
            }
            R.id.menu_account_list_edit -> {
                startSupportActionMode(accountActionMode)
            }
            R.id.menu_account_list_edit_group -> {
                startSupportActionMode(groupActionMode)

                adapter.setEditMode(AccountListAdapter.EditMode.Group)
                adapter.notifyAllDataSetChanged()
            }
            R.id.menu_account_list_toggle_pins -> {
                val showPins = preferences.getBoolean(Preferences.SHOW_PINS, true)

                adapter.showPins = !showPins
                preferences.edit {
                    putBoolean(Preferences.SHOW_PINS, !showPins)
                }
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
                        if (account.id == BaseAccount.DEFAULT_ID) {
                            val id = viewModel.accountDao.getAccount(account.name, account.issuer).id

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