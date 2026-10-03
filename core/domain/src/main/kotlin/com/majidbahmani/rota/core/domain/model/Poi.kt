package com.majidbahmani.rota.core.domain.model

/**
 * A place shown on the map. [name] is null when the source has neither a name, brand nor
 * operator; the UI then shows the category instead.
 */
data class Poi(
    val id: String,
    val name: String?,
    val location: GeoPoint,
    val address: String?,
    val operator: String?,
    val openingHours: String?,
    val hasFee: Boolean?,
    val details: PoiDetails,
) {
    /** Derived from [details], so the two can never disagree. */
    val category: PoiCategory
        get() = when (details) {
            is PoiDetails.Charging -> PoiCategory.EV_CHARGER
            is PoiDetails.Fuel -> PoiCategory.FUEL
            is PoiDetails.Parking -> PoiCategory.PARKING
        }
}
