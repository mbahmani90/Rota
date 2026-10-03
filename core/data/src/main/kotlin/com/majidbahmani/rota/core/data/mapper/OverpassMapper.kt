package com.majidbahmani.rota.core.data.mapper

import com.majidbahmani.rota.core.data.remote.dto.OverpassElementDto
import com.majidbahmani.rota.core.data.remote.dto.OverpassResponseDto
import com.majidbahmani.rota.core.domain.model.ChargingConnector
import com.majidbahmani.rota.core.domain.model.ConnectorType
import com.majidbahmani.rota.core.domain.model.FuelType
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.ParkingType
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiDetails

/** Elements that aren't a supported category or have no coordinates are dropped. */
fun OverpassResponseDto.toDomain(): List<Poi> = elements.mapNotNull { it.toDomain() }

/** Null when the element isn't a supported category or has no coordinates. */
fun OverpassElementDto.toDomain(): Poi? {
    val details = toDetails() ?: return null
    val location = toGeoPoint() ?: return null
    return Poi(
        id = "$type/$id",
        name = tags["name"] ?: tags["brand"] ?: tags["operator"],
        location = location,
        address = toAddress(),
        operator = tags["operator"] ?: tags["brand"],
        openingHours = tags["opening_hours"],
        hasFee = when (tags["fee"]) {
            "yes" -> true
            "no" -> false
            else -> null
        },
        details = details,
    )
}

// Nodes have lat/lon; ways and relations have a center (requested with `out center`).
private fun OverpassElementDto.toGeoPoint(): GeoPoint? = when {
    lat != null && lon != null -> GeoPoint(lat, lon)
    center != null -> GeoPoint(center.lat, center.lon)
    else -> null
}

private fun OverpassElementDto.toAddress(): String? {
    val street = listOfNotNull(tags["addr:street"], tags["addr:housenumber"])
        .joinToString(" ")
        .ifBlank { null }
    return listOfNotNull(street, tags["addr:city"]).joinToString(", ").ifBlank { null }
}

private fun OverpassElementDto.toDetails(): PoiDetails? = when (tags["amenity"]) {
    "charging_station" -> PoiDetails.Charging(
        connectors = toConnectors(),
        network = tags["network"],
    )
    "fuel" -> PoiDetails.Fuel(
        fuelTypes = FUEL_TAGS.filterKeys { tags[it] == "yes" }.values.toSet(),
    )
    "parking" -> PoiDetails.Parking(
        type = tags["parking"]?.toParkingType(),
        capacity = tags["capacity"]?.toIntOrNull(),
    )
    else -> null
}

// `socket:<type>` holds the count; `socket:<type>:output` the power, e.g. "22 kW".
private fun OverpassElementDto.toConnectors(): List<ChargingConnector> =
    tags.filterKeys { it.startsWith(SOCKET_PREFIX) && it.count { c -> c == ':' } == 1 }
        .filterValues { it != "no" && it != "0" }
        .map { (key, value) ->
            val socket = key.removePrefix(SOCKET_PREFIX)
            ChargingConnector(
                type = socket.toConnectorType(),
                count = value.toIntOrNull(),
                maxPowerKw = tags["$key:output"]?.toMaxPowerKw(),
            )
        }

private fun String.toConnectorType(): ConnectorType = when {
    this == "type2" -> ConnectorType.TYPE_2
    this == "type2_cable" -> ConnectorType.TYPE_2_CABLE
    this == "type2_combo" -> ConnectorType.CCS
    this == "chademo" -> ConnectorType.CHADEMO
    this == "schuko" -> ConnectorType.SCHUKO
    startsWith("tesla") -> ConnectorType.TESLA
    else -> ConnectorType.OTHER
}

/** "22 kW" → 22.0, "7400 W" → 7.4, "11 kW;22 kW" → 22.0. A value without a unit is read as kW. */
internal fun String.toMaxPowerKw(): Double? =
    POWER_REGEX.findAll(this).mapNotNull { match ->
        val number = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return@mapNotNull null
        if (match.groupValues[2].equals("W", ignoreCase = true)) number / 1000 else number
    }.maxOrNull()

private fun String.toParkingType(): ParkingType = when (this) {
    "surface" -> ParkingType.SURFACE
    "underground" -> ParkingType.UNDERGROUND
    "multi-storey" -> ParkingType.MULTI_STOREY
    "street_side", "lane" -> ParkingType.STREET_SIDE
    else -> ParkingType.OTHER
}

private const val SOCKET_PREFIX = "socket:"

private val POWER_REGEX = Regex("""(\d+(?:[.,]\d+)?)\s*(kW|W)?""", RegexOption.IGNORE_CASE)

private val FUEL_TAGS = mapOf(
    "fuel:diesel" to FuelType.DIESEL,
    "fuel:octane_95" to FuelType.OCTANE_95,
    "fuel:octane_98" to FuelType.OCTANE_98,
    "fuel:lpg" to FuelType.LPG,
    "fuel:cng" to FuelType.CNG,
    "fuel:adblue" to FuelType.ADBLUE,
)
