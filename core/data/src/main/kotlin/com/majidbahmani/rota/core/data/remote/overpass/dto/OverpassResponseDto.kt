package com.majidbahmani.rota.core.data.remote.overpass.dto

import kotlinx.serialization.Serializable

/** Response of the Overpass API `interpreter` endpoint with `[out:json]`. Not paginated. */
@Serializable
data class OverpassResponseDto(
    val version: Double? = null,
    val generator: String? = null,
    val osm3s: OverpassMetaDto? = null,
    val elements: List<OverpassElementDto> = emptyList(),
    /** Set when the query failed on the server (e.g. timeout); the HTTP status can still be 200. */
    val remark: String? = null,
)
