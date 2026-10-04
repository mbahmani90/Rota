package com.majidbahmani.rota.core.data.remote.google.dto

import kotlinx.serialization.Serializable

@Serializable
data class CircleDto(
    val center: LatLngDto,
    /** Meters, greater than 0 and at most 50,000. */
    val radius: Double,
)
