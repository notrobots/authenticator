package dev.notrobots.authenticator.ui.accountlist

import android.content.Context
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.notrobots.authenticator.db.AccountDao
import dev.notrobots.authenticator.db.AuthenticatorDatabase
import dev.notrobots.authenticator.db.AuthenticatorDatabaseModule
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import junit.framework.TestCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountListViewModelTest : TestCase() {
    private lateinit var viewModel: AccountListViewModel
    private lateinit var database: AuthenticatorDatabase

    @Before
    public override fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AuthenticatorDatabase::class.java
        ).build()
        viewModel = AccountListViewModel(
            database.accountDao(),
            database.accountGroupDao(),
        )
    }

    @Test
    fun testAddAccount() {
        val account = Account("johndoe", "2233445566").apply {
            issuer = "github.com"
            label = "Github"
        }

        runBlocking {
            viewModel.addAccount(account)

            assertThrows<Exception> {
                viewModel.addAccount(account)
            }

            assertDoesNotThrow {
                viewModel.addAccount(account.apply { label = "" })
            }
        }
    }

    @Test
    fun testAddGroup() {
        val group = AccountGroup("Group 1")

        runBlocking {
            assertDoesNotThrow {
                viewModel.addGroup(group)
            }
            assertThrows<Exception> {
                viewModel.addGroup(group)
            }
        }
    }

    @Test
    fun testCheckIfAccountExists() {
        val account = Account("johndoe", "22334455")

        runBlocking {
            viewModel.addAccount(account)

            assert(viewModel.checkIfAccountExists(account))
        }
    }

    @Test
    fun testCheckIfGroupExists() {
        val group = AccountGroup("Group 1")

        runBlocking {
            viewModel.addGroup(group)

            assert(viewModel.checkIfGroupExists(group))
            assert(viewModel.checkIfGroupExists(group.name))
        }
    }
}