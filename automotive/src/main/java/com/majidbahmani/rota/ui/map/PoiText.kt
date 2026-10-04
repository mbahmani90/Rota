package com.majidbahmani.rota.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.majidbahmani.rota.R
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import kotlin.math.roundToInt

@Composable
fun PoiCategory.title(): String = stringResource(
    when (this) {
        PoiCategory.EV_CHARGER -> R.string.category_ev_charger
        PoiCategory.FUEL -> R.string.category_fuel
        PoiCategory.PARKING -> R.string.category_parking
    }
)

/** The POI's name, or its category when the source has none. */
@Composable
fun Poi.displayName(): String = name ?: stringResource(
    when (category) {
        PoiCategory.EV_CHARGER -> R.string.category_ev_charger_single
        PoiCategory.FUEL -> R.string.category_fuel_single
        PoiCategory.PARKING -> R.string.category_parking_single
    }
)

/** Under 1 km to the nearest 10 m (no false GPS precision), otherwise km with one decimal. */
@Composable
fun distanceText(meters: Double): String =
    if (meters < 1_000) {
        stringResource(R.string.distance_meters, (meters / 10).roundToInt() * 10)
    } else {
        stringResource(R.string.distance_kilometers, meters / 1_000)
    }
