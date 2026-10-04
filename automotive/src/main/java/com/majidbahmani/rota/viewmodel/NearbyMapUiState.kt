package com.majidbahmani.rota.viewmodel

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.NearbyPoi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDataSource

/**
 * A data class, not a sealed Loading/Success/Error: the map and category chips stay on screen
 * in every state, only the markers and overlays change.
 */
data class NearbyMapUiState(
    val category: PoiCategory,
    val center: GeoPoint,
    /** Where these places come from; null until the setting is read. */
    val dataSource: PoiDataSource? = null,
    /** Nearest first. */
    val places: List<NearbyPoi> = emptyList(),
    val isLoading: Boolean = false,
    val error: ErrorReason? = null,
    /** Highlighted in the list and on the map; always one of [places] or null. */
    val selectedPlaceId: String? = null,
    /** Whether the list panel is open; folding it gives the map the full width. */
    val panelExpanded: Boolean = true,
    /** Increases when the map's size changed (panel folded or opened), so it re-fits the view. */
    val mapLayoutVersion: Int = 0,
    val showDataSourceDialog: Boolean = false,
) {
    /** What went wrong, not how to say it: the UI picks the text. */
    enum class ErrorReason { NO_CONNECTION, SERVICE }
}
