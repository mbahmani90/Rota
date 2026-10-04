package com.majidbahmani.rota.core.domain.repository

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDataSource

interface PoiRepository {

    /**
     * POIs from [source] of the given [categories] within [radiusMeters] of [center], in no
     * particular order. An empty [categories] set returns an empty list without a request.
     */
    suspend fun getNearbyPois(
        source: PoiDataSource,
        center: GeoPoint,
        radiusMeters: Int,
        categories: Set<PoiCategory>,
    ): Result<List<Poi>>
}
