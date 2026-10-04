package com.majidbahmani.rota.core.data.datasource

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory

/** One POI provider. Throws on failure; the repository turns errors into a Result. */
interface PoiRemoteDataSource {

    /** The repository has already checked that [radiusMeters] > 0 and [categories] isn't empty. */
    suspend fun getNearbyPois(center: GeoPoint, radiusMeters: Int, categories: Set<PoiCategory>): List<Poi>
}
