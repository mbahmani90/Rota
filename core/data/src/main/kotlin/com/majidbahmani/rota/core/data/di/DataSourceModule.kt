package com.majidbahmani.rota.core.data.di

import com.majidbahmani.rota.core.data.datasource.GooglePlacesPoiDataSource
import com.majidbahmani.rota.core.data.datasource.OverpassPoiDataSource
import com.majidbahmani.rota.core.data.datasource.PoiRemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    @OverpassSource
    abstract fun bindOverpassDataSource(impl: OverpassPoiDataSource): PoiRemoteDataSource

    @Binds
    @Singleton
    @GooglePlacesSource
    abstract fun bindGooglePlacesDataSource(impl: GooglePlacesPoiDataSource): PoiRemoteDataSource
}
