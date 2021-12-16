package dev.notrobots.authenticator.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.util.Coroutines.coroutine
import junit.framework.TestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountDaoTest : TestCase() {
    private lateinit var database: AuthenticatorDatabase
    private lateinit var accountDao: AccountDao

    @Before
    public override fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AuthenticatorDatabase::class.java
        ).build()
        accountDao = database.accountDao()
    }

    @Test
    fun testExists() {
        val account = Account("johndoe", "22334455")

        coroutine {
            assert(!accountDao.exists(account))
            assert(!accountDao.exists(account))
            accountDao.insert(account)
            assert(accountDao.exists(account))
        }
    }
}