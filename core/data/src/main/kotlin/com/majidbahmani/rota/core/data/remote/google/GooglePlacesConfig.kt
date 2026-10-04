package com.majidbahmani.rota.core.data.remote.google

/** [apiKey] comes from MAPS_API_KEY in local.properties; blank when it isn't set. */
data class GooglePlacesConfig(
    val apiKey: String,
) {
    val isAvailable: Boolean get() = apiKey.isNotBlank()
}
