package com.example.crowdalert.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
            ).addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideIncidentDao(database: CrowdAlertDatabase): IncidentDao = database.incidentDao()

    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE incidents ADD COLUMN reportedByName TEXT")
            }
        }
}
