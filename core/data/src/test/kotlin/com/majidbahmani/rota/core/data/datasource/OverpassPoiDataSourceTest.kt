package com.majidbahmani.rota.core.data.datasource

import com.majidbahmani.rota.core.data.fake.FakeOverpassApi
import com.majidbahmani.rota.core.data.remote.overpass.OverpassServerException
import com.majidbahmani.rota.core.data.remote.overpass.dto.OverpassElementDto
import com.majidbahmani.rota.core.data.remote.overpass.dto.OverpassResponseDto
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.PoiCategory
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassPoiDataSourceTest {

    private val api = FakeOverpassApi()
    private val dataSource = OverpassPoiDataSource(api)
    private val lisbon = GeoPoint(38.7223, -9.1393)

    private suspend fun search(
        categories: Set<PoiCategory> = setOf(PoiCategory.FUEL),
        center: GeoPoint = lisbon,
        radiusMeters: Int = 1000,
    ) = dataSource.getNearbyPois(center, radiusMeters, categories)

    @Test
    fun `success maps elements to domain`() = runTest {
        api.response = OverpassResponseDto(
            elements = listOf(
                OverpassElementDto(type = "node", id = 1, lat = 1.0, lon = 2.0, tags = mapOf("amenity" to "fuel")),
                OverpassElementDto(type = "node", id = 2, lat = 1.0, lon = 2.0, tags = mapOf("amenity" to "cafe")),
            ),
        )

        assertEquals(listOf("node/1"), search().map { it.id })
        assertEquals(1, api.queries.size)
    }

    @Test(expected = OverpassServerException::class)
    fun `remark throws a server exception`() = runTest {
        api.response = OverpassResponseDto(remark = "runtime error: Query timed out")

        search()
    }

    @Test(expected = IOException::class)
    fun `network error is thrown to the repository`() = runTest {
        api.error = IOException("offline")

        search()
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
}
