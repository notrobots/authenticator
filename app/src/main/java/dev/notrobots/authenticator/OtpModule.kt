package dev.notrobots.authenticator

import android.content.Context
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.notrobots.authenticator.models.TotpClock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OtpModule {
    @Provides
    @Singleton
    fun provideTotpClock(@ApplicationContext applicationContext: Context): TotpClock {
        return TotpClock(
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        )
    }
}