package com.majidbahmani.rota.core.domain.usecase

import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * The POI source to use: the saved choice if it's available on this build, otherwise Google
 * Places when available (it has a key), otherwise Overpass. A saved choice can become
 * unavailable, e.g. Google after the key was removed.
 */
class ObservePoiDataSourceUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<PoiDataSource> {
        val available = repository.availablePoiDataSources
        return repository.savedPoiDataSource
            .map { saved -> saved?.takeIf { it in available } ?: defaultFor(available) }
            .distinctUntilChanged()
    }

    fun available(): Set<PoiDataSource> = repository.availablePoiDataSources

    private fun defaultFor(available: Set<PoiDataSource>): PoiDataSource =
        if (PoiDataSource.GOOGLE_PLACES in available) PoiDataSource.GOOGLE_PLACES else PoiDataSource.OVERPASS
}
