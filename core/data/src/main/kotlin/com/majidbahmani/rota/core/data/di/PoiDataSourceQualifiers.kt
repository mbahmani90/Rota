package com.majidbahmani.rota.core.data.di

import javax.inject.Qualifier

/** Both data sources have the type PoiRemoteDataSource, so Hilt needs a name for each. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OverpassSource

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GooglePlacesSource
