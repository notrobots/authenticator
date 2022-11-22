package dev.notrobots.authenticator.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountTagCrossRef
import dev.notrobots.authenticator.models.Tag

@Database(
    entities = [
        Account::class,
        Tag::class,
        AccountTagCrossRef::class
    ],
    version = 1
)
abstract class AuthenticatorDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun tagDao(): TagDao
    abstract fun accountTagCrossRefDao(): AccountTagCrossRefDao
}