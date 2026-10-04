package com.majidbahmani.rota.core.data.fake

import com.majidbahmani.rota.core.data.datasource.PoiRemoteDataSource
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory

/** Counts calls and returns [pois], or throws [error] when set. */
class FakePoiRemoteDataSource(
    var pois: List<Poi> = emptyList(),
    var error: Throwable? = null,
) : PoiRemoteDataSource {

    var calls = 0
        private set

    override suspend fun getNearbyPois(center: GeoPoint, radiusMeters: Int, categories: Set<PoiCategory>): List<Poi> {
        calls++
        error?.let { throw it }
        return pois
    }
}
