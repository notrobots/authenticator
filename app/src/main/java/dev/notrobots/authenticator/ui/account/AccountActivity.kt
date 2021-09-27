package dev.notrobots.authenticator.ui.account

import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.activities.ThemedActivity
import dev.notrobots.authenticator.extensions.isOnlySpaces
import dev.notrobots.authenticator.extensions.setClearErrorOnType
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

        // An account ID was passed from the previous activity
        // Load that account and let the user edit its data
        if (intent.hasExtra(EXTRA_ACCOUNT)) {
            account = intent.getSerializableExtra(EXTRA_ACCOUNT) as Account
            title = account!!.displayName
            text_account_name.setText(account!!.name)
            text_account_secret.setText(account!!.secret)
            text_account_label.setText(account!!.label)
            text_account_issuer.setText(account!!.issuer)
            spinner_account_type.setSelection(account!!.type.toString())
        } else {
            title = "Add account"
        }

        layout_account_name.setClearErrorOnType()
        layout_account_secret.setClearErrorOnType()
        layout_account_label.setClearErrorOnType()
        layout_account_issuer.setClearErrorOnType()
        btn_account_confirm.setOnClickListener {
            val intent = Intent()
            val result: Int

            if (account != null) {
                //TODO: Edit account without adding a new one to the DB
                // The DAO needs a primary integer key which is used to retrieve the values
                // The insert should still check for the same name in the DB and disallow or replace the existing one

                result = RESULT_UPDATE
            } else {
                account = Account()
                result = RESULT_INSERT
            }

            val name = text_account_name.text.toString()
            val issuer = text_account_issuer.text.toString()
            val label = text_account_label.text.toString()
            val secret = text_account_secret.text.toString()
            val type = parseEnum<OTPType>(spinner_account_type.selectedValue.toString())!!
            var hasError = false

            if (name.isBlank()) {
                layout_account_name.error = "Field can't be empty"
                hasError = true
            }

            if (issuer.isOnlySpaces()) {
                layout_account_issuer.error = "Field can't be blank"
                hasError = true
            }

            if (label.isOnlySpaces()) {
                layout_account_secret.error = "String can be blank"
                hasError = true
            }

            if (secret.isBlank()) {
                layout_account_secret.error = "Field can't be empty"
                hasError = true
            }

            if (!hasError) {
                account!!.name = name
                account!!.issuer = issuer
                account!!.label = label
                account!!.secret = secret
                account!!.type = type

                intent.putExtra(EXTRA_ACCOUNT, account)

                setResult(result, intent)
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