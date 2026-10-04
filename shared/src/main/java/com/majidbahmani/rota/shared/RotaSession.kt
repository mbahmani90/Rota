package com.majidbahmani.rota.shared

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.usecase.GetNearbyPoisUseCase
import com.majidbahmani.rota.shared.ui.nearby.NearbyPlacesScreen

class RotaSession(
    private val getNearbyPois: GetNearbyPoisUseCase,
) : Session() {

    // Opens EV chargers directly until the category screen is added.
    override fun onCreateScreen(intent: Intent): Screen =
        NearbyPlacesScreen(carContext, getNearbyPois, PoiCategory.EV_CHARGER)
}
