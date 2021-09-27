package dev.notrobots.authenticator.ui.accountlist

import android.content.Context
import android.view.View
import android.view.View.inflate
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.data.KnownIssuers
import dev.notrobots.authenticator.extensions.find
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPProvider
import kotlinx.android.synthetic.main.item_account.view.*

class AccountListAdapter(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    private val data: LiveData<List<Account>>
) : ArrayAdapter<Account>(context, 0) {

    init {
        data.observe(lifecycleOwner) {
            notifyDataSetChanged()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflate(context, R.layout.item_account, null)

        data.value?.get(position)?.let { account ->
            val icon = KnownIssuers.find { k, _ -> k.matches(account.issuer) }
            val text = if (account.label.isNotEmpty()) {
                "${account.label} (${account.name})"
            } else {
                account.name
            }

            view.text_account_label.text = text
            view.text_account_pin.text = OTPProvider.generate(account)
        }

        return view
    }

    override fun getCount(): Int {
        return data.value?.size ?: 0
    }
}