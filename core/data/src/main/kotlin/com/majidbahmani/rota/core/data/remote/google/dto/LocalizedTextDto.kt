package com.majidbahmani.rota.core.data.remote.google.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocalizedTextDto(
    val text: String,
    val languageCode: String? = null,
)
