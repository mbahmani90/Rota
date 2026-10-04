package com.majidbahmani.rota.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.usecase.ObservePoiDataSourceUseCase
import com.majidbahmani.rota.core.domain.usecase.SetPoiDataSourceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    /** Null until the saved setting is read. */
    val selected: PoiDataSource? = null,
    val available: Set<PoiDataSource> = emptySet(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observePoiDataSource: ObservePoiDataSourceUseCase,
    private val setPoiDataSource: SetPoiDataSourceUseCase,
) : ViewModel() {

    private val available = observePoiDataSource.available()

    val uiState: StateFlow<SettingsUiState> = observePoiDataSource()
        .map { SettingsUiState(selected = it, available = available) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState(available = available))

    fun onDataSourceSelected(source: PoiDataSource) {
        viewModelScope.launch { setPoiDataSource(source) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
