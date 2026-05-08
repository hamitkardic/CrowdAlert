package com.example.crowdalert.di

import android.content.Context
import androidx.room.Room
import com.example.crowdalert.data.room.CrowdAlertDatabase
import com.example.crowdalert.data.room.IncidentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideCrowdAlertDatabase(@ApplicationContext context: Context): CrowdAlertDatabase =
        Room
            .databaseBuilder(
                context,
                CrowdAlertDatabase::class.java,
                "crowdalert_database",
            ).build()

    @Provides
    @Singleton
    fun provideIncidentDao(database: CrowdAlertDatabase): IncidentDao = database.incidentDao()
}
