package com.majidbahmani.rota.core.data.mapper

import com.majidbahmani.rota.core.data.remote.google.dto.LatLngDto
import com.majidbahmani.rota.core.data.remote.google.dto.LocalizedTextDto
import com.majidbahmani.rota.core.data.remote.google.dto.PlaceDto
import com.majidbahmani.rota.core.data.remote.google.dto.SearchNearbyResponseDto
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDetails
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GooglePlacesMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val pois: List<Poi> by lazy {
        val text = javaClass.getResource("/google/search_nearby_documented_shape.json")!!.readText()
        json.decodeFromString(SearchNearbyResponseDto.serializer(), text).toDomain()
    }

    private fun poi(id: String) = pois.single { it.id == id }

    private fun place(
        primaryType: String? = "gas_station",
        types: List<String> = emptyList(),
        location: LatLngDto? = LatLngDto(1.0, 2.0),
        name: String? = "Name",
    ) = PlaceDto(
        id = "id",
        displayName = name?.let { LocalizedTextDto(it) },
        location = location,
        primaryType = primaryType,
        types = types,
    )

    @Test
    fun `sample maps supported places and drops the cafe`() {
        assertEquals(listOf("test-charger-1", "test-fuel-1", "test-parking-1"), pois.map { it.id })
        assertEquals(
            listOf(PoiCategory.EV_CHARGER, PoiCategory.FUEL, PoiCategory.PARKING),
            pois.map { it.category },
        )
    }

    @Test
    fun `charger maps the pro fields and leaves the rest empty`() {
        assertEquals(
            Poi(
                id = "test-charger-1",
                name = "Powerdot Charging Station",
                location = GeoPoint(38.7222735, -9.1288602),
                address = "Av. Gen. Roçadas, 1170-166 Lisboa, Portugal",
                operator = null,
                openingHours = null,
                hasFee = null,
                details = PoiDetails.Charging(connectors = emptyList(), network = null),
            ),
            poi("test-charger-1"),
        )
    }

    @Test
    fun `category falls back to types when the primary type is another kind of place`() {
        assertEquals(PoiCategory.FUEL, poi("test-fuel-1").category)
    }

    @Test
    fun `category comes from types when there is no primary type`() {
        assertEquals(PoiDetails.Parking(type = null, capacity = null), poi("test-parking-1").details)
    }

    @Test
    fun `primary type wins over types`() {
        val poi = place(primaryType = "parking", types = listOf("gas_station", "parking")).toDomain()

        assertEquals(PoiCategory.PARKING, poi?.category)
    }

    @Test
    fun `place without location is dropped`() {
        assertNull(place(location = null).toDomain())
    }

    @Test
    fun `blank or missing name is null`() {
        assertNull(place(name = " ").toDomain()?.name)
        assertNull(place(name = null).toDomain()?.name)
    }

    @Test
    fun `request contains only the requested categories in a stable order`() {
        val request = searchNearbyRequest(
            center = GeoPoint(38.7223, -9.1393),
            radiusMeters = 2000,
            categories = setOf(PoiCategory.PARKING, PoiCategory.EV_CHARGER),
        )

        assertEquals(listOf("electric_vehicle_charging_station", "parking"), request.includedTypes)
        assertEquals(2000.0, request.locationRestriction.circle.radius, 0.0)
    }
}
