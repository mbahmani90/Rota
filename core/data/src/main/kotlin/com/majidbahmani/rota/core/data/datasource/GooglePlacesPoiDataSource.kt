package com.majidbahmani.rota.core.data.datasource

import com.majidbahmani.rota.core.data.mapper.searchNearbyRequest
import com.majidbahmani.rota.core.data.mapper.toDomain
import com.majidbahmani.rota.core.data.remote.google.GooglePlacesApi
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import javax.inject.Inject

/** Google Places API (New), Nearby Search with the Pro field mask. Needs an API key. */
class GooglePlacesPoiDataSource @Inject constructor(
    private val api: GooglePlacesApi,
) : PoiRemoteDataSource {

    override suspend fun getNearbyPois(center: GeoPoint, radiusMeters: Int, categories: Set<PoiCategory>): List<Poi> {
        // Google rejects a radius above 50 km with HTTP 400.
        val radius = radiusMeters.coerceAtMost(MAX_RADIUS_METERS)
        return api.searchNearby(searchNearbyRequest(center, radius, categories)).toDomain()
    }

    private companion object {
        const val MAX_RADIUS_METERS = 50_000
    }
}
