package com.majidbahmani.rota.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoPointDistanceTest {

    @Test
    fun `same point is zero`() {
        assertEquals(0.0, GeoPoint(38.7, -9.1).distanceTo(GeoPoint(38.7, -9.1)), 0.0)
    }

    @Test
    fun `one degree of latitude is about 111 km`() {
        assertEquals(111_195.0, GeoPoint(0.0, 0.0).distanceTo(GeoPoint(1.0, 0.0)), 1.0)
    }

    @Test
    fun `lisbon to porto is about 274 km`() {
        val lisbon = GeoPoint(38.7223, -9.1393)
        val porto = GeoPoint(41.1579, -8.6291)

        assertEquals(274_296.0, lisbon.distanceTo(porto), 1.0)
    }

    @Test
    fun `distance is symmetric`() {
        val a = GeoPoint(38.7223, -9.1393)
        val b = GeoPoint(38.7330, -9.1477)

        assertEquals(a.distanceTo(b), b.distanceTo(a), 1e-9)
    }
}
