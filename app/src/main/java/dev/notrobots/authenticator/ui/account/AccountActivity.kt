package dev.notrobots.authenticator.ui.account

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.hasErrors
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.setClearErrorOnType
import dev.notrobots.androidstuff.extensions.setErrorWhen
import dev.notrobots.androidstuff.util.*
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.BaseActivity
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AccountGroupDao
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountExporter
import dev.notrobots.authenticator.models.AccountGroup
import dev.notrobots.authenticator.models.OTPType
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel
import dev.notrobots.authenticator.util.AuthenticatorException
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AccountActivity : BaseActivity() {
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
            text_account_name.setText(account.name)
            text_account_secret.setText(account.secret)
            text_account_label.setText(account.label)
            text_account_issuer.setText(account.issuer)
            text_account_counter_value.setText(account.counter.toString())
            spinner_account_type.setSelection(account.type.toString())
        } else {
            account = Account()
            sourceAccount = null
            title = "Add account"
        }

        if (account.type == OTPType.HOTP) {
            layout_account_counter.visibility = View.VISIBLE
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
        } else {
            layout_account_counter.visibility = View.GONE
        }

        layout_account_name.setClearErrorOnType()
        layout_account_secret.setClearErrorOnType()
        layout_account_label.setClearErrorOnType()
        layout_account_issuer.setClearErrorOnType()
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
            val type = parseEnum<OTPType>(spinner_account_type.selectedValue.toString())!!
            val hasError = {
                layout_account_name.hasErrors
                        && layout_account_label.hasErrors
                        && layout_account_issuer.hasErrors
                        && layout_account_secret.hasErrors
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
                AccountExporter.validateSecret(secret, true)
            } catch (e: Exception) {
                layout_account_secret.error = e.message
            }

            when (type) {
                //TODO Check the conditions for each of the different extra options
            }

            if (!hasError()) {
                account.also {
                    it.groupId = spinner_account_group.selectedValue as Long
                    it.name = name
                    it.issuer = issuer
                    it.label = label
                    it.secret = secret
                    it.type = type
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