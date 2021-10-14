package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup

//@Dao
//interface AccountGroupDao {
//    @Query("SELECT * FROM AccountGroup")
//    fun getAll(): LiveData<List<AccountGroup>>
//
//    suspend fun insert(vararg groups: AccountGroup)
//
//    @Update
//    suspend fun update(group: AccountGroup)
//
//    @Delete
//    suspend fun delete(group: AccountGroup)
//}