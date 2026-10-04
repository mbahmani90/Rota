package com.majidbahmani.rota.core.data.datasource

import com.majidbahmani.rota.core.data.fake.FakeGooglePlacesApi
import com.majidbahmani.rota.core.data.remote.google.GooglePlacesApi
import com.majidbahmani.rota.core.data.remote.google.dto.LatLngDto
import com.majidbahmani.rota.core.data.remote.google.dto.LocalizedTextDto
import com.majidbahmani.rota.core.data.remote.google.dto.PlaceDto
import com.majidbahmani.rota.core.data.remote.google.dto.SearchNearbyResponseDto
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.PoiCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GooglePlacesPoiDataSourceTest {

    private val api = FakeGooglePlacesApi()
    private val dataSource = GooglePlacesPoiDataSource(api)
    private val lisbon = GeoPoint(38.7223, -9.1393)

    @Test
    fun `success maps places to domain`() = runTest {
        api.response = SearchNearbyResponseDto(
            places = listOf(
                PlaceDto(
                    id = "p1",
                    displayName = LocalizedTextDto("Galp"),
                    location = LatLngDto(38.72, -9.13),
                    primaryType = "gas_station",
                ),
            ),
        )

        val pois = dataSource.getNearbyPois(lisbon, 5_000, setOf(PoiCategory.FUEL))

        assertEquals(listOf("p1"), pois.map { it.id })
        assertEquals(listOf("Galp"), pois.map { it.name })
    }

    @Test
    fun `request uses the category type, center, radius and the pro field mask`() = runTest {
        dataSource.getNearbyPois(lisbon, 3_000, setOf(PoiCategory.EV_CHARGER))

        val request = api.requests.single()
        assertEquals(listOf("electric_vehicle_charging_station"), request.includedTypes)
        assertEquals(LatLngDto(38.7223, -9.1393), request.locationRestriction.circle.center)
        assertEquals(3_000.0, request.locationRestriction.circle.radius, 0.0)
        assertEquals(listOf(GooglePlacesApi.PRO_FIELD_MASK), api.fieldMasks)
    }

    @Test
    fun `radius above 50 km is capped`() = runTest {
        dataSource.getNearbyPois(lisbon, 80_000, setOf(PoiCategory.FUEL))

        assertEquals(50_000.0, api.requests.single().locationRestriction.circle.radius, 0.0)
    }
}
