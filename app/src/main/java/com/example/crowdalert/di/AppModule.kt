package com.example.crowdalert.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt [Module] for application-scoped dependencies.
 * Extra modules (e.g. [FirebaseModule]) hold Firebase clients.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
