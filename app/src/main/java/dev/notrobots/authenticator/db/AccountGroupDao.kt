package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.authenticator.models.AccountGroup
import dev.notrobots.authenticator.models.BaseAccount
import dev.notrobots.authenticator.models.GroupWithAccounts

@Dao
interface AccountGroupDao {
    @Query("SELECT * FROM AccountGroup")
    suspend fun getGroups(): List<AccountGroup>

    @Transaction
    @Query("SELECT * FROM AccountGroup ORDER BY `order`=${BaseAccount.DEFAULT_ORDER}, `order`")
    fun getGroupsWithAccounts(): LiveData<List<GroupWithAccounts>>

    @Query("SELECT * FROM AccountGroup WHERE name = :name")
    suspend fun getGroup(name: String): AccountGroup?

    @Query("SELECT * FROM AccountGroup WHERE id = :id")
    suspend fun getGroup(id: Long): AccountGroup?

    @Query("SELECT COUNT(id) FROM AccountGroup WHERE name = :name")
    suspend fun getCount(name: String): Int

    @Transaction
    suspend fun exists(name: String): Boolean {
        return getCount(name) > 0
    }

    @Transaction
    suspend fun exists(group: AccountGroup): Boolean {
        return getCount(group.name) > 0
    }

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
    suspend fun delete(group: AccountGroup)

    //FIXME: Fix the return parameters, they may be useful in the future
    @Delete
    suspend fun delete(groups: List<AccountGroup>)

    @Transaction
    suspend fun deleteGroupWithAccounts(group: AccountGroup) {
        deleteAccounts(group.id)
        delete(group)
    }

    @Transaction
    suspend fun deleteGroupWithAccounts(groups: List<AccountGroup>) {
        for (group in groups) {
            deleteGroupWithAccounts(group)
        }
    }

    @Query("DELETE FROM Account WHERE groupId = :groupId")
    suspend fun deleteAccounts(groupId: Long)

    @Query("DELETE FROM AccountGroup")
    suspend fun deleteAll()
}