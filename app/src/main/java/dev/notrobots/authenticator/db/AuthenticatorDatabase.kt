package dev.notrobots.authenticator.db

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.notrobots.authenticator.models.Account

@Database(entities = [Account::class], version = 1)
abstract class AuthenticatorDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
}