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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
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

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NearbyMapUiState> = combine(
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
        .stateIn(
            viewModelScope,
            SharingStarted.Lazily,
            NearbyMapUiState(selectedCategory.value, center, isLoading = true),
        )

    fun onCategorySelected(category: PoiCategory) {
        selectedCategory.value = category
    }

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    // Timeouts and a missing network are IOExceptions; anything else is a server-side problem.
    private fun Throwable.toErrorReason(): ErrorReason =
        if (this is IOException) ErrorReason.NO_CONNECTION else ErrorReason.SERVICE
}
