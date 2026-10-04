package com.majidbahmani.rota.core.domain.usecase

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.NearbyPoi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.distanceTo
import com.majidbahmani.rota.core.domain.repository.PoiRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Nearest POIs first, each with its distance from [center], from the data source currently
 * chosen in the settings. The repository returns them unordered; car screens can only show a
 * few items, so the order decides what the driver sees.
 */
class GetNearbyPoisUseCase @Inject constructor(
    private val repository: PoiRepository,
    private val observePoiDataSource: ObservePoiDataSourceUseCase,
) {
    suspend operator fun invoke(
        center: GeoPoint,
        radiusMeters: Int,
        categories: Set<PoiCategory>,
    ): Result<List<NearbyPoi>> {
        val source = observePoiDataSource().first()
        return repository.getNearbyPois(source, center, radiusMeters, categories).map { pois ->
            pois.map { NearbyPoi(it, center.distanceTo(it.location)) }
                .sortedBy { it.distanceMeters }
        }
    }
}
