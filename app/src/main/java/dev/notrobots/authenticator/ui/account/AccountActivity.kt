package dev.notrobots.authenticator.ui.account

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.hasErrors
import dev.notrobots.androidstuff.extensions.setError
import dev.notrobots.androidstuff.extensions.setErrorWhen
import dev.notrobots.androidstuff.util.viewBindings
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.databinding.ActivityAccountBinding
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import dev.notrobots.authenticator.util.isValidBase32
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountActivity : ThemedActivity() {
    private val viewModel by viewModels<AccountListViewModel>()
    private val binding by viewBindings<ActivityAccountBinding>(this)
    private lateinit var account: Account
    private var sourceAccount: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (intent.hasExtra(EXTRA_ACCOUNT)) {
            account = intent.getSerializableExtra(EXTRA_ACCOUNT) as Account
            sourceAccount = account.copy()
            title = account.displayName
        } else {
            account = Account()
            sourceAccount = null
            title = "Add account"
        }

        binding.layoutAccountName.setErrorWhen("Name cannot be empty") { s ->
            s.isEmpty()
        }
        binding.layoutAccountSecret.setError(
            "Secret cannot be empty" to { it.isEmpty() },
            "Secret key must be a base32 string" to { !isValidBase32(it) }
        )
        binding.layoutAccountLabel.setErrorWhen("Label cannot be blank") { s ->
            s.isOnlySpaces()
        }
        binding.layoutAccountIssuer.setErrorWhen("Issuer cannot be blank") { s ->
            s.isOnlySpaces()
        }
        binding.layoutAccountDigits.setErrorWhen("Value must be higher than 0") { s ->
            val n = s.toIntOrNull()

            n == null || n <= 0
        }
        binding.layoutAccountPeriod.setError(
            "Field cannot be empty" to { it.isEmpty() },
            "Value must be higher than 0" to { (it.toIntOrNull() ?: 0) <= 0 }
        )
        binding.spinnerAccountAlgorithm.setValues<HmacAlgorithm>()
        binding.spinnerAccountType.setValues<OTPType>()
        binding.spinnerAccountType.onSelectionChanged = { value, _ ->
            when (value) {
                OTPType.HOTP -> binding.periodCounterSwitcher.showView(R.id.layout_account_counter_value)
                OTPType.TOTP -> binding.periodCounterSwitcher.showView(R.id.layout_account_period)
            }
        }
        binding.btnAccountConfirm.setOnClickListener { addOrUpdateAccount() }
        binding.btnAccountConfirm.text = if (sourceAccount == null) "Add" else "Update"
        binding.layoutAccountCounterValue.setStartIconOnClickListener {
            if (account.counter > 0) {
                account.counter--
                binding.textAccountCounterValue.setText(account.counter.toString())
                binding.textAccountCounterValue.setSelection(binding.textAccountCounterValue.length())
            }
        }
        binding.layoutAccountCounterValue.setEndIconOnClickListener {
            account.counter++   //FIXME: What's the maximum here??
            binding.textAccountCounterValue.setText(account.counter.toString())
            binding.textAccountCounterValue.setSelection(binding.textAccountCounterValue.length())
        }
        binding.textAccountCounterValue.addTextChangedListener {
            if (it.isNullOrEmpty()) {
                binding.textAccountCounterValue.setText("0")
                binding.textAccountCounterValue.setSelection(binding.textAccountCounterValue.length())
            }

            account.counter = it.toString().toLongOrNull() ?: 0
        }

        text_account_digits.setText(Account.DEFAULT_DIGITS.toString())
        text_account_period.setText(Account.DEFAULT_PERIOD.toString())

        sourceAccount?.also {
            text_account_name.setText(it.name)
            text_account_secret.setText(it.secret)
            text_account_label.setText(it.label)
            text_account_issuer.setText(it.issuer)
            text_account_digits.setText(it.digits.toString())
            text_account_period.setText(it.period.toString())
            spinner_account_algorithm.setSelection(it.algorithm)
            text_account_counter_value.setText(it.counter.toString())
            binding.spinnerAccountType.setSelection(it.type)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_account, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
            R.id.menu_account_save -> {
                addOrUpdateAccount()
            }

            else -> return false
        }

        return true
    }

    private fun addOrUpdateAccount() {
        val name = text_account_name.text.toString()
        val issuer = text_account_issuer.text.toString()
        val label = text_account_label.text.toString()
        val secret = text_account_secret.text.toString()
        val hasError = {
            layout_account_name.hasErrors ||
            layout_account_label.hasErrors ||
            layout_account_issuer.hasErrors ||
            layout_account_secret.hasErrors ||
            binding.layoutAccountDigits.hasErrors ||
            binding.layoutAccountPeriod.hasErrors
        }

        if (!hasError()) {
            account.also {
                it.name = name
                it.issuer = issuer
                it.label = label
                it.secret = secret
                it.type = spinner_account_type.selectedValue as OTPType
                it.digits = text_account_digits.text.toString().toIntOrNull() ?: 0
                it.period = text_account_period.text.toString().toLongOrNull() ?: 0
                it.algorithm = spinner_account_algorithm.selectedValue as HmacAlgorithm
            }

            lifecycleScope.launch {
                if (viewModel.accountDao.exists(account.name, account.label, account.issuer)) {
                    //TODO: More precise error
                    layout_account_name.error = "An account with the same name, label or issuer already exists"
                } else {
                    if (sourceAccount != null) {
                        viewModel.updateAccount(account)
                    } else {
                        viewModel.insertAccount(account)
                    }

                    finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_ACCOUNT = "AccountActivity.ACCOUNT"
    }
}