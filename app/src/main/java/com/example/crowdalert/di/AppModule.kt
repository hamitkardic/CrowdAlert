package com.example.crowdalert.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt [Module] for application-scoped dependencies.
 * Firebase clients and repository bindings are added in later steps.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
