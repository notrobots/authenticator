package dev.notrobots.authenticator.db

import androidx.lifecycle.LiveData
import androidx.room.*
import dev.notrobots.authenticator.models.Tag
import dev.notrobots.authenticator.models.TagWithAccounts

@Dao
interface TagDao {
    @Query("SELECT * FROM Tag")
    suspend fun getTags(): List<Tag>

    @Query("SELECT * FROM Tag")
    fun getTagsLive(): LiveData<List<Tag>>

    @Query("SELECT * FROM Tag WHERE name = :name")
    suspend fun getTag(name: String): Tag?

    @Query("SELECT EXISTS(SELECT * FROM Tag WHERE tagId = :id)")
    suspend fun exists(id: Long): Boolean

    @Query("SELECT EXISTS(SELECT * FROM Tag WHERE name = :name)")
    suspend fun exists(name: String): Boolean

    @Insert
    suspend fun insert(tag: Tag): Long

    @Insert
    suspend fun insert(tags: List<Tag>): List<Long>

    @Insert
    suspend fun insert(tags: Set<Tag>): List<Long>

    @Upsert
    suspend fun insertOrUpdate(tag: Tag): Long  //TODO: Create a base dao with these queries already defined

    @Upsert
    suspend fun insertOrUpdate(tags: List<Tag>): List<Long>

    @Upsert
    suspend fun insertOrUpdate(tags: Set<Tag>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(tag: Tag): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(tags: List<Tag>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(tags: Set<Tag>): List<Long>

    @Update
    suspend fun update(tag: Tag): Int

    @Delete
    suspend fun delete(tag: Tag): Int

    @Query("DELETE FROM Tag")
    suspend fun deleteAll(): Int
}