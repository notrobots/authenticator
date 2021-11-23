package dev.notrobots.authenticator.ui.account

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.BaseActivity
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.hasErrors
import dev.notrobots.androidstuff.extensions.setClearErrorOnType
import dev.notrobots.androidstuff.util.*
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountExporter
import dev.notrobots.authenticator.models.OTPType
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import dev.turingcomplete.kotlinonetimepassword.HmacAlgorithm
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountActivity : ThemedActivity() {
    private val viewModel by viewModels<AccountListViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        val account: Account
        val sourceAccount: Account?

        if (intent.hasExtra(EXTRA_ACCOUNT)) {
            account = intent.getSerializableExtra(EXTRA_ACCOUNT) as Account
            sourceAccount = account.clone()
            title = account.displayName
        } else {
            account = Account()
            sourceAccount = null
            title = "Add account"
        }

        layout_account_name.setClearErrorOnType()
        layout_account_secret.setClearErrorOnType()
        layout_account_label.setClearErrorOnType()
        layout_account_issuer.setClearErrorOnType()
        spinner_account_algorithm.setValues<HmacAlgorithm>()
        spinner_account_type.setValues<OTPType>()
        spinner_account_type.onItemClickListener = { entry, value ->
            if (parseEnum<OTPType>(value.toString()) == OTPType.HOTP) {
                layout_account_counter.visibility = View.VISIBLE
            } else {
                layout_account_counter.visibility = View.GONE
            }
        }
        btn_account_confirm.setOnClickListener {
            val name = text_account_name.text.toString()
            val issuer = text_account_issuer.text.toString()
            val label = text_account_label.text.toString()
            val secret = text_account_secret.text.toString()
            val hasError = {
                layout_account_name.hasErrors
                        || layout_account_label.hasErrors
                        || layout_account_issuer.hasErrors
                        || layout_account_secret.hasErrors
            }

            try {
                AccountExporter.validateName(name)
            } catch (e: Exception) {
                layout_account_name.error = e.message
            }

            try {
                AccountExporter.validateLabel(label)
            } catch (e: Exception) {
                layout_account_label.error = e.message
            }

            try {
                AccountExporter.validateIssuer(issuer)
            } catch (e: Exception) {
                layout_account_issuer.error = e.message
            }

            try {
                AccountExporter.validateSecret(secret)
            } catch (e: Exception) {
                layout_account_secret.error = e.message
            }

            if (!hasError()) {
                account.also {
                    it.groupId = spinner_account_group.selectedValue as Long
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
                    try {
                        if (sourceAccount != null) {
                            val overwrite = sourceAccount.name == account.name
                                    && sourceAccount.label == account.label
                                    && sourceAccount.issuer == account.issuer

                            viewModel.updateAccount(account, overwrite)
                        } else {
                            viewModel.addAccount(account)
                        }

                        finish()
                    } catch (e: Exception) {
                        //FIXME: Could use a snackbar here
                        layout_account_name.error = e.message
                    }
                }
            }
        }
        layout_account_counter.visibility = if (account.type == OTPType.HOTP) View.VISIBLE else View.GONE
        img_account_counter_decrement.setOnClickListener {
            if (account.counter > 0) {
                account.counter--
                text_account_counter_value.setText(account.counter.toString())
            }
        }
        img_account_counter_increment.setOnClickListener {
            account.counter++   //FIXME: What's the maximum here??
            text_account_counter_value.setText(account.counter.toString())
        }
        text_account_counter_value.addTextChangedListener {
            account.counter = it.toString().toLongOrNull() ?: 0
        }

        text_account_name.setText(account.name)
        text_account_secret.setText(account.secret)
        text_account_label.setText(account.label)
        text_account_issuer.setText(account.issuer)
        text_account_digits.setText(account.digits.toString())
        text_account_period.setText(account.period.toString())
        spinner_account_algorithm.setSelection(account.algorithm)
        text_account_counter_value.setText(account.counter.toString())
        spinner_account_type.setSelection(account.type.toString())

        viewModel.groups.observe(this) {
            spinner_account_group.entries = it.map { it.name }
            spinner_account_group.values = it.map { it.id }
            spinner_account_group.setSelection(account.groupId)
        }
    }

    companion object {
        const val EXTRA_ACCOUNT = "AccountActivity.ACCOUNT"
    }
}