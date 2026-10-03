package com.majidbahmani.rota.core.domain.model

/** A [poi] with its straight-line distance from the search center. */
data class NearbyPoi(
    val poi: Poi,
    val distanceMeters: Double,
)
