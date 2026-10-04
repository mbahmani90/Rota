package com.majidbahmani.rota.core.domain.config

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.PoiCategory

object SearchConfig {

    /** Lisbon city centre; replaced by the car's location in the location step. */
    val DEFAULT_CENTER = GeoPoint(38.7223, -9.1393)

    /** Fuel stations are sparse in city centres, so they get a larger radius. */
    fun radiusMeters(category: PoiCategory): Int = when (category) {
        PoiCategory.EV_CHARGER -> 3_000
        PoiCategory.FUEL -> 5_000
        PoiCategory.PARKING -> 2_000
    }
}
