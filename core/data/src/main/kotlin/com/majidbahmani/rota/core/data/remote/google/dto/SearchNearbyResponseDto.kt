package com.majidbahmani.rota.core.data.remote.google.dto

import kotlinx.serialization.Serializable

/** No results may come back as `{}` (empty lists are left out), hence the default. */
@Serializable
data class SearchNearbyResponseDto(
    val places: List<PlaceDto> = emptyList(),
)
