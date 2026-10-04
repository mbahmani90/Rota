package com.majidbahmani.rota.core.data.repository

import com.majidbahmani.rota.core.data.datasource.PoiRemoteDataSource
import com.majidbahmani.rota.core.data.di.GooglePlacesSource
import com.majidbahmani.rota.core.data.di.OverpassSource
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.repository.PoiRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Coordinates the POI data sources: routes each search to the requested [PoiDataSource].
 * The only place where data-source errors become a Result.
 */
class PoiRepositoryImpl @Inject constructor(
    @param:OverpassSource private val overpass: PoiRemoteDataSource,
    @param:GooglePlacesSource private val googlePlaces: PoiRemoteDataSource,
) : PoiRepository {

    override suspend fun getNearbyPois(
        source: PoiDataSource,
        center: GeoPoint,
        radiusMeters: Int,
        categories: Set<PoiCategory>,
    ): Result<List<Poi>> {
        // Invalid input is a programming error, so it throws instead of becoming a failure.
        require(radiusMeters > 0) { "radiusMeters must be positive, was $radiusMeters" }
        if (categories.isEmpty()) return Result.success(emptyList())
        return try {
            Result.success(dataSourceFor(source).getNearbyPois(center, radiusMeters, categories))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun dataSourceFor(source: PoiDataSource): PoiRemoteDataSource = when (source) {
        PoiDataSource.OVERPASS -> overpass
        PoiDataSource.GOOGLE_PLACES -> googlePlaces
    }
}
