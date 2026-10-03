package com.majidbahmani.rota.core.data.repository

import com.majidbahmani.rota.core.data.mapper.toDomain
import com.majidbahmani.rota.core.data.remote.OverpassApi
import com.majidbahmani.rota.core.data.remote.OverpassServerException
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.repository.PoiRepository
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class PoiRepositoryImpl @Inject constructor(
    private val api: OverpassApi,
) : PoiRepository {

    override suspend fun getNearbyPois(
        center: GeoPoint,
        radiusMeters: Int,
        categories: Set<PoiCategory>,
    ): Result<List<Poi>> {
        if (categories.isEmpty()) return Result.success(emptyList())
        val query = buildQuery(center, radiusMeters, categories)
        return try {
            val response = api.interpreter(query)
            // Overpass can answer 200 with the error only in `remark`.
            response.remark?.let { throw OverpassServerException(it) }
            Result.success(response.toDomain())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Overpass QL; filtering happens on the server to keep responses small. */
    private fun buildQuery(center: GeoPoint, radiusMeters: Int, categories: Set<PoiCategory>): String {
        require(radiusMeters > 0) { "radiusMeters must be positive, was $radiusMeters" }
        // Locale.ROOT: always a dot as decimal separator; fixed digits: no scientific notation.
        val around = String.format(
            Locale.ROOT,
            "(around:%d,%.6f,%.6f)",
            radiusMeters,
            center.latitude,
            center.longitude,
        )
        val statements = PoiCategory.entries
            .filter { it in categories }
            .joinToString("\n") { "  nwr${it.tagFilter()}$around;" }
        return "[out:json][timeout:${OverpassApi.QUERY_TIMEOUT_SECONDS}];\n(\n$statements\n);\nout center tags;"
    }

    private fun PoiCategory.tagFilter(): String = when (this) {
        PoiCategory.EV_CHARGER -> """["amenity"="charging_station"]"""
        PoiCategory.FUEL -> """["amenity"="fuel"]"""
        // Skip on-street spaces, private garages and private or residents-only parking.
        // `!~` also keeps elements that don't have the tag.
        PoiCategory.PARKING -> """["amenity"="parking"]""" +
            """["parking"!~"^(street_side|lane|on_kerb|half_on_kerb|layby|garage_boxes|sheds)$"]""" +
            """["access"!~"^(private|no|permit)$"]"""
    }
}
