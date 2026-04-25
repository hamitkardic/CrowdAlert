package com.example.crowdalert.di

import com.example.crowdalert.data.repository.AuthRepository
import com.example.crowdalert.data.repository.AuthRepositoryImpl
import com.example.crowdalert.data.repository.IncidentRepository
import com.example.crowdalert.data.repository.IncidentRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindIncidentRepository(impl: IncidentRepositoryImpl): IncidentRepository
}
