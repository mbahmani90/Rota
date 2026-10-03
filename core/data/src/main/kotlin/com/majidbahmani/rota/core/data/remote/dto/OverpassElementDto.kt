package com.majidbahmani.rota.core.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * An OSM object. Only nodes have [lat]/[lon]; ways and relations have [center]
 * (requested with `out center`). [id] is unique per [type] only.
 */
@Serializable
data class OverpassElementDto(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val center: OverpassCenterDto? = null,
    /** Free-form OSM tags (`name`, `brand`, `socket:type2:output`, …). */
    val tags: Map<String, String> = emptyMap(),
)
