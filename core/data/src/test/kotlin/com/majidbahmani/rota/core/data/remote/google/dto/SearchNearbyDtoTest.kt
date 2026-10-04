package com.majidbahmani.rota.core.data.remote.google.dto

import com.majidbahmani.rota.core.data.mapper.searchNearbyRequest
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.PoiCategory
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The sample follows the shape in Google's documentation; replace it with a captured response once a key is set. */
class SearchNearbyDtoTest {

    // Same configuration as NetworkModule.
    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(text: String) = json.decodeFromString(SearchNearbyResponseDto.serializer(), text)

    private fun sample(): SearchNearbyResponseDto =
        decode(javaClass.getResource("/google/search_nearby_documented_shape.json")!!.readText())

    @Test
    fun `sample decodes all places and ignores unrequested fields`() {
        val places = sample().places

        assertEquals(4, places.size)
        assertEquals("Powerdot Charging Station", places.first().displayName?.text)
        assertEquals(LatLngDto(38.7222735, -9.1288602), places.first().location)
    }

    @Test
    fun `missing optional fields decode as null or empty`() {
        val parking = sample().places.single { it.id == "test-parking-1" }

        assertNull(parking.formattedAddress)
        assertNull(parking.primaryType)
    }

    @Test
    fun `no results as an empty object decode as an empty list`() {
        assertEquals(emptyList<PlaceDto>(), decode("{}").places)
    }

    @Test
    fun `request encodes every field the API needs`() {
        val request = searchNearbyRequest(
            center = GeoPoint(38.7223, -9.1393),
            radiusMeters = 3000,
            categories = setOf(PoiCategory.EV_CHARGER),
        )

        val encoded = json.encodeToString(SearchNearbyRequestDto.serializer(), request)

        assertEquals(
            Json.parseToJsonElement(
                """
                {
                  "includedTypes": ["electric_vehicle_charging_station"],
                  "maxResultCount": 20,
                  "locationRestriction": {
                    "circle": { "center": { "latitude": 38.7223, "longitude": -9.1393 }, "radius": 3000.0 }
                  },
                  "rankPreference": "DISTANCE"
                }
                """
            ).jsonObject,
            Json.parseToJsonElement(encoded).jsonObject,
        )
    }
}
