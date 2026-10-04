package com.majidbahmani.rota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.rota.core.domain.config.SearchConfig
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.usecase.GetNearbyPoisUseCase
import com.majidbahmani.rota.core.domain.usecase.ObservePoiDataSourceUseCase
import com.majidbahmani.rota.viewmodel.NearbyMapUiState.ErrorReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class NearbyMapViewModel @Inject constructor(
    private val getNearbyPois: GetNearbyPoisUseCase,
    observePoiDataSource: ObservePoiDataSourceUseCase,
) : ViewModel() {

    private val center = SearchConfig.DEFAULT_CENTER

    // Inputs from the UI, so the ViewModel owns them.
    private val selectedCategory = MutableStateFlow(PoiCategory.EV_CHARGER)
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Screen-only state (no search input); kept here instead of in Compose `remember`. */
    private data class ScreenState(
        val selectedPlaceId: String? = null,
        val panelExpanded: Boolean = true,
        val mapLayoutVersion: Int = 0,
        val showDataSourceDialog: Boolean = false,
    )

    private val screenState = MutableStateFlow(ScreenState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchState: Flow<NearbyMapUiState> = combine(
        selectedCategory,
        // A different source in the settings searches again.
        observePoiDataSource(),
        retryTrigger.onStart { emit(Unit) },
    ) { category, dataSource, _ -> category to dataSource }
        // A new category, source or retry cancels the search still running.
        .flatMapLatest { (category, dataSource) ->
            flow {
                val state = NearbyMapUiState(category, center, dataSource)
                emit(state.copy(isLoading = true))
                emit(
                    getNearbyPois(center, SearchConfig.radiusMeters(category), setOf(category)).fold(
                        onSuccess = { state.copy(places = it) },
                        onFailure = { state.copy(error = it.toErrorReason()) },
                    )
                )
            }
        }

    // Screen state is combined after the search, so it never starts a new search.
    val uiState: StateFlow<NearbyMapUiState> = combine(searchState, screenState) { search, screen ->
        search.copy(
            // A selection only counts while its place is in the current results.
            selectedPlaceId = screen.selectedPlaceId?.takeIf { id -> search.places.any { it.poi.id == id } },
            panelExpanded = screen.panelExpanded,
            mapLayoutVersion = screen.mapLayoutVersion,
            showDataSourceDialog = screen.showDataSourceDialog,
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            NearbyMapUiState(selectedCategory.value, center, isLoading = true),
        )

    fun onCategorySelected(category: PoiCategory) {
        screenState.update { it.copy(selectedPlaceId = null) }
        selectedCategory.value = category
    }

    fun retry() {
        screenState.update { it.copy(selectedPlaceId = null) }
        retryTrigger.tryEmit(Unit)
    }

    /** From the list or the map; the other one follows. */
    fun onPlaceSelected(placeId: String) {
        screenState.update { it.copy(selectedPlaceId = placeId) }
    }

    fun onTogglePanel() {
        screenState.update { it.copy(panelExpanded = !it.panelExpanded) }
    }

    /** Called when the panel animation ends: the map now has its new size. */
    fun onPanelAnimationFinished() {
        screenState.update { it.copy(mapLayoutVersion = it.mapLayoutVersion + 1) }
    }

    fun onChangeDataSource() {
        screenState.update { it.copy(showDataSourceDialog = true) }
    }

    fun onDataSourceDialogDismissed() {
        screenState.update { it.copy(showDataSourceDialog = false) }
    }

    // Timeouts and a missing network are IOExceptions; anything else is a server-side problem.
    private fun Throwable.toErrorReason(): ErrorReason =
        if (this is IOException) ErrorReason.NO_CONNECTION else ErrorReason.SERVICE
}
