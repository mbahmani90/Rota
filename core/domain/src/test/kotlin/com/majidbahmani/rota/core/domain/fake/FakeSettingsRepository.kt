package com.majidbahmani.rota.core.domain.fake

import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory settings; [availablePoiDataSources] decides which sources can be chosen. */
class FakeSettingsRepository(
    override val availablePoiDataSources: Set<PoiDataSource> = PoiDataSource.entries.toSet(),
    saved: PoiDataSource? = null,
) : SettingsRepository {

    private val saved = MutableStateFlow(saved)
    override val savedPoiDataSource: Flow<PoiDataSource?> = this.saved

    override suspend fun setPoiDataSource(source: PoiDataSource) {
        saved.value = source
    }
}
