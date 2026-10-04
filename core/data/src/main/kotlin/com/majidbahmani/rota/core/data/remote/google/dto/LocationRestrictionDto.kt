package com.majidbahmani.rota.core.data.remote.google.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationRestrictionDto(
    val circle: CircleDto,
)
