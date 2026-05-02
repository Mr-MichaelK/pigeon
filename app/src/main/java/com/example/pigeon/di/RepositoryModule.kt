package com.example.pigeon.di

import com.example.pigeon.data.repository.LocationRepositoryImpl
import com.example.pigeon.domain.repository.LocationRepository
import com.example.pigeon.data.repository.local.LocalUserRepository
import com.example.pigeon.domain.repository.UserRepository
import com.example.pigeon.data.repository.EventRepositoryImpl
import com.example.pigeon.domain.repository.EventRepository
import com.example.pigeon.data.repository.VerificationRepositoryImpl
import com.example.pigeon.domain.repository.VerificationRepository
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
    abstract fun bindUserRepository(
        localUserRepository: LocalUserRepository
    ): UserRepository

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        eventRepositoryImpl: EventRepositoryImpl
    ): EventRepository

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindVerificationRepository(
        verificationRepositoryImpl: VerificationRepositoryImpl
    ): VerificationRepository
}
