package dev.notrobots.authenticator.models

import android.net.Uri
import androidx.room.Entity
import androidx.room.Index
import java.io.Serializable

@Entity(indices = [Index(value = ["name"], unique = true)])
class AccountGroup(
    name: String
) : BaseAccount(name), Serializable {
    /**
     * Expanded state of this group
     */
    var isExpanded: Boolean = true

    /**
     * Whether or not this is a default group
     *
     * A default group is not shown like a regular group and there must be
     * only one default group which is also automatically created
     */
    var isDefault: Boolean = false

    override fun getUri(): Uri {
        return Uri.Builder()
            .scheme(AccountExporter.OTP_SCHEME)
            .authority(AccountExporter.OTP_GROUP_AUTHORITY)
            .path(name)
            .appendQueryParameter(AccountExporter.OTP_ORDER, order.toString())
            .build()
    }

    companion object {
        val DEFAULT_GROUP = AccountGroup("").apply {
            id = Account.DEFAULT_GROUP_ID
            order = DEFAULT_ORDER
            isDefault = true
        }
    }
}