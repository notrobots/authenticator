package dev.notrobots.authenticator.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.OTPAlgorithm
import dev.notrobots.authenticator.models.OTPType
import dev.notrobots.authenticator.time.TimerTask
import dev.notrobots.authenticator.ui.home.AccountListAdapter
import dev.notrobots.authenticator.util.parseEnum
import dev.notrobots.authenticator.ui.home.AccountListViewModel
import kotlinx.android.synthetic.main.activity_main.*
import dev.notrobots.authenticator.util.*
import kotlinx.coroutines.launch
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), TimerTask.Listener {
    private val viewModel by viewModels<AccountListViewModel>()
    private val adapter by lazy {
        AccountListAdapter(this, this, viewModel.accounts)
    }
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerTask = TimerTask()

    override fun onStart() {
        super.onStart()

        timerTask.start()
        timerTask.setListener(this)

        //TODO: Start countdown
    }

    override fun onTick(timeLeft: Long) {
        Log.d(App.TAG, "Time left: $timeLeft")
    }

    override fun onFinish() {
        adapter.notifyDataSetChanged()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel.accounts.observe(this) {
            Log.d(App.TAG, "Updated!")
        }

        list_accounts.adapter = adapter
        /**
         * Test values
         *
         * Test: otpauth://totp/www.twitter.com:@johndoe?secret=VYCF4MTEPW5SPBMNFPQUMBXOK7LAKNUD&issuer=www.twitter.com&algorithm=SHA1&digits=6&period=30
         * Amazon: otpauth://totp/Amazon%3Amaxpilotto99%40gmail.com?secret=BT7K3XVLORHO5NWRPEBO5RMNDX6UFE4JYQRUG4YIGKSF46JYD3DQ&issuer=Amazon
         */
        test_text_otpauth.setText("otpauth://totp/Amazon%3Amaxpilotto99%40gmail.com?secret=BT7K3XVLORHO5NWRPEBO5RMNDX6UFE4JYQRUG4YIGKSF46JYD3DQ&issuer=Amazon")
        test_btn_otpauth.setOnClickListener {
            addAccount(test_text_otpauth.text.toString())
        }
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
        }

        return true
    }

    private fun addAccount(input: String) {
        val uri = Uri.parse(input)

        if (uri.scheme == OTP_SCHEME) {
            // Required fields
            val path = uri.path?.let {
                if (it.startsWith("/")) {
                    it.removePrefix("/")
                } else {
                    null
                }
            } ?: TODO("Path malformed")
            val pathMatch = Regex("(?:(.+):)?(.+)").find(path)
            val type = parseEnum<OTPType>(uri.authority, true) ?: TODO("Invalid type")
            val secret = uri.getQueryParameter(OTP_SECRET) ?: TODO("Missing secret")
            val name = pathMatch?.groupValues?.get(2)
            val label = pathMatch?.groupValues?.get(1)

            // Optional fields
            val issuer = uri.getQueryParameter(OTP_ISSUER)
            val algorithm = parseEnum(uri.getQueryParameter(OTP_ALGORITHM), true) ?: DEFAULT_OTP_ALGORITHM
            val digits = uri.getQueryParameter(OTP_DIGITS)?.toIntOrNull() ?: DEFAULT_OTP_DIGITS
            val counter = uri.getQueryParameter(OTP_COUNTER)?.toIntOrNull() ?: DEFAULT_OTP_COUNTER
            val period = uri.getQueryParameter(OTP_PERIOD)?.toIntOrNull() ?: DEFAULT_OTP_PERIOD

            requireNotEmpty(name, secret) {
                TODO("Fields cannot be empty")
            }

            lifecycleScope.launch {
                val account = Account(name!!, issuer!!, label!!, secret, type, input)

                viewModel.accountDao.insert(account)
            }
        } else {
            TODO("Invalid schema")
        }
    }

    companion object {
        const val OTP_SCHEME = "otpauth"
        const val OTP_SECRET = "secret"
        const val OTP_ISSUER = "issuer"
        const val OTP_COUNTER = "counter"
        const val OTP_ALGORITHM = "algorithm"
        const val OTP_DIGITS = "digits"
        const val OTP_PERIOD = "period"
        const val DEFAULT_OTP_DIGITS = 6
        const val DEFAULT_OTP_PERIOD = 30
        const val DEFAULT_OTP_COUNTER = 0
        val DEFAULT_OTP_ALGORITHM = OTPAlgorithm.SHA1
    }
}