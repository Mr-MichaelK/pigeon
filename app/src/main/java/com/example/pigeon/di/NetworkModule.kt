package com.example.pigeon.di

import com.example.pigeon.data.network.NearbySyncManagerImpl
import com.example.pigeon.domain.network.NearbySyncManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {

    @Binds
    @Singleton
    abstract fun bindNearbySyncManager(
        nearbySyncManagerImpl: NearbySyncManagerImpl
    ): NearbySyncManager
}
