package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.room.*
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.models.SortMode.Companion.ORDER_BY_ISSUER
import dev.notrobots.authenticator.models.SortMode.Companion.ORDER_BY_LABEL
import dev.notrobots.authenticator.models.SortMode.Companion.ORDER_BY_NAME
import dev.notrobots.authenticator.models.SortMode.Companion.SORT_ASC
import dev.notrobots.authenticator.models.SortMode.Companion.SORT_DESC

@Dao
interface AccountDao {
    @Query("SELECT * FROM Account WHERE accountId = :id")
    suspend fun getAccount(id: Long): Account?

    @Query("SELECT * FROM Account WHERE name = :name AND label = :label AND issuer = :issuer")
    suspend fun getAccount(name: String, label: String, issuer: String): Account?

    @Transaction
    @Query("SELECT * FROM Account WHERE accountId = :accountId")
    suspend fun getAccountWithTags(accountId: Long): AccountWithTags

    @Transaction
    @Query("SELECT * FROM Account WHERE accountId = :accountId")
    fun getAccountWithTagsLive(accountId: Long): LiveData<AccountWithTags>

    @Query("SELECT * FROM Account ORDER BY `order`")
    suspend fun getAccounts(): List<Account>

    @Query("SELECT * FROM Account ORDER BY `order`")
    fun getAccountsLive(): LiveData<List<Account>>

    @Transaction
    @Query("SELECT * FROM Account")
    suspend fun getAccountsWithTags(): List<AccountWithTags>

    @Transaction
    @Query("SELECT * FROM Account")
    fun getAccountsWithTagsLive(): LiveData<List<AccountWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM Account
        LEFT JOIN AccountTagCrossRef 
            ON AccountTagCrossRef.accountId = Account.accountId 
        WHERE tagId = :tagId 
        GROUP BY Account.accountId
        ORDER BY 
            CASE WHEN :orderBy = $ORDER_BY_NAME AND :orderDir = $SORT_ASC THEN name END ASC,
            CASE WHEN :orderBy = $ORDER_BY_LABEL AND :orderDir = $SORT_ASC THEN label END ASC,
            CASE WHEN :orderBy = $ORDER_BY_ISSUER AND :orderDir = $SORT_ASC THEN issuer END ASC,
            CASE WHEN :orderBy = $ORDER_BY_NAME AND :orderDir = $SORT_DESC THEN name END DESC,
            CASE WHEN :orderBy = $ORDER_BY_LABEL AND :orderDir = $SORT_DESC THEN label END DESC,
            CASE WHEN :orderBy = $ORDER_BY_ISSUER AND :orderDir = $SORT_DESC THEN issuer END DESC
        """
    )
    fun getAccountsWithTagsLive(orderDir: Int, orderBy: Int, tagId: Long): LiveData<List<AccountWithTags>>

    @Transaction
    @Query(
        """
        SELECT * FROM Account
        LEFT JOIN AccountTagCrossRef ON AccountTagCrossRef.accountId = Account.accountId 
        GROUP BY Account.accountId
        ORDER BY 
            CASE WHEN :orderBy = $ORDER_BY_NAME AND :orderDir = $SORT_ASC THEN name END ASC,
            CASE WHEN :orderBy = $ORDER_BY_LABEL AND :orderDir = $SORT_ASC THEN label END ASC,
            CASE WHEN :orderBy = $ORDER_BY_ISSUER AND :orderDir = $SORT_ASC THEN issuer END ASC,
            CASE WHEN :orderBy = $ORDER_BY_NAME AND :orderDir = $SORT_DESC THEN name END DESC,
            CASE WHEN :orderBy = $ORDER_BY_LABEL AND :orderDir = $SORT_DESC THEN label END DESC,
            CASE WHEN :orderBy = $ORDER_BY_ISSUER AND :orderDir = $SORT_DESC THEN issuer END DESC
        """
    )
    fun getAccountsWithTagsLive(orderDir: Int, orderBy: Int): LiveData<List<AccountWithTags>>

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)    //XXX Is this needed?
    suspend fun insert(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accounts: List<Account>): List<Long>

    @Insert
    suspend fun insert(accountTagCrossRef: AccountTagCrossRef)

    @Update
    suspend fun update(account: Account)

    @Update
    suspend fun update(accounts: List<Account>)

    @Delete
    suspend fun delete(account: Account)

    @Delete
    suspend fun delete(accounts: List<Account>)

    @Delete
    suspend fun delete(accountTagCrossRef: AccountTagCrossRef)

    @Query("DELETE FROM AccountTagCrossRef WHERE accountId = :accountId")
    suspend fun removeTags(accountId: Long)

    @Query("DELETE FROM Account")
    suspend fun deleteAll()
}