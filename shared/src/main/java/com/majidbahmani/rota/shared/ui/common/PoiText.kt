package com.majidbahmani.rota.shared.ui.common

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import androidx.car.app.model.Distance
import androidx.car.app.model.DistanceSpan
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.shared.R
import kotlin.math.roundToInt

fun PoiCategory.title(context: Context): String = context.getString(
    when (this) {
        PoiCategory.EV_CHARGER -> R.string.category_ev_charger
        PoiCategory.FUEL -> R.string.category_fuel
        PoiCategory.PARKING -> R.string.category_parking
    },
)

/** The POI's name, or its category when the source has none. */
fun Poi.displayName(context: Context): String = name ?: context.getString(
    when (category) {
        PoiCategory.EV_CHARGER -> R.string.category_ev_charger_single
        PoiCategory.FUEL -> R.string.category_fuel_single
        PoiCategory.PARKING -> R.string.category_parking_single
    },
)

/**
 * The distance, optionally followed by " · [suffix]". A [DistanceSpan] lets the host format it
 * for the driver's units and locale.
 */
fun distanceText(distanceMeters: Double, suffix: String? = null): CharSequence {
    val text = SpannableString(if (suffix.isNullOrBlank()) " " else "  · $suffix")
    text.setSpan(DistanceSpan.create(distance(distanceMeters)), 0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    return text
}

private fun distance(meters: Double): Distance = if (meters < 1_000) {
    // Nearest 10 m: finer would suggest more precision than GPS has.
    Distance.create((meters / 10).roundToInt() * 10.0, Distance.UNIT_METERS)
} else {
    Distance.create(meters / 1_000, Distance.UNIT_KILOMETERS_P1)
}
