package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account ORDER BY `order`")
    fun getAccounts(): LiveData<List<Account>>

    @Query("SELECT * FROM Account WHERE name = :name AND label = :label AND issuer = :issuer")
    suspend fun getAccount(name: String, label: String, issuer: String): Account

    @Query("SELECT COUNT(name) FROM Account WHERE name = :name AND label = :label AND issuer = :issuer")
    suspend fun getCount(name: String, label: String, issuer: String): Int

    @Query("SELECT COALESCE(MAX(`order`), 0) FROM Account WHERE groupId = :groupId")
    suspend fun getLargestOrder(groupId: Long = Account.DEFAULT_GROUP_ID): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: Account)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accounts: List<Account>)

    @Update
    suspend fun update(account: Account)

    @Update
    suspend fun update(accounts: List<Account>)

//    @Query("UPDATE Account SET `order` = :order WHERE id = :id")
//    suspend fun updateOrder(id: Long, order: Long)

    @Delete
    suspend fun delete(account: Account)

    @Delete
    suspend fun delete(accounts: List<Account>)

    @Query("DELETE FROM Account")
    suspend fun deleteAll()
}