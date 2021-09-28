package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.authenticator.models.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account WHERE id = :id")
    suspend fun getAccount(id: Long): Account

    @Query("SELECT * FROM Account WHERE name = :name AND issuer = :issuer")
    suspend fun getAccount(name: String, issuer: String): Account

    @Query("SELECT * FROM Account")
    fun getAll(): LiveData<List<Account>>

    @Deprecated("Get the secret directly from the account object")
    @Query("SELECT secret FROM Account WHERE name = :name and issuer = :issuer")
    suspend fun getSecret(name: String, issuer: String): String

    @Query("SELECT COUNT(id) FROM Account WHERE name = :name AND issuer = :issuer")
    suspend fun getCount(name: String, issuer: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg accounts: Account)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(account: Account): Int

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM Account")
    suspend fun deleteAll()
}