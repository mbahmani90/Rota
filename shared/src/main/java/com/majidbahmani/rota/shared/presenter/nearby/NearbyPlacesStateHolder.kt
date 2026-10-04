package com.majidbahmani.rota.shared.presenter.nearby

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.usecase.GetNearbyPoisUseCase
import com.majidbahmani.rota.shared.presenter.nearby.NearbyPlacesUiState.ErrorReason
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * Car Screens aren't ViewModelStoreOwners, so this takes the ViewModel's role: it owns the
 * state and runs the use case in the Screen's [scope]. Plain Kotlin, so it's unit-testable.
 */
class NearbyPlacesStateHolder(
    private val getNearbyPois: GetNearbyPoisUseCase,
    scope: CoroutineScope,
    private val center: GeoPoint,
    private val category: PoiCategory,
    private val radiusMeters: Int,
) {
    // extraBufferCapacity lets retry() emit from a non-suspend function without dropping it.
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Loads on the first collector; each retry searches again. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<NearbyPlacesUiState> = retryTrigger
        .onStart { emit(Unit) }
        // A new search cancels one still running, so an old answer can't overwrite it.
        .flatMapLatest {
            flow {
                emit(NearbyPlacesUiState.Loading)
                emit(
                    getNearbyPois(center, radiusMeters, setOf(category)).fold(
                        onSuccess = { NearbyPlacesUiState.Success(it) },
                        onFailure = { NearbyPlacesUiState.Error(it.toErrorReason()) },
                    ),
                )
            }
        }
        // Lazily: keeps the result while the Screen is in the back stack (e.g. on details).
        .stateIn(scope, SharingStarted.Lazily, NearbyPlacesUiState.Loading)

    fun retry() {
        retryTrigger.tryEmit(Unit)
    }

    // Timeouts and a missing network are IOExceptions; anything else is a server-side problem.
    private fun Throwable.toErrorReason(): ErrorReason =
        if (this is IOException) ErrorReason.NO_CONNECTION else ErrorReason.SERVICE
}
