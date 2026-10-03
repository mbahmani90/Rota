package com.majidbahmani.rota.core.domain.usecase

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.NearbyPoi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.distanceTo
import com.majidbahmani.rota.core.domain.repository.PoiRepository
import javax.inject.Inject

/**
 * Nearest POIs first, each with its distance from [center]. The repository returns them
 * unordered; car screens can only show a few items, so the order decides what the driver sees.
 */
class GetNearbyPoisUseCase @Inject constructor(
    private val repository: PoiRepository,
) {
    suspend operator fun invoke(
        center: GeoPoint,
        radiusMeters: Int,
        categories: Set<PoiCategory>,
    ): Result<List<NearbyPoi>> =
        repository.getNearbyPois(center, radiusMeters, categories).map { pois ->
            pois.map { NearbyPoi(it, center.distanceTo(it.location)) }
                .sortedBy { it.distanceMeters }
        }
}
