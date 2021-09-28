package dev.notrobots.authenticator.ui.account

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.ThemedActivity
import dev.notrobots.authenticator.extensions.*
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPType
import dev.notrobots.authenticator.util.parseEnum
import kotlinx.android.synthetic.main.activity_account.*

@AndroidEntryPoint
class AccountActivity : ThemedActivity() {
    private var account: Account? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        val resultCode: Int

        // An account ID was passed from the previous activity
        // Load that account and let the user edit its data
        if (intent.hasExtra(EXTRA_ACCOUNT)) {
            account = intent.getSerializableExtra(EXTRA_ACCOUNT) as Account
            resultCode = RESULT_UPDATE

            title = account!!.displayName
            text_account_name.setText(account!!.name)
            text_account_secret.setText(account!!.secret)
            text_account_label.setText(account!!.label)
            text_account_issuer.setText(account!!.issuer)
            spinner_account_type.setSelection(account!!.type.toString())
        } else {
            title = "Add account"
            account = Account()
            resultCode = RESULT_INSERT
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

        btn_account_confirm.setOnClickListener {
            val name = text_account_name.text.toString()
            val issuer = text_account_issuer.text.toString()
            val label = text_account_label.text.toString()
            val secret = text_account_secret.text.toString()
            val type = parseEnum<OTPType>(spinner_account_type.selectedValue.toString())!!
            var hasError = false

            if (name.isBlank()) {
                layout_account_name.error = "Name cannot be empty"
                hasError = true
            }

            if (label.isOnlySpaces()) {
                layout_account_label.error = "Label cannot be blank"
                hasError = true
            }

            if (issuer.isOnlySpaces()) {
                layout_account_issuer.error = "Issuer cannot be blank"
                hasError = true
            }

            try {
                Account.validateSecret(secret)
            } catch (e: Exception) {
                layout_account_secret.error = e.message
                hasError = true
            }

            when (type) {
                //TODO Check the conditions for each of the different extra options
            }

            if (!hasError) {
                val result = Intent()

//                if (account != null) {
//                    //TODO: Edit account without adding a new one to the DB
//                    // The DAO needs a primary integer key which is used to retrieve the values
//                    // The insert should still check for the same name in the DB and disallow or replace the existing one
//
//                    result = RESULT_UPDATE
//                } else {
//                    result = RESULT_INSERT
//                }

                account?.also {
                    it.name = name
                    it.issuer = issuer
                    it.label = label
                    it.secret = secret
                    it.type = type
                }

                result.putExtra(EXTRA_ACCOUNT, account)
                setResult(resultCode, result)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_ACCOUNT = "AccountActivity.ACCOUNT"
        const val RESULT_INSERT = 100
        const val RESULT_UPDATE = 200
    }
}