package com.majidbahmani.rota.core.data.mapper

import com.majidbahmani.rota.core.data.remote.dto.OverpassCenterDto
import com.majidbahmani.rota.core.data.remote.dto.OverpassElementDto
import com.majidbahmani.rota.core.data.remote.dto.OverpassResponseDto
import com.majidbahmani.rota.core.domain.model.ChargingConnector
import com.majidbahmani.rota.core.domain.model.ConnectorType
import com.majidbahmani.rota.core.domain.model.FuelType
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.ParkingType
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.model.PoiDetails
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverpassMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val pois: List<Poi> by lazy {
        val text = javaClass.getResource("/overpass/lisbon_sample.json")!!.readText()
        json.decodeFromString(OverpassResponseDto.serializer(), text).toDomain()
    }

    private fun poi(id: String) = pois.single { it.id == id }

    private fun element(
        tags: Map<String, String>,
        lat: Double? = 38.7,
        lon: Double? = -9.1,
        center: OverpassCenterDto? = null,
    ) = OverpassElementDto(type = "node", id = 1, lat = lat, lon = lon, center = center, tags = tags)

    @Test
    fun `lisbon sample maps every element`() {
        assertEquals(8, pois.size)
        assertEquals(
            mapOf(PoiCategory.EV_CHARGER to 2, PoiCategory.FUEL to 2, PoiCategory.PARKING to 4),
            pois.groupingBy { it.category }.eachCount(),
        )
    }

    @Test
    fun `charger maps connectors, network and common fields`() {
        val charger = poi("node/3706318964")

        assertEquals("LSB-90129", charger.name)
        assertEquals(GeoPoint(38.7222735, -9.1288602), charger.location)
        assertEquals("Avenida General Roçadas, Lisboa", charger.address)
        assertEquals("Powerdot", charger.operator)
        assertEquals("24/7", charger.openingHours)
        assertEquals(true, charger.hasFee)
        assertEquals(
            PoiDetails.Charging(
                connectors = listOf(ChargingConnector(ConnectorType.TYPE_2, count = 2, maxPowerKw = 22.0)),
                network = "Mobi.E",
            ),
            charger.details,
        )
    }

    @Test
    fun `fuel station maps fuel types`() {
        val fuel = poi("node/1828521236")

        assertEquals(
            PoiDetails.Fuel(setOf(FuelType.DIESEL, FuelType.OCTANE_95, FuelType.OCTANE_98)),
            fuel.details,
        )
    }

    @Test
    fun `fuel station without fuel tags has no fuel types`() {
        assertEquals(PoiDetails.Fuel(emptySet()), poi("node/610554965").details)
    }

    @Test
    fun `relation uses its center and maps parking details`() {
        val parking = poi("relation/16862461")

        assertEquals("Saldanha", parking.name)
        assertEquals("Empark", parking.operator)
        assertEquals(PoiDetails.Parking(ParkingType.UNDERGROUND, capacity = 495), parking.details)
    }

    @Test
    fun `name falls back to operator, then null`() {
        assertEquals("Fitness Hut", poi("way/98824283").name)
        assertNull(poi("node/2448412798").name)
    }

    @Test
    fun `street side parking is mapped as street side`() {
        assertEquals(
            PoiDetails.Parking(ParkingType.STREET_SIDE, capacity = null),
            poi("way/168879905").details,
        )
    }

    @Test
    fun `unsupported amenity is dropped`() {
        assertNull(element(mapOf("amenity" to "cafe")).toDomain())
    }

    @Test
    fun `element without coordinates is dropped`() {
        assertNull(element(mapOf("amenity" to "fuel"), lat = null, lon = null).toDomain())
    }

    @Test
    fun `way uses center when lat and lon are missing`() {
        val poi = element(mapOf("amenity" to "parking"), lat = null, lon = null, center = OverpassCenterDto(1.0, 2.0))
            .toDomain()

        assertEquals(GeoPoint(1.0, 2.0), poi?.location)
    }

    @Test
    fun `connectors ignore detail keys, unavailable sockets and map types`() {
        val details = element(
            mapOf(
                "amenity" to "charging_station",
                "socket:type2_combo" to "2",
                "socket:type2_combo:output" to "150 kW",
                "socket:type2_combo:current" to "200",
                "socket:chademo" to "yes",
                "socket:schuko" to "no",
                "socket:tesla_supercharger" to "1",
            )
        ).toDomain()?.details as PoiDetails.Charging

        assertEquals(
            listOf(
                ChargingConnector(ConnectorType.CCS, count = 2, maxPowerKw = 150.0),
                ChargingConnector(ConnectorType.CHADEMO, count = null, maxPowerKw = null),
                ChargingConnector(ConnectorType.TESLA, count = 1, maxPowerKw = null),
            ),
            details.connectors,
        )
    }

    @Test
    fun `power parses units, decimals and multiple values`() {
        assertEquals(22.0, "22 kW".toMaxPowerKw())
        assertEquals(7.4, "7400 W".toMaxPowerKw())
        assertEquals(3.7, "3,7 kW".toMaxPowerKw())
        assertEquals(22.0, "11 kW;22 kW".toMaxPowerKw())
        assertEquals(50.0, "50".toMaxPowerKw())
        assertNull("unknown".toMaxPowerKw())
    }

    @Test
    fun `unknown fee value is null`() {
        assertNull(element(mapOf("amenity" to "parking", "fee" to "interval")).toDomain()?.hasFee)
    }
}
