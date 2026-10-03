package com.majidbahmani.rota.core.data.repository

import com.majidbahmani.rota.core.data.fake.FakeOverpassApi
import com.majidbahmani.rota.core.data.remote.OverpassServerException
import com.majidbahmani.rota.core.data.remote.dto.OverpassElementDto
import com.majidbahmani.rota.core.data.remote.dto.OverpassResponseDto
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.PoiCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PoiRepositoryImplTest {

    private val api = FakeOverpassApi()
    private val repository = PoiRepositoryImpl(api)
    private val lisbon = GeoPoint(38.7223, -9.1393)

    private suspend fun search(
        categories: Set<PoiCategory> = setOf(PoiCategory.FUEL),
        center: GeoPoint = lisbon,
        radiusMeters: Int = 1000,
    ) = repository.getNearbyPois(center, radiusMeters, categories)

    @Test
    fun `success maps elements to domain`() = runTest {
        api.response = OverpassResponseDto(
            elements = listOf(
                OverpassElementDto(type = "node", id = 1, lat = 1.0, lon = 2.0, tags = mapOf("amenity" to "fuel")),
                OverpassElementDto(type = "node", id = 2, lat = 1.0, lon = 2.0, tags = mapOf("amenity" to "cafe")),
            )
        )

        val pois = search().getOrThrow()

        assertEquals(listOf("node/1"), pois.map { it.id })
        assertEquals(1, api.queries.size)
    }

    @Test
    fun `remark is reported as failure`() = runTest {
        api.response = OverpassResponseDto(remark = "runtime error: Query timed out")

        val error = search().exceptionOrNull()

        assertTrue(error is OverpassServerException)
        assertEquals("runtime error: Query timed out", error?.message)
    }

    @Test
    fun `network error is reported as failure`() = runTest {
        api.error = IOException("offline")

        assertTrue(search().exceptionOrNull() is IOException)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation is rethrown, not wrapped`() = runTest {
        api.error = CancellationException("cancelled")

        search()
    }

    @Test
    fun `empty categories return empty list without a request`() = runTest {
        val pois = search(categories = emptySet()).getOrThrow()

        assertEquals(emptyList<Any>(), pois)
        assertEquals(emptyList<String>(), api.queries)
    }

    @Test
    fun `query for all categories`() = runTest {
        search(categories = PoiCategory.entries.toSet(), radiusMeters = 1500)

        assertEquals(
            """
            [out:json][timeout:25];
            (
              nwr["amenity"="charging_station"](around:1500,38.722300,-9.139300);
              nwr["amenity"="fuel"](around:1500,38.722300,-9.139300);
              nwr["amenity"="parking"]["parking"!~"^(street_side|lane|on_kerb|half_on_kerb|layby|garage_boxes|sheds)${'$'}"]["access"!~"^(private|no|permit)${'$'}"](around:1500,38.722300,-9.139300);
            );
            out center tags;
            """.trimIndent(),
            api.queries.single(),
        )
    }

    @Test
    fun `query contains only the requested categories`() = runTest {
        search(categories = setOf(PoiCategory.FUEL))

        val query = api.queries.single()
        assertTrue(query.contains("""["amenity"="fuel"]"""))
        assertFalse(query.contains("charging_station"))
        assertFalse(query.contains("parking"))
    }

    @Test
    fun `coordinates near zero are not in scientific notation`() = runTest {
        search(center = GeoPoint(0.00001, -0.00001), radiusMeters = 100)

        assertTrue(api.queries.single().contains("(around:100,0.000010,-0.000010)"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `radius must be positive`() = runTest {
        search(radiusMeters = 0)
    }
}
