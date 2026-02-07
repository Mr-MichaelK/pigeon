package com.example.pigeon.di

import com.example.pigeon.data.repository.local.LocalUserRepository
import com.example.pigeon.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        localUserRepository: LocalUserRepository
    ): UserRepository
}
