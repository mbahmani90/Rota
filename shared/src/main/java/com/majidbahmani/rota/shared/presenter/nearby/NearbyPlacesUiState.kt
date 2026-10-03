package com.majidbahmani.rota.shared.presenter.nearby

import com.majidbahmani.rota.core.domain.model.NearbyPoi

sealed interface NearbyPlacesUiState {
    data object Loading : NearbyPlacesUiState

    /** Nearest first; an empty list means nothing was found. */
    data class Success(val places: List<NearbyPoi>) : NearbyPlacesUiState

    data class Error(val reason: ErrorReason) : NearbyPlacesUiState

    /** What went wrong, not how to say it: the UI picks the text. */
    enum class ErrorReason { NO_CONNECTION, SERVICE }
}
