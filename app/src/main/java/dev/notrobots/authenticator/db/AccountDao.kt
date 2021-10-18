package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.authenticator.models.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account ORDER BY `order`")
    fun getAccounts(): LiveData<List<Account>>

    @Query("SELECT * FROM Account WHERE id = :id")
    suspend fun getAccount(id: Long): Account

    @Query("SELECT * FROM Account WHERE name = :name AND issuer = :issuer")
    suspend fun getAccount(name: String, issuer: String): Account

    @Query("SELECT COUNT(id) FROM Account WHERE name = :name AND issuer = :issuer")
    suspend fun getCount(name: String, issuer: String): Int

    @Query("SELECT COALESCE(MAX(`order`), 0) FROM Account")
    suspend fun getLastOrder(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg accounts: Account)

    @Update
    suspend fun update(account: Account)

    @Update
    suspend fun update(accounts: List<Account>)

    @Query("UPDATE Account SET `order` = :order WHERE id = :id")
    suspend fun updateOrder(id: Long, order: Long)

    @Delete
    suspend fun delete(account: Account)

    @Delete
    suspend fun delete(accounts: List<Account>)

    @Query("DELETE FROM Account")
    suspend fun deleteAll()
}