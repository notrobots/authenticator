package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.authenticator.models.Tag
import dev.notrobots.authenticator.models.TagWithAccounts

@Dao
interface TagDao {
    @Query("SELECT * FROM Tag")
    fun getTags(): List<Tag>

    @Query("SELECT * FROM Tag")
    fun getTagsLive(): LiveData<List<Tag>>

    @Transaction
    @Query("SELECT * FROM Tag")
    fun getTagsWithAccounts(): List<TagWithAccounts>

    @Transaction
    @Query("SELECT * FROM Tag")
    fun getTagsWithAccountsLive(): LiveData<List<TagWithAccounts>>

    @Transaction
    @Query("SELECT * FROM Tag WHERE tagId = :id")
    fun getTagWithAccounts(id: Int): TagWithAccounts

    @Transaction
    @Query("SELECT * FROM Tag WHERE tagId = :id")
    fun getTagWithAccountsLive(id: Int): LiveData<TagWithAccounts>

    @Transaction
    @Query("SELECT * FROM Tag WHERE name = :name")
    fun getTagWithAccounts(name: String): TagWithAccounts

    @Transaction
    @Query("SELECT * FROM Tag WHERE name = :name")
    fun getTagWithAccountsLive(name: String): LiveData<TagWithAccounts>

    @Query("SELECT EXISTS(SELECT * FROM Tag WHERE tagId = :id)")
    fun exists(id: Int): Boolean

    @Query("SELECT EXISTS(SELECT * FROM Tag WHERE name = :name)")
    fun exists(name: String): Boolean

    @Insert
    fun insert(tag: Tag): Long

    @Insert
    fun insert(tags: List<Tag>)

    @Update
    fun update(tag: Tag)

    @Delete
    fun delete(tag: Tag)
}