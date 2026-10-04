package com.majidbahmani.rota.core.data.remote.google.dto

import kotlinx.serialization.Serializable

/** Only the Pro fields requested by [com.majidbahmani.rota.core.data.remote.google.GooglePlacesApi.PRO_FIELD_MASK]. */
@Serializable
data class PlaceDto(
    val id: String,
    val displayName: LocalizedTextDto? = null,
    val formattedAddress: String? = null,
    val location: LatLngDto? = null,
    val primaryType: String? = null,
    val types: List<String> = emptyList(),
)
