package com.majidbahmani.rota.core.data.repository

import com.majidbahmani.rota.core.data.fake.FakePoiRemoteDataSource
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.core.domain.model.PoiDetails
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PoiRepositoryImplTest {

    private val overpass = FakePoiRemoteDataSource(pois = listOf(poi("osm")))
    private val google = FakePoiRemoteDataSource(pois = listOf(poi("google")))
    private val lisbon = GeoPoint(38.7223, -9.1393)

    private val repository = PoiRepositoryImpl(overpass, google)

    private suspend fun search(
        source: PoiDataSource = PoiDataSource.OVERPASS,
        categories: Set<PoiCategory> = setOf(PoiCategory.FUEL),
        radiusMeters: Int = 1_000,
    ) = repository.getNearbyPois(source, lisbon, radiusMeters, categories)

    private fun poi(id: String) = Poi(
        id = id,
        name = null,
        location = GeoPoint(38.72, -9.13),
        address = null,
        operator = null,
        openingHours = null,
        hasFee = null,
        details = PoiDetails.Fuel(emptySet()),
    )

    @Test
    fun `overpass routes to the overpass data source`() = runTest {
        val pois = search(PoiDataSource.OVERPASS).getOrThrow()

        assertEquals(listOf("osm"), pois.map { it.id })
        assertEquals(0, google.calls)
    }

    @Test
    fun `google places routes to the google data source`() = runTest {
        val pois = search(PoiDataSource.GOOGLE_PLACES).getOrThrow()

        assertEquals(listOf("google"), pois.map { it.id })
        assertEquals(0, overpass.calls)
    }

    @Test
    fun `empty categories return an empty list without a request`() = runTest {
        val pois = search(categories = emptySet()).getOrThrow()

        assertEquals(emptyList<Poi>(), pois)
        assertEquals(0, overpass.calls + google.calls)
    }

    @Test
    fun `data source error becomes a failure`() = runTest {
        val error = IOException("offline")
        overpass.error = error

        assertSame(error, search().exceptionOrNull())
    }

    @Test
    fun `malformed response becomes a failure, not a crash`() = runTest {
        overpass.error = SerializationException("bad json")

        assertTrue(search().exceptionOrNull() is SerializationException)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is rethrown, not wrapped`() = runTest {
        overpass.error = CancellationException("cancelled")

        search()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `radius must be positive`() = runTest {
        search(radiusMeters = 0)
    }
}
