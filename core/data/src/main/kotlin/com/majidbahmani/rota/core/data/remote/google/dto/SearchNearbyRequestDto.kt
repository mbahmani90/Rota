package com.majidbahmani.rota.core.data.remote.google.dto

import kotlinx.serialization.Serializable

/**
 * No default values on purpose: kotlinx.serialization leaves out properties that still have
 * their default value (encodeDefaults = false), so they would never reach the server.
 */
@Serializable
data class SearchNearbyRequestDto(
    val includedTypes: List<String>,
    val maxResultCount: Int,
    val locationRestriction: LocationRestrictionDto,
    val rankPreference: String,
)
