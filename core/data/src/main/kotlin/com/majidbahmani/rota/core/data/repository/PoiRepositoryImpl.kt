package com.majidbahmani.rota.core.data.repository

import com.majidbahmani.rota.core.data.datasource.PoiRemoteDataSource
import com.majidbahmani.rota.core.data.di.GooglePlacesSource
import com.majidbahmani.rota.core.data.di.OverpassSource
import com.majidbahmani.rota.core.data.remote.google.GooglePlacesConfig
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.repository.PoiRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Coordinates the POI data sources: Google Places when an API key is set, Overpass otherwise.
 * The only place where data-source errors become a Result.
 */
class PoiRepositoryImpl @Inject constructor(
    @param:OverpassSource private val overpass: PoiRemoteDataSource,
    @param:GooglePlacesSource private val googlePlaces: PoiRemoteDataSource,
    private val googlePlacesConfig: GooglePlacesConfig,
) : PoiRepository {

    override suspend fun getNearbyPois(
        center: GeoPoint,
        radiusMeters: Int,
        categories: Set<PoiCategory>,
    ): Result<List<Poi>> {
        // Invalid input is a programming error, so it throws instead of becoming a failure.
        require(radiusMeters > 0) { "radiusMeters must be positive, was $radiusMeters" }
        if (categories.isEmpty()) return Result.success(emptyList())
        return try {
            Result.success(selectedSource().getNearbyPois(center, radiusMeters, categories))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun selectedSource(): PoiRemoteDataSource =
        if (googlePlacesConfig.isAvailable) googlePlaces else overpass
}
