package com.majidbahmani.rota.core.domain.fake

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.repository.PoiRepository

/** Records requests and returns [result]. */
class FakePoiRepository(
    var result: Result<List<Poi>> = Result.success(emptyList()),
) : PoiRepository {

    data class Request(val center: GeoPoint, val radiusMeters: Int, val categories: Set<PoiCategory>)

    val requests = mutableListOf<Request>()

    override suspend fun getNearbyPois(
        center: GeoPoint,
        radiusMeters: Int,
        categories: Set<PoiCategory>,
    ): Result<List<Poi>> {
        requests += Request(center, radiusMeters, categories)
        return result
    }
}
