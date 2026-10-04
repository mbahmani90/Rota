package com.majidbahmani.rota.core.data.di

import android.content.Context
import com.majidbahmani.rota.core.data.BuildConfig
import com.majidbahmani.rota.core.data.remote.google.GooglePlacesApi
import com.majidbahmani.rota.core.data.remote.google.GooglePlacesConfig
import com.majidbahmani.rota.core.data.remote.google.GooglePlacesHeadersInterceptor
import com.majidbahmani.rota.core.data.remote.google.signingCertificateSha1
import com.majidbahmani.rota.core.data.remote.overpass.OverpassApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Overpass asks clients to identify themselves (fair-use policy).
    private const val USER_AGENT = "Rota (https://github.com/mbahmani90/Rota)"

    // Longer than the query's server timeout so Overpass can answer before OkHttp gives up.
    private const val READ_TIMEOUT_SECONDS = OverpassApi.QUERY_TIMEOUT_SECONDS + 10L

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                // BASIC, not BODY: responses can be hundreds of KB.
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            },
        )
        .build()

    @Provides
    @Singleton
    @OverpassRetrofit
    fun provideOverpassRetrofit(json: Json, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(OverpassApi.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideOverpassApi(@OverpassRetrofit retrofit: Retrofit): OverpassApi = retrofit.create(OverpassApi::class.java)

    @Provides
    @Singleton
    fun provideGooglePlacesConfig(): GooglePlacesConfig = GooglePlacesConfig(BuildConfig.MAPS_API_KEY)

    @Provides
    @Singleton
    @GooglePlacesRetrofit
    fun provideGooglePlacesRetrofit(
        json: Json,
        client: OkHttpClient,
        config: GooglePlacesConfig,
        @ApplicationContext context: Context,
    ): Retrofit {
        // Derived from the shared client, so the connection pool is shared (doc 06). Added after
        // the logging interceptor, so the key never appears in the log.
        val googleClient = client.newBuilder()
            .addInterceptor(
                GooglePlacesHeadersInterceptor(
                    apiKey = config.apiKey,
                    packageName = context.packageName,
                    certificateSha1 = { context.signingCertificateSha1() },
                ),
            )
            .build()
        return Retrofit.Builder()
            .baseUrl(GooglePlacesApi.BASE_URL)
            .client(googleClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideGooglePlacesApi(@GooglePlacesRetrofit retrofit: Retrofit): GooglePlacesApi =
        retrofit.create(GooglePlacesApi::class.java)
}
