package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.models.*
import dev.notrobots.authenticator.models.SortMode.Companion.ORDER_BY_ISSUER
import dev.notrobots.authenticator.models.SortMode.Companion.ORDER_BY_LABEL
import dev.notrobots.authenticator.models.SortMode.Companion.ORDER_BY_NAME
import dev.notrobots.authenticator.models.SortMode.Companion.SORT_ASC
import dev.notrobots.authenticator.models.SortMode.Companion.SORT_DESC
import dev.notrobots.authenticator.ui.accountlist.AccountListViewModel

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
            SELECT * 
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

    @Insert
    suspend fun insert(account: Account): Long

    @Insert
    suspend fun insert(accounts: List<Account>): List<Long>

    /**
     * Inserts the given [account] into the database and takes care of the ordering.
     */
    @Transaction
    suspend fun insert(account: Account, incrementOrder: Boolean): Long {
        if (incrementOrder) {
            val last = getLargestOrder()

            account.order = last + 1
        }

        Logger.logd("Adding new account")
        return insert(account)
    }

    /**
     * Inserts the given [account] and changes its name so that it doesn't collide with
     * another existing account.
     *
     * @return The id of the inserted account.
     */
    @Transaction
    suspend fun insertAccountWithSameName(account: Account): Long {
        try {
            val rgx = Regex("\\s(\\d+)$")
            val lastSimilarName = getSimilarNames("${account.name}%").maxOf { it }
            val match = rgx.find(lastSimilarName)
            val name = if (match == null) {
                "$lastSimilarName 1"
            } else {
                val value = match.groupValues[1].toInt()

                lastSimilarName.replace(rgx, " ${value + 1}")
            }

            val newAccount = account.clone().apply {
                this.name = name
            }

            return insert(newAccount, true)
        }catch (e: Exception) {
            Logger.loge("DB error", e)
            throw Exception(e)
        }
    }

    @Update
    suspend fun update(account: Account): Int

    @Update
    suspend fun update(accounts: List<Account>): Int

    /**
     * Updates the given [account].
     */
    suspend fun update(account: Account, isDummy: Boolean) {
        if (isDummy) {
            // Since we are updating the account using a dummy account with the new values
            // We might need to fetch the corresponding account using name, label and issuer
            // We then set the id of the account we want to update to our dummy account and pass that
            // to the update function
            val stored = getAccount(account.name, account.label, account.issuer)

            if (account.accountId == Account.DEFAULT_ID) {
                if (stored != null) {
                    account.accountId = stored.accountId
                } else {
                    Logger.loge("Cannot update id: Account not found")
                    return
                }
            }

            if (account.order == Account.DEFAULT_ORDER) {
                if (stored != null) {
                    account.order = stored.order
                } else {
                    Logger.loge("Cannot update order: Account not found")
                    return
                }
            }
        }

        update(account)
        Logger.logd("Updating account")
    }

    @Delete
    suspend fun delete(account: Account): Int

    @Delete
    suspend fun delete(accounts: List<Account>): Int

    @Query("DELETE FROM Account")
    suspend fun deleteAll(): Int
}