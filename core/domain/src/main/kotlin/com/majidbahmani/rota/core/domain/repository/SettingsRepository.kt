package com.majidbahmani.rota.core.domain.repository

import com.majidbahmani.rota.core.domain.model.PoiDataSource
import kotlinx.coroutines.flow.Flow

/** Storage only; which source to use is decided by ObservePoiDataSourceUseCase. */
interface SettingsRepository {

    /** Sources that can be used on this build; Google Places only with an API key. */
    val availablePoiDataSources: Set<PoiDataSource>

    /** The saved choice as stored; null when nothing (valid) is saved. */
    val savedPoiDataSource: Flow<PoiDataSource?>

    suspend fun setPoiDataSource(source: PoiDataSource)
}
