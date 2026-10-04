package com.majidbahmani.rota.core.data.remote.google.dto

import kotlinx.serialization.Serializable

/** Used in both the request (circle center) and the response (place location). */
@Serializable
data class LatLngDto(
    val latitude: Double,
    val longitude: Double,
)
