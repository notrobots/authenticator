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
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.extensions.copyToClipboard
import dev.notrobots.androidstuff.extensions.makeToast
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
class AccountListActivity : BaseActivity(), ActionMode.Callback {
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
    private val editAccount = registerForActivityResult(StartActivityForResult()) {
        val account = it.data?.getSerializableExtra(AccountActivity.EXTRA_ACCOUNT) as? Account

        if (it.resultCode == AccountActivity.RESULT_INSERT && account != null) {
            addAccount(account)
        } else if (it.resultCode == AccountActivity.RESULT_UPDATE && account != null) {
            updateAccount(account)  //FIXME: Handle the case in which an account is updated and the new label already exists
            logd("Updating account with displayName: ${account.displayName}")
        } else if (it.resultCode == Activity.RESULT_CANCELED) {
            logd("Action cancelled")
        }
    }

    private val touchHelperCallback = object : ItemTouchHelper.Callback() {
        override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
            // The header is allowed to move only if the the group edit mode is enabled
            if (viewHolder.bindingAdapterPosition == 0 && adapter.editMode != AccountListAdapter.EditMode.Group) {
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
            val from = viewHolder.bindingAdapterPosition
            val to = target.bindingAdapterPosition

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
            val intent = Intent(this, AccountActivity::class.java)

            editAccount.launch(intent)
            btn_add_account.close(true)
        }

        viewModel.accounts.observe(this) {
            val accounts = it.chunked(5).mapIndexed { i, list ->
                AccountGroup("Group: ${i + 1}").apply {
                    id = i.toLong()
                    accounts = list.toMutableList()
                }
            }
            val adapters = accounts.map {
                AccountListAdapter().apply {
                    touchHelper = this@AccountListActivity.touchHelper
                    onItemClickListener = { account, position, id ->
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
                    onItemLongClickListener = { account, position, _ ->
                        if (actionMode == null) {
                            account.isSelected = true
                            notifyItemChanged(position)
                            startSupportActionMode(this@AccountListActivity)
                        }

                        true
                    }
                    onItemEditListener = {
                        val intent = Intent(this@AccountListActivity, AccountActivity::class.java)

                        intent.putExtra(AccountActivity.EXTRA_ACCOUNT, it)
                        editAccount.launch(intent)
                        actionMode?.finish()
                    }
                    setData(it)
                }
            }

            adapter.addAllAdapters(adapters)
        }
        touchHelper.attachToRecyclerView(list_accounts)
    }

    override fun onPause() {
        super.onPause()

        lifecycleScope.launch {
            viewModel.accountDao.update(adapter.accounts)
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
                }
            }
            R.id.menu_refresh -> {
                adapter.notifyDataSetChanged()
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
                    var last = viewModel.accountDao.getLastOrder()

                    for (test in tests) {
                        test.order = ++last
                        viewModel.accountDao.insert(test)
                    }
                }
            }
            R.id.menu_account_list_edit -> {
                if (actionMode == null) {
                    startSupportActionMode(this)
                }
            }
            R.id.menu_account_list_edit_group -> {
                startSupportActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        return true
                    }

                    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                        return false
                    }

                    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                        return true
                    }

                    override fun onDestroyActionMode(mode: ActionMode?) {
                        adapter.setEditMode(AccountListAdapter.EditMode.Disabled)
                        adapter.notifyAllDataSetChanged()
                    }
                })

                adapter.setEditMode(AccountListAdapter.EditMode.Group)
                adapter.notifyAllDataSetChanged()
            }
        }

        return true
    }

    //endregion

    //region Action mode

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_account_list_context, menu)
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
                    val intent = Intent(this, ExportActivity::class.java)

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
        adapter.clearSelected()
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

    //region Account

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
     * Inserts the given [account] to the database.
     *
     * In case the account (name & issuer) already exists, the user is prompt with a
     * dialog asking them if they want to overwrite the existing account.
     *
     * This method **will not** throw any exceptions.
     */
    private fun addAccount(account: Account) {
        lifecycleScope.launch {
            val count = viewModel.accountDao.getCount(account.name, account.issuer)

            // If the count is greater than 0, that means there's one other account
            // with the same name and issuer, ask the user if they want to overwrite it
            if (count > 0) {
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

                account.order = last + 1
                viewModel.accountDao.insert(account)
                logd("Adding new account: $account")
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
            if (account.id == null) {
                val original = viewModel.accountDao.getAccount(account.name, account.issuer)

                account.id = original.id
            }

            viewModel.accountDao.update(account)
        }
    }

    //endregion
}