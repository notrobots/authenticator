package dev.notrobots.authenticator.db

import androidx.room.*
import dev.notrobots.authenticator.models.Account
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account")
    fun getAll(): Flow<List<Account>>

    @Query("SELECT secret FROM Account WHERE name = :name and issuer = :issuer")
    fun getSecret(name: String, issuer: String): Flow<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg accounts: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM Account")
    suspend fun deleteAll()
}