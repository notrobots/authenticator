package dev.notrobots.authenticator.db

import androidx.room.*
import dev.notrobots.androidstuff.util.Logger
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountTagCrossRef
import dev.notrobots.authenticator.models.AccountWithTags
import dev.notrobots.authenticator.models.Tag

@Dao
interface AccountTagCrossRefDao {
    @Transaction
    @Query("SELECT * FROM Account")
    suspend fun getAccountsWithTags(): List<AccountWithTags>

    @Insert
    suspend fun insert(accountTagCrossRef: AccountTagCrossRef): Long

    @Query("INSERT INTO AccountTagCrossRef VALUES(:accountId, :tagId)")
    suspend fun insert(accountId: Long, tagId: Long): Long

    @Transaction
    suspend fun insertWithNames(accountId: Long, tagNames: Iterable<String>?) {
        tagNames?.forEach { name ->
            val tagId = getTag(name).tagId

            insert(accountId, tagId)
        }
    }

    @Query("SELECT * FROM Tag WHERE name = :name")
    suspend fun getTag(name: String): Tag

    @Delete
    suspend fun delete(accountTagCrossRef: AccountTagCrossRef): Int

    /**
     * Removes all the records with the given [accountId].
     */
    @Query("DELETE FROM AccountTagCrossRef WHERE accountId = :accountId")
    suspend fun deleteWithAccountId(accountId: Long): Int

    /**
     * Removes all the tags from the account with the given [tagId].
     */
    @Query("DELETE FROM AccountTagCrossRef WHERE tagId = :tagId")
    suspend fun deleteWithTagId(tagId: Long): Int

    /**
     * Removes the tag with the given [tagId] from the account with the given [accountId].
     */
    @Query("DELETE FROM AccountTagCrossRef WHERE accountId = :accountId and tagId = :tagId")
    suspend fun delete(accountId: Long, tagId: Long): Int

    /**
     * Returns whether or not there's a record with the given [accountId] and [tagId].
     */
    @Query(
        """
        SELECT EXISTS(
            SELECT * 
            FROM AccountTagCrossRef
            WHERE accountId = :accountId and tagId = :tagId
        )
        """
    )
    suspend fun exists(accountId: Long, tagId: Long): Boolean
}