package dev.notrobots.authenticator.ui.backupimport

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.AndroidEntryPoint
import dev.notrobots.androidstuff.activities.ThemedActivity
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.setClearErrorOnType
import dev.notrobots.androidstuff.extensions.startActivity
import dev.notrobots.androidstuff.util.loge
import dev.notrobots.androidstuff.util.parseEnum
import dev.notrobots.authenticator.R
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.dialogs.ErrorDialog
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountExporter
import dev.notrobots.authenticator.models.ImportType
import dev.notrobots.authenticator.ui.accountlist.AccountListActivity
import dev.notrobots.authenticator.ui.barcode.BarcodeScannerActivity
import kotlinx.android.synthetic.main.activity_import.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ImportActivity : ThemedActivity() {
    private val accountExporter = AccountExporter()
    private val barcodeScanner by lazy {
        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC
            )
            .build()

        BarcodeScanning.getClient(scannerOptions)
    }
    private val scanBarcode = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            if (it.data != null) {
                val uri = it.data!!.getStringExtra(BarcodeScannerActivity.EXTRA_QR_DATA) ?: ""

                try {
                    val accounts = accountExporter.import(uri)

                    setImportResult(accounts)
                } catch (e: Exception) {
                    val dialog = ErrorDialog()

                    dialog.setErrorMessage(e.message)
                    dialog.show(supportFragmentManager, null)
                }
            }
        }
    }
    private val textFilePicker = registerForActivityResult(OpenDocument()) {
        layout_import_text_value.editText?.setText(it.toString())
        contentResolver.openInputStream(it)?.let {
            val content = it.reader().readText()
            val accounts = accountExporter.import(content)

            setImportResult(accounts)
        }
    }
    private val imageFilePicker = registerForActivityResult(OpenDocument()) {
        val image = InputImage.fromFilePath(this, it)

        layout_import_file.text = it.toString()
        barcodeScanner.process(image)
            .addOnSuccessListener {
                val content = it.first().rawValue

                try {
                    val accounts = accountExporter.import(content!!)

                    setImportResult(accounts)
                }catch (e: Exception) {
                    val dialog = ErrorDialog()

                    dialog.setErrorMessage("Import data is corrupt")    //FIXME: Show detailed error with account index ecc
                    dialog.show(supportFragmentManager, null)
                }
            }
            .addOnFailureListener {
                loge("Error: $it")
            }
    }
    private var accounts = listOf<Account>()

    @Inject
    lateinit var accountDao: AccountDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_import)

        title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val types = ImportType.values()
            .map { it.name }
            .toMutableList().apply {
                add(0, "")
            }

        spinner_import_type.values = types
        spinner_import_type.onItemClickListener = { entry, value ->
            val type = parseEnum<ImportType>(value as String)

            when (type) {
                ImportType.QR -> {
                    val intent = Intent(this, BarcodeScannerActivity::class.java)

                    scanBarcode.launch(intent)
                    switcher_import_input.visibility = View.GONE
                    setImportResult(emptyList())
                }
                ImportType.QRFile -> {
                    switcher_import_input.showView(R.id.layout_import_file)
                    switcher_import_input.visibility = View.VISIBLE

                    layout_import_file.onClickListener = {
                        imageFilePicker.launch(arrayOf("image/*"))
                    }
                    setImportResult(emptyList())
                }
                ImportType.TextFile -> {
                    switcher_import_input.showView(R.id.layout_import_file)
                    switcher_import_input.visibility = View.VISIBLE

                    layout_import_file.onClickListener = {
                        textFilePicker.launch(arrayOf("text/*"))
                    }
                    setImportResult(emptyList())
                }
                ImportType.Text -> {
                    switcher_import_input.showView(R.id.layout_import_text)
                    switcher_import_input.visibility = View.VISIBLE

                    layout_import_text_value.setClearErrorOnType()
                    btn_import_text.setOnClickListener {
                        val text = layout_import_text_value.editText?.text.toString()

                        if (text.isEmpty()) {
                            layout_import_text_value.error = "Text is empty"

                            return@setOnClickListener
                        }

                        try {
                            val accounts = accountExporter.import(text)

                            setImportResult(accounts)
                        } catch (e: Exception) {
                            val dialog = ErrorDialog()

                            dialog.setErrorMessage("Import data is corrupt")    //FIXME: Show detailed error with account index ecc
                            dialog.show(supportFragmentManager, null)
                        }
                    }
                    setImportResult(emptyList())
                }
            }
        }
        btn_import_confirm.setOnClickListener {
            if (accounts.isEmpty()) {
                makeToast("No accounts to import")
            } else {
                lifecycleScope.launch {
                    accountDao.insert(accounts) //TODO: Let the user choose and tell them which are going to get replaced

                    startActivity(AccountListActivity::class) {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> false
        }
    }

    private fun setImportResult(accounts: List<Account>) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            accounts.map { it.displayName }
        )

        txt_import_output.adapter = adapter
        this.accounts = accounts
    }
}