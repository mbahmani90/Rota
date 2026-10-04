package com.majidbahmani.rota.core.data.mapper

import com.majidbahmani.rota.core.data.remote.google.GooglePlacesApi
import com.majidbahmani.rota.core.data.remote.google.dto.CircleDto
import com.majidbahmani.rota.core.data.remote.google.dto.LatLngDto
import com.majidbahmani.rota.core.data.remote.google.dto.LocationRestrictionDto
import com.majidbahmani.rota.core.data.remote.google.dto.PlaceDto
import com.majidbahmani.rota.core.data.remote.google.dto.SearchNearbyRequestDto
import com.majidbahmani.rota.core.data.remote.google.dto.SearchNearbyResponseDto
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDetails

// Google place types (Places API "Table A") for our categories.
private const val TYPE_EV_CHARGER = "electric_vehicle_charging_station"
private const val TYPE_FUEL = "gas_station"
private const val TYPE_PARKING = "parking"

fun PoiCategory.toGooglePlaceType(): String = when (this) {
    PoiCategory.EV_CHARGER -> TYPE_EV_CHARGER
    PoiCategory.FUEL -> TYPE_FUEL
    PoiCategory.PARKING -> TYPE_PARKING
}

fun searchNearbyRequest(center: GeoPoint, radiusMeters: Int, categories: Set<PoiCategory>): SearchNearbyRequestDto =
    SearchNearbyRequestDto(
        includedTypes = PoiCategory.entries.filter { it in categories }.map { it.toGooglePlaceType() },
        maxResultCount = GooglePlacesApi.MAX_RESULT_COUNT,
        locationRestriction = LocationRestrictionDto(
            circle = CircleDto(
                center = LatLngDto(center.latitude, center.longitude),
                radius = radiusMeters.toDouble(),
            ),
        ),
        rankPreference = GooglePlacesApi.RANK_BY_DISTANCE,
    )

/** Places that aren't a supported category or have no location are dropped. */
fun SearchNearbyResponseDto.toDomain(): List<Poi> = places.mapNotNull { it.toDomain() }

/**
 * Null when the place isn't a supported category or has no location. The Pro tier has no
 * connectors, fuel types or opening hours, so those stay empty.
 */
fun PlaceDto.toDomain(): Poi? {
    val category = category() ?: return null
    val location = location ?: return null
    return Poi(
        id = id,
        name = displayName?.text?.ifBlank { null },
        location = GeoPoint(location.latitude, location.longitude),
        address = formattedAddress?.ifBlank { null },
        operator = null,
        openingHours = null,
        hasFee = null,
        details = when (category) {
            PoiCategory.EV_CHARGER -> PoiDetails.Charging(connectors = emptyList(), network = null)
            PoiCategory.FUEL -> PoiDetails.Fuel(fuelTypes = emptySet())
            PoiCategory.PARKING -> PoiDetails.Parking(type = null, capacity = null)
        },
    )
}

// primaryType first: a fuel station's types can also include e.g. "convenience_store" or "car_wash".
private fun PlaceDto.category(): PoiCategory? =
    primaryType?.toCategory() ?: types.firstNotNullOfOrNull { it.toCategory() }

private fun String.toCategory(): PoiCategory? = when (this) {
    TYPE_EV_CHARGER -> PoiCategory.EV_CHARGER
    TYPE_FUEL -> PoiCategory.FUEL
    TYPE_PARKING -> PoiCategory.PARKING
    else -> null
}
