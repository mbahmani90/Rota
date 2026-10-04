package com.majidbahmani.rota.core.data.remote.overpass.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OverpassMetaDto(
    @SerialName("timestamp_osm_base") val timestampOsmBase: String? = null,
    /** ODbL attribution that the app must show. */
    val copyright: String? = null,
)
