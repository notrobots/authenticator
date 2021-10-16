package dev.notrobots.authenticator.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.notrobots.authenticator.models.Account
import dev.notrobots.authenticator.models.AccountGroup

@Database(entities = [Account::class, AccountGroup::class], version = 1)
abstract class AuthenticatorDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun accountGroupDao(): AccountGroupDao
}