package com.majidbahmani.rota.core.data.remote.overpass.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverpassResponseDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun decode(text: String): OverpassResponseDto =
        json.decodeFromString(OverpassResponseDto.serializer(), text)

    private fun lisbonSample(): OverpassResponseDto {
        val text = javaClass.getResource("/overpass/lisbon_sample.json")!!.readText()
        return decode(text)
    }

    @Test
    fun `lisbon sample decodes all elements and metadata`() {
        val response = lisbonSample()

        assertEquals(8, response.elements.size)
        assertTrue(response.osm3s?.copyright.orEmpty().contains("openstreetmap.org"))
        assertNull(response.remark)
    }

    @Test
    fun `node has lat and lon, no center`() {
        val node = lisbonSample().elements.first { it.type == "node" }

        assertNotNull(node.lat)
        assertNotNull(node.lon)
        assertNull(node.center)
    }

    @Test
    fun `way and relation have center, no lat and lon`() {
        val areas = lisbonSample().elements.filter { it.type != "node" }

        assertTrue(areas.isNotEmpty())
        areas.forEach {
            assertNotNull(it.center)
            assertNull(it.lat)
        }
    }

    @Test
    fun `charger tags are kept as a map`() {
        val charger = lisbonSample().elements.first { it.id == 3706318964 }

        assertEquals("charging_station", charger.tags["amenity"])
        assertEquals("22 kW", charger.tags["socket:type2:output"])
    }

    @Test
    fun `element without tags and runtime error remark decode`() {
        val response = decode(
            """{"elements":[{"type":"node","id":1,"lat":1.0,"lon":2.0}],
               "remark":"runtime error: Query timed out"}"""
        )

        assertEquals(emptyMap<String, String>(), response.elements.single().tags)
        assertEquals("runtime error: Query timed out", response.remark)
    }
}
