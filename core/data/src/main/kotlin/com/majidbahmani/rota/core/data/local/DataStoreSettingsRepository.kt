package com.majidbahmani.rota.core.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.majidbahmani.rota.core.data.remote.google.GooglePlacesConfig
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/** Stores settings; the rules for using them are in the domain's use cases. */
class DataStoreSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    googlePlacesConfig: GooglePlacesConfig,
) : SettingsRepository {

    override val availablePoiDataSources: Set<PoiDataSource> =
        if (googlePlacesConfig.isAvailable) PoiDataSource.entries.toSet() else setOf(PoiDataSource.OVERPASS)

    override val savedPoiDataSource: Flow<PoiDataSource?> = dataStore.data
        // An unreadable file counts as nothing saved.
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { preferences ->
            // Unknown values (e.g. a removed source) count as nothing saved.
            preferences[POI_DATA_SOURCE]?.let { saved -> PoiDataSource.entries.firstOrNull { it.name == saved } }
        }
        .distinctUntilChanged()

    override suspend fun setPoiDataSource(source: PoiDataSource) {
        dataStore.edit { it[POI_DATA_SOURCE] = source.name }
    }

    private companion object {
        // Stored by name, so reordering the enum can't change a saved choice.
        val POI_DATA_SOURCE = stringPreferencesKey("poi_data_source")
    }
}
