package com.majidbahmani.rota.core.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.majidbahmani.rota.core.data.remote.google.GooglePlacesConfig
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Runs against a real DataStore file in a temporary folder. */
class DataStoreSettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "settings.preferences_pb") }

    private fun TestScope.dataStore() =
        PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })

    private fun TestScope.repository(apiKey: String) =
        DataStoreSettingsRepository(dataStore(), GooglePlacesConfig(apiKey))

    @Test
    fun `available sources depend on the key`() = runTest {
        assertEquals(setOf(PoiDataSource.OVERPASS), repository(apiKey = "").availablePoiDataSources)
        assertEquals(PoiDataSource.entries.toSet(), repository(apiKey = "test-key").availablePoiDataSources)
    }

    @Test
    fun `nothing saved is null`() = runTest {
        assertNull(repository(apiKey = "").savedPoiDataSource.first())
    }

    @Test
    fun `saved choice is read back as stored`() = runTest {
        val repository = repository(apiKey = "")

        // Storage doesn't apply rules: the use case decides whether Google can be used.
        repository.setPoiDataSource(PoiDataSource.GOOGLE_PLACES)

        assertEquals(PoiDataSource.GOOGLE_PLACES, repository.savedPoiDataSource.first())
    }

    @Test
    fun `unknown saved value is null`() = runTest {
        val dataStore = dataStore()
        dataStore.edit { it[stringPreferencesKey("poi_data_source")] = "REMOVED_SOURCE" }

        val repository = DataStoreSettingsRepository(dataStore, GooglePlacesConfig(apiKey = ""))

        assertNull(repository.savedPoiDataSource.first())
    }
}
