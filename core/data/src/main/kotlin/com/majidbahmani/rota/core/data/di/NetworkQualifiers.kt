package com.majidbahmani.rota.core.data.di

import javax.inject.Qualifier

/** One Retrofit per base URL (doc 06), so each needs a name. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OverpassRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GooglePlacesRetrofit
