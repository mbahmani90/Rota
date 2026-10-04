package com.majidbahmani.rota.viewmodel

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.NearbyPoi
import com.majidbahmani.rota.core.domain.model.PoiCategory

/**
 * A data class, not a sealed Loading/Success/Error: the map and category chips stay on screen
 * in every state, only the markers and overlays change.
 */
data class NearbyMapUiState(
    val category: PoiCategory,
    val center: GeoPoint,
    /** Nearest first. */
    val places: List<NearbyPoi> = emptyList(),
    val isLoading: Boolean = false,
    val error: ErrorReason? = null,
) {
    /** What went wrong, not how to say it: the UI picks the text. */
    enum class ErrorReason { NO_CONNECTION, SERVICE }
}
