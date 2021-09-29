package dev.notrobots.authenticator.ui.accountlist

import android.app.Activity
import android.content.Intent
import dev.notrobots.authenticator.google.TotpClock
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.dialogs.AccountURLDialog
import dev.notrobots.authenticator.dialogs.ErrorDialog
import dev.notrobots.authenticator.dialogs.ReplaceAccountDialog
import dev.notrobots.authenticator.google.TotpCountdownTask
import dev.notrobots.authenticator.google.TotpCounter
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.util.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.models.OTPProvider
import dev.notrobots.authenticator.ui.account.AccountActivity
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import dev.turingcomplete.kotlinonetimepassword.RandomSecretGenerator
import kotlinx.android.synthetic.main.activity_account_list.*
import kotlinx.android.synthetic.main.item_account.view.*

@AndroidEntryPoint
class AccountListActivity : AppCompatActivity(), ActionMode.Callback {
    private val viewModel by viewModels<AccountListViewModel>()
    private val adapter by lazy {
        AccountListAdapter()
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
        if (it.data != null) {
            val account = it.data!!.getSerializableExtra(AccountActivity.EXTRA_ACCOUNT) as Account

            if (it.resultCode == AccountActivity.RESULT_INSERT) {
                addAccount(account)
            } else if (it.resultCode == AccountActivity.RESULT_UPDATE) {
                updateAccount(account)
                logd("Updating account with displayName: ${account.displayName}")
            }
        }
    }
    private var actionMode: ActionMode? = null

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

        adapter.onItemClickListener = { account, position, _ ->
            if (actionMode != null) {
                account.toggleSelected()

                if (!account.isSelected && adapter.selectedAccounts.isEmpty()) {
                    actionMode?.finish()
                }

                adapter.notifyItemChanged(position)
            } else {
                copyToClipboard(OTPProvider.generate(account))
                makeToast("Copied!")
            }
        }
        adapter.onItemLongClickListener = { _, _, _ ->
            startSupportActionMode(this)

            true
        }

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
            adapter.setData(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        actionMode?.finish()
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        actionMode?.finish()
    }

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
                val secret = {
                    RandomSecretGenerator()
                        .createRandomSecret(HmacAlgorithm.SHA1)
                        .toString()
                }
                var count = 0
                val tests = Array(30) {
                    Account("Test: ${count++}", secret())
                }

                lifecycleScope.launch {
                    viewModel.accountDao.insert(*tests)
                }
            }
        }

        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        actionMode = mode
        menuInflater.inflate(R.menu.menu_account_list_context, menu)

        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_account_remove -> {
                lifecycleScope.launch {
                    //TODO Show dialog

                    viewModel.accountDao.delete(adapter.selectedAccounts)
                }
                actionMode?.finish()
            }
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
        adapter.clearSelected()
    }

    private fun setTotpCountdownPhaseFromTimeTillNextValue(millisRemaining: Long) {
        setTotpCountdownPhase(millisRemaining.toDouble() / TimeUnit.SECONDS.toMillis(totpCounter.timeStep))
    }

    private fun setTotpCountdownPhase(phase: Double) {
        updateCountdownIndicators()

        for (child in list_accounts.children) {
            child.pb_phase.setPhase(phase)
        }
    }

    private fun updateCountdownIndicators() {
//        var i = 0
//        val len: Int = userList.getChildCount()
//        while (i < len) {
//            val listEntry: View = userList.getChildAt(i)
//            val indicator: CountdownIndicator = listEntry.findViewById(R.id.countdown_icon)
//            if (indicator != null) {
//                indicator.setPhase(totpCountdownPhase)
//            }
//            i++
//        }


//        Log.d(App.TAG, "Phase: ${phase}")
    }

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

            val i = viewModel.accountDao.update(account)

            logd("Something: $i")
        }
    }
}