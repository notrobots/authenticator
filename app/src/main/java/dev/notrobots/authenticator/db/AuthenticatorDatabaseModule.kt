package dev.notrobots.authenticator.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.notrobots.authenticator.R
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object AuthenticatorDatabaseModule {
    @Provides
    fun provideAccountDao(database: AuthenticatorDatabase): AccountDao {
        return database.accountDao()
    }

    @Provides
    fun provideTagDao(database: AuthenticatorDatabase): TagDao {
        return database.tagDao()
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): AuthenticatorDatabase {
        return Room.databaseBuilder(
            appContext,
            AuthenticatorDatabase::class.java,
            appContext.getString(R.string.label_app_name)
        ).build()
    }
}