package com.majidbahmani.rota.core.domain.model

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** Great-circle (haversine) distance in meters; straight line, not driving distance. */
fun GeoPoint.distanceTo(other: GeoPoint): Double {
    val lat1 = latitude.toRadians()
    val lat2 = other.latitude.toRadians()
    val dLat = lat2 - lat1
    val dLon = (other.longitude - longitude).toRadians()
    val h = sin(dLat / 2).let { it * it } + cos(lat1) * cos(lat2) * sin(dLon / 2).let { it * it }
    return 2 * EARTH_RADIUS_METERS * asin(sqrt(h))
}

// kotlin.math only (no java.lang.Math), so the domain stays ready for KMP.
private fun Double.toRadians(): Double = this * PI / 180
