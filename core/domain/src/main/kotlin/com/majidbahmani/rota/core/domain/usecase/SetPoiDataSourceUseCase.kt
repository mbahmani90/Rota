package com.majidbahmani.rota.core.domain.usecase

import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.repository.SettingsRepository
import javax.inject.Inject

/** Saves the user's choice; a source that isn't available on this build is ignored. */
class SetPoiDataSourceUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(source: PoiDataSource): Boolean {
        if (source !in repository.availablePoiDataSources) return false
        repository.setPoiDataSource(source)
        return true
    }
}
