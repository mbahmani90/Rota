package com.majidbahmani.rota.core.data.di

import com.majidbahmani.rota.core.data.repository.PoiRepositoryImpl
import com.majidbahmani.rota.core.domain.repository.PoiRepository
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
    abstract fun bindPoiRepository(impl: PoiRepositoryImpl): PoiRepository
}
