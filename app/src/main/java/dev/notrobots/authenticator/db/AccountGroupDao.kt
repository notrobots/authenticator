package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.authenticator.models.AccountGroup
import dev.notrobots.authenticator.models.BaseAccount
import dev.notrobots.authenticator.models.GroupWithAccounts

@Dao
interface AccountGroupDao {
    @Query("SELECT * FROM AccountGroup")
    fun getGroups(): LiveData<List<AccountGroup>>

    @Transaction
    @Query("SELECT * FROM AccountGroup ORDER BY `order`=${BaseAccount.DEFAULT_ORDER}, `order`")
    fun getGroupsWithAccounts(): LiveData<List<GroupWithAccounts>>

    @Query("SELECT COUNT(id) FROM AccountGroup WHERE name = :name")
    suspend fun getCount(name: String): Int

    @Query("SELECT count(1) WHERE EXISTS (SELECT * FROM AccountGroup)")
    suspend fun isNotEmpty(): Int

    @Query("SELECT COALESCE(MAX(`order`), 0) FROM AccountGroup")
    suspend fun getLastOrder(): Long

    @Insert
    suspend fun insert(group: AccountGroup): Long

    @Insert
    suspend fun insert(groups: List<AccountGroup>): List<Long>

    @Update
    suspend fun update(group: AccountGroup): Int

    @Update
    suspend fun update(groups: List<AccountGroup>): Int

    @Delete
    suspend fun delete(group: AccountGroup) //FIXME: Fix the return parameters, they may be useful in the future

    @Delete
    suspend fun delete(groups: List<AccountGroup>) //FIXME: Fix the return parameters, they may be useful in the future

    @Query("DELETE FROM AccountGroup")
    suspend fun deleteAll()
}