package dev.notrobots.authenticator.ui.account

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.makeToast
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
import dev.notrobots.authenticator.util.AuthenticatorException
import kotlinx.android.synthetic.main.activity_account.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AccountActivity : BaseActivity() {
    private val viewModel by viewModels<AccountViewModel>()

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
            spinner_account_type.setSelection(account.type.toString())
        } else {
            account = Account()
            sourceAccount = null
            title = "Add account"
        }

        layout_account_name.setClearErrorOnType()
        layout_account_secret.setClearErrorOnType()
        layout_account_label.setClearErrorOnType()
        layout_account_issuer.setClearErrorOnType()

        spinner_account_type.onItemClickListener = { entry, value ->
            //TODO: Switch extra parameters fragments

//            when(parseEnum<OTPType>(value.toString())) {
//                OTPType.TOTP -> TODO()
//                OTPType.HOTP -> TODO()
//            }
        }

        viewModel.groups.observe(this) {
            spinner_account_group.entries = it.map { it.name }
            spinner_account_group.values = it.map { it.id }
            spinner_account_group.setSelection(account.groupId)
        }

        btn_account_confirm.setOnClickListener {
            val name = text_account_name.text.toString()
            val issuer = text_account_issuer.text.toString()
            val label = text_account_label.text.toString()
            val secret = text_account_secret.text.toString()
            val type = parseEnum<OTPType>(spinner_account_type.selectedValue.toString())!!
            var hasError = false

            tryRun({ AccountExporter.validateName(name) }) {
                it?.let {
                    it as AuthenticatorException
                    layout_account_name.error = it.message
                    hasError = true
                }
            }

            tryRun({ AccountExporter.validateLabel(label) }) {
                it?.let {
                    it as AuthenticatorException
                    layout_account_label.error = it.message
                    hasError = true
                }
            }

            tryRun({ AccountExporter.validateIssuer(issuer) }) {
                it?.let {
                    it as AuthenticatorException
                    layout_account_issuer.error = it.message
                    hasError = true
                }
            }

            tryRun({ AccountExporter.validateSecret(secret) }) {
                it?.let {
                    layout_account_secret.error = it.message
                    hasError = true
                }
            }

            when (type) {
                //TODO Check the conditions for each of the different extra options
            }

            if (!hasError) {
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
                            if (sourceAccount == account) {
                                makeToast("No changes were made")
                            } else {
                                viewModel.updateAccount(account)
                            }
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
    }

    companion object {
        const val EXTRA_ACCOUNT = "AccountActivity.ACCOUNT"
    }
}