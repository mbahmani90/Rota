package com.majidbahmani.rota.core.domain.usecase

import com.majidbahmani.rota.core.domain.fake.FakePoiRepository
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDetails
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class GetNearbyPoisUseCaseTest {

    private val repository = FakePoiRepository()
    private val getNearbyPois = GetNearbyPoisUseCase(repository)
    private val center = GeoPoint(38.7223, -9.1393)

    private fun fuel(id: String, latitude: Double) = Poi(
        id = id,
        name = null,
        location = GeoPoint(latitude, center.longitude),
        address = null,
        operator = null,
        openingHours = null,
        hasFee = null,
        details = PoiDetails.Fuel(emptySet()),
    )

    @Test
    fun `results are sorted by distance with distances`() = runTest {
        // 0.01° of latitude ≈ 1.1 km
        repository.result = Result.success(
            listOf(fuel("far", 38.7423), fuel("near", 38.7233), fuel("middle", 38.7323))
        )

        val nearby = getNearbyPois(center, radiusMeters = 3000, categories = setOf(PoiCategory.FUEL)).getOrThrow()

        assertEquals(listOf("near", "middle", "far"), nearby.map { it.poi.id })
        assertEquals(111.2, nearby.first().distanceMeters, 0.1)
    }

    @Test
    fun `parameters are passed to the repository`() = runTest {
        val categories = setOf(PoiCategory.EV_CHARGER, PoiCategory.PARKING)

        getNearbyPois(center, radiusMeters = 1500, categories = categories)

        assertEquals(listOf(FakePoiRepository.Request(center, 1500, categories)), repository.requests)
    }

    @Test
    fun `failure is passed through`() = runTest {
        val error = IOException("offline")
        repository.result = Result.failure(error)

        val result = getNearbyPois(center, radiusMeters = 1000, categories = setOf(PoiCategory.FUEL))

        assertSame(error, result.exceptionOrNull())
    }

    @Test
    fun `empty result stays empty`() = runTest {
        val result = getNearbyPois(center, radiusMeters = 1000, categories = setOf(PoiCategory.FUEL))

        assertTrue(result.getOrThrow().isEmpty())
    }
}
