package com.majidbahmani.rota.core.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class OverpassCenterDto(
    val lat: Double,
    val lon: Double,
)
