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
import dev.notrobots.androidstuff.extensions.setDisabled
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
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountActivity : ThemedActivity() {
    private val viewModel by viewModels<AccountListViewModel>()
    private val binding by viewBindings<ActivityAccountBinding>(this)
    private lateinit var account: Account
    private var sourceAccount: Account? = null
    private var showAdvanced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbarLayout.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarLayout.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (intent.hasExtra(EXTRA_ACCOUNT)) {
            account = intent.getSerializableExtra(EXTRA_ACCOUNT) as Account
            sourceAccount = account.clone()
            title = account.name
        } else {
            account = Account()
            sourceAccount = null
            setTitle(R.string.label_add_account)
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
        binding.spinnerAccountType.setOnSelectionChangeListener { value, _ ->
            when (value) {
                OTPType.HOTP -> binding.periodCounterSwitcher.showView(R.id.layout_account_counter_value)
                OTPType.TOTP -> binding.periodCounterSwitcher.showView(R.id.layout_account_period)
            }
        }
        binding.spinnerAccountType.setValues<OTPType>()
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
        binding.textAccountDigits.setText(Account.DEFAULT_DIGITS.toString())
        binding.textAccountPeriod.setText(Account.DEFAULT_PERIOD.toString())
        binding.advancedSettingsToggle.setOnClickListener {
            showAdvanced = !showAdvanced
            binding.advancedSettingsToggle.setText(if (showAdvanced) R.string.label_hide_advanced else R.string.label_show_advanced)
            binding.advancedSettings.setDisabled(!showAdvanced)
        }

        sourceAccount?.also {
            binding.textAccountName.setText(it.name)
            binding.textAccountSecret.setText(it.secret)
            binding.textAccountLabel.setText(it.label)
            binding.textAccountIssuer.setText(it.issuer)
            binding.textAccountDigits.setText(it.digits.toString())
            binding.textAccountPeriod.setText(it.period.toString())
            binding.spinnerAccountAlgorithm.setSelection(it.algorithm)
            binding.textAccountCounterValue.setText(it.counter.toString())
            binding.spinnerAccountType.setSelection(it.type)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_account, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_account_save) {
            addOrUpdateAccount()
            return true
        }

        return false
    }

    private fun addOrUpdateAccount() {
        val name = binding.textAccountName.text.toString()
        val issuer = binding.textAccountIssuer.text.toString()
        val label = binding.textAccountLabel.text.toString()
        val secret = binding.textAccountSecret.text.toString()
        val hasError = {
            binding.layoutAccountName.hasErrors ||
            binding.layoutAccountLabel.hasErrors ||
            binding.layoutAccountIssuer.hasErrors ||
            binding.layoutAccountSecret.hasErrors ||
            binding.layoutAccountDigits.hasErrors ||
            binding.layoutAccountPeriod.hasErrors
        }

        if (!hasError()) {
            account.also {
                it.name = name
                it.issuer = issuer
                it.label = label
                it.secret = secret
                it.type = binding.spinnerAccountType.selectedValue as OTPType
                it.digits = binding.textAccountDigits.text.toString().toIntOrNull() ?: 0
                it.period = binding.textAccountPeriod.text.toString().toLongOrNull() ?: 0
                it.algorithm = binding.spinnerAccountAlgorithm.selectedValue as HmacAlgorithm
            }

            lifecycleScope.launch {
                val sameNames = account.name == sourceAccount?.name &&
                                account.label == sourceAccount?.label &&
                                account.issuer == sourceAccount?.issuer

                if (sourceAccount != null) {
                    if (sameNames || !viewModel.accountDao.exists(account.name, account.label, account.issuer)) {
                        viewModel.updateAccount(account)
                        finish()
                    } else {
                        //TODO: More precise error
                        binding.layoutAccountName.error = "An account with the same name, label or issuer already exists"
                    }
                } else {
                    if (!viewModel.accountDao.exists(account.name, account.label, account.issuer)) {
                        viewModel.insertAccount(account)
                        finish()
                    } else {
                        //TODO: More precise error
                        binding.layoutAccountName.error = "An account with the same name, label or issuer already exists"
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_ACCOUNT = "AccountActivity.ACCOUNT"
    }
}