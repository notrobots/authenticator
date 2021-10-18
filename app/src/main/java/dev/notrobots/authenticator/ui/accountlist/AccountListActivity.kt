package dev.notrobots.authenticator.ui.accountlist

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.activity.viewModels
import androidx.appcompat.view.ActionMode
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
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
import dev.notrobots.authenticator.dialogs.*
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.google.CountdownIndicator
import dev.notrobots.authenticator.google.TotpClock
import dev.notrobots.authenticator.google.TotpCountdownTask
import dev.notrobots.authenticator.google.TotpCounter
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import dev.notrobots.authenticator.models.OTPProvider
import dev.notrobots.authenticator.ui.account.AccountActivity
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.notrobots.authenticator.ui.export.ExportActivity
import dev.notrobots.authenticator.ui.export.ExportConfigActivity
import kotlinx.android.synthetic.main.activity_account_list.*
import kotlinx.android.synthetic.main.item_account.view.*
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
                    addAccount(url)
                } catch (e: Exception) {
                    //FIXME: This dialog sucks ass
                    val dialog = ErrorDialog()

                    dialog.setErrorMessage(e.message)
                    dialog.show(supportFragmentManager, null)
                }
            }
        }
    }
//    private val editAccount = registerForActivityResult(StartActivityForResult()) {
//        val account = it.data?.getSerializableExtra(AccountActivity.EXTRA_ACCOUNT) as? Account
//
//        if (it.resultCode == AccountActivity.RESULT_INSERT && account != null) {
//            addAccount(account)
//        } else if (it.resultCode == AccountActivity.RESULT_UPDATE && account != null) {
//            updateAccount(account)  //FIXME: Handle the case in which an account is updated and the new label already exists
//            logd("Updating account with displayName: ${account.displayName}")
//        } else if (it.resultCode == Activity.RESULT_CANCELED) {
//            logd("Action cancelled")
//        }
//    }
//    private val addAccount = registerForActivityResult(StartActivityForResult()) {
//
//    }

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

    private val accountActionMode = object  : ActionMode.Callback {
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
                R.id.menu_group_remove -> {
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

    //region Activity lifecycle

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
                addAccount(it)
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

                try {
                    addGroup(group)
                    dialog.dismiss()
                }catch (e: Exception) {
                    dialog.error = e.message
                }
            }
            dialog.show(supportFragmentManager, null)
            btn_add_account.close(true)
        }

        viewModel.groupsWithAccount.observe(this) {
            //FIXME: This needs to be optimized
            val adapters = it.map {
                AccountListAdapter(it).apply {
                    touchHelper = this@AccountListActivity.touchHelper
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
                                    checkIfGroupExists(name) {
                                        if (it) {
                                            dialog.error = "A group with the same name already exists"
                                        } else {
                                            group.name = name
                                            viewModel.accountGroupDao.update(group)
                                            dialog.dismiss()
                                        }
                                    }
                                }
                            }
                            dialog.show(supportFragmentManager, null)
                        }
                    })
                }
            }

            adapter.clearAdapters()
            adapter.addAllAdapters(adapters)
        }
        touchHelper.attachToRecyclerView(list_accounts)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> {
                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()
                    viewModel.accountGroupDao.deleteAll()
                }
            }
            R.id.menu_refresh -> {
                adapter.notifyAllDataSetChanged()
            }
            R.id.menu_add_test -> {
                val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
                val rand = Random()
                val secret = {
                    buildString {
                        repeat(8) {
                            append(alphabet[rand.nextInt(alphabet.length)])
                        }
                    }
                }
                var count = 0
                val tests = Array(30) {
                    Account("Test: ${count++}", secret())
                }

                lifecycleScope.launch {
                    viewModel.accountDao.deleteAll()
                    viewModel.accountGroupDao.deleteAll()

                    var last = viewModel.accountDao.getLastOrder()
                    val defaultGroupId = viewModel.accountGroupDao.insert(AccountGroup.DEFAULT_GROUP)

                    for (test in tests) {
                        test.order = ++last
                        test.groupId = defaultGroupId
                        viewModel.accountDao.insert(test)
                    }
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

    //region Accounts & Groups

    /**
     * Parses the given [input] into an [Uri] and tries to insert it into the database.
     *
     * This method may throw an exception if the string is malformed, the exception message
     * can be shown to the user.
     */
    private fun addAccount(input: String) {
        addAccount(input.toUri())
    }

    /**
     * Retrieves the [Account] information from the [Uri] and tries to insert it into the database.
     *
     * This method may throw an exception if the Uri is malformed, the exception message
     * can be shown to the user.
     */
    private fun addAccount(uri: Uri) {
        addAccount(Account.parse(uri))
    }

    /**
     * Inserts the given [account] into the database.
     *
     * In case the account (name & issuer) already exists, the user is prompt with a
     * dialog asking them if they want to overwrite the existing account.
     *
     * This method **will not** throw any exceptions.
     */
    private fun addAccount(account: Account) {
        checkIfAccountExists(account) {
            // If the count is greater than 0, that means there's one other account
            // with the same name and issuer, ask the user if they want to overwrite it
            if (it) {
                val dialog = ReplaceAccountDialog() //TODO Let the user keep both accounts

                dialog.onReplaceListener = {
                    updateAccount(account)
                    logd("Replacing existing account: $account")
                }
                dialog.show(supportFragmentManager, null)
            }
            // No account with the same name and issuer was found in the database,
            // insert the given account
            else {
                val last = viewModel.accountDao.getLastOrder()
                val defaultGroupCreated = viewModel.accountGroupDao.isNotEmpty() > 0

                if (!defaultGroupCreated) {
                    viewModel.accountGroupDao.insert(AccountGroup.DEFAULT_GROUP)
                }

                account.order = last + 1
                viewModel.accountDao.insert(account)
                logd("Adding new account: ${account.name}")
            }
        }
    }

    /**
     * Updates the given account in the database.
     *
     * If the account ID is null it looks up for a record with the same name and issuer and
     * updates that record
     */
    private fun updateAccount(account: Account) {
        lifecycleScope.launch {
            if (account.id <= 0) {  //FIXME: What the hell is this
                val original = viewModel.accountDao.getAccount(account.name, account.issuer)

                account.id = original.id
            }

            viewModel.accountDao.update(account)
        }
    }

    /**
     * Inserts the given [group] into the database
     *
     * If the group already exists it will be replaced
     */
    private fun addGroup(group: AccountGroup) {
        checkIfGroupExists(group) {
            if (it) {
                error("A group with the same name already exists")
            } else {
                val last = viewModel.accountGroupDao.getLastOrder()

                group.order = last + 1
                viewModel.accountGroupDao.insert(group)
                logd("Adding new group: ${group.name}")
            }
        }
    }

    /**
     * Checks if the given [account] already exists and then invokes the given [block] with the result
     *
     * The [block] is invoked inside of a coroutine
     */
    private fun checkIfAccountExists(account: Account, block: suspend (Boolean) -> Unit) {
        lifecycleScope.launch {
            val count = viewModel.accountDao.getCount(account.name, account.issuer)

            block(count > 0)
        }
    }

    /**
     * Checks if the given [group] already exists and then invokes the given [block] with the result
     *
     * The [block] is invoked inside of a coroutine
     */
    private fun checkIfGroupExists(group: AccountGroup, block: suspend (Boolean) -> Unit) {
        checkIfGroupExists(group.name, block)
    }

    /**
     * Checks if a group with the given [name] already exists and then invokes the given [block] with the result
     *
     * The [block] is invoked inside of a coroutine
     */
    private fun checkIfGroupExists(name: String, block: suspend (Boolean) -> Unit) {
        lifecycleScope.launch {
            val count = viewModel.accountGroupDao.getCount(name)

            block(count > 0)
        }
    }

    //endregion
}