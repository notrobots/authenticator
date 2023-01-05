package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
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

    @Transaction            //TODO: A "dummy" account is frequently used in the app, this should be named as such to avoid confusion
    suspend fun getAccount(dummy: Account): Account? {
        return getAccount(dummy.name, dummy.label, dummy.issuer)
    }

    @Transaction
    @Query(
        """
        SELECT Tag.*
        FROM Tag
        INNER JOIN AccountTagCrossRef ON AccountTagCrossRef.tagId = Tag.tagId
        WHERE accountId = :accountId
        """
    )
    suspend fun getTags(accountId: Long): List<Tag>

    @Query("SELECT * FROM Account ORDER BY `order`")
    suspend fun getAccounts(): List<Account>

    @Query("SELECT * FROM Account ORDER BY `order`")
    fun getAccountsLive(): LiveData<List<Account>>

    @Transaction
    @Query(
        """
        SELECT Account.*
        FROM Account
        LEFT JOIN AccountTagCrossRef ON AccountTagCrossRef.accountId = Account.accountId 
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
    fun getAccountsLive(orderDir: Int, orderBy: Int, tagId: Long): LiveData<List<Account>>

    @Transaction
    @Query(
        """
        SELECT Account.*
        FROM Account
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
    fun getAccountsLive(orderDir: Int, orderBy: Int): LiveData<List<Account>>

    @Query("SELECT name FROM Account WHERE name LIKE :query")
    suspend fun getSimilarNames(query: String): List<String>

    @Query(
        """
        SELECT EXISTS(
            SELECT accountId 
            FROM Account 
            WHERE name = :name and label = :label and issuer = :issuer
        )
        """
    )
    suspend fun exists(name: String, label: String, issuer: String): Boolean

    @Transaction
    suspend fun exists(account: Account): Boolean {
        return exists(account.name, account.label, account.issuer)
    }

    @Query("SELECT COALESCE(MAX(`order`), 0) FROM Account")
    suspend fun getLargestOrder(): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)    //XXX Is this needed?
    suspend fun insert(account: Account): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(accounts: List<Account>): List<Long>

    @Update
    suspend fun update(account: Account): Int

    @Update
    suspend fun update(accounts: List<Account>): Int

    @Delete
    suspend fun delete(account: Account): Int

    @Delete
    suspend fun delete(accounts: List<Account>): Int

    @Query("DELETE FROM Account")
    suspend fun deleteAll(): Int
}