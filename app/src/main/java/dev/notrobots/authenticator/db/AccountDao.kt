package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import dev.notrobots.authenticator.models.Account

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account ORDER BY `order`")
    suspend fun getAccounts(): List<Account>

    @Query("SELECT * FROM Account ORDER BY `order`")
    fun getAccountsLive(): LiveData<List<Account>>

    @Query(
        "SELECT * FROM Account ORDER BY " +
        "CASE WHEN :direction = 0 THEN name END ASC," +
        "CASE WHEN :direction = 1 THEN name END DESC"
    )
    fun getAccountsOrderedByName(direction: Int): LiveData<List<Account>>

    @Query(
        "SELECT * FROM Account ORDER BY " +
        "CASE WHEN :direction = 0 THEN label END ASC," +
        "CASE WHEN :direction = 1 THEN label END DESC"
    )
    fun getAccountsOrderedByLabel(direction: Int): LiveData<List<Account>>

    @Query(
        "SELECT * FROM Account ORDER BY " +
        "CASE WHEN :direction = 0 THEN issuer END ASC," +
        "CASE WHEN :direction = 1 THEN issuer END DESC"
    )
    fun getAccountsOrderedByIssuer(direction: Int): LiveData<List<Account>>

    @Query("SELECT * FROM Account WHERE id = :id")
    suspend fun getAccount(id: Long): Account?

    @Query("SELECT * FROM Account WHERE name = :name AND label = :label AND issuer = :issuer")
    suspend fun getAccount(name: String, label: String, issuer: String): Account?

    @Query("SELECT COUNT(name) FROM Account WHERE name = :name AND label = :label AND issuer = :issuer")
    suspend fun getCount(name: String, label: String, issuer: String): Int

    @Transaction
    suspend fun exists(name: String, label: String, issuer: String): Boolean {
        return getCount(name, label, issuer) > 0
    }

    @Transaction
    suspend fun exists(account: Account): Boolean {
        return getCount(account.name, account.label, account.issuer) > 0
    }

    @Query("SELECT COALESCE(MAX(`order`), 0) FROM Account")
    suspend fun getLargestOrder(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accounts: List<Account>)

    @Update
    suspend fun update(account: Account)

    @Update
    suspend fun update(accounts: List<Account>)

    @Delete
    suspend fun delete(account: Account)

    @Delete
    suspend fun delete(accounts: List<Account>)

    @Query("DELETE FROM Account")
    suspend fun deleteAll()
}