package com.majidbahmani.rota.shared.ui.nearby

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.constraints.ConstraintManager
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarLocation
import androidx.car.app.model.ItemList
import androidx.car.app.model.Metadata
import androidx.car.app.model.Place
import androidx.car.app.model.PlaceListMapTemplate
import androidx.car.app.model.PlaceMarker
import androidx.car.app.model.Row
import androidx.car.app.model.Template
import androidx.car.app.versioning.CarAppApiLevels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.NearbyPoi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.usecase.GetNearbyPoisUseCase
import com.majidbahmani.rota.shared.R
import com.majidbahmani.rota.shared.SearchConfig
import com.majidbahmani.rota.shared.presenter.nearby.NearbyPlacesStateHolder
import com.majidbahmani.rota.shared.presenter.nearby.NearbyPlacesUiState
import com.majidbahmani.rota.shared.presenter.nearby.NearbyPlacesUiState.ErrorReason
import com.majidbahmani.rota.shared.ui.common.displayName
import com.majidbahmani.rota.shared.ui.common.distanceText
import com.majidbahmani.rota.shared.ui.common.title
import kotlinx.coroutines.launch

/** Nearest places of one [category] on the host's map; renders [NearbyPlacesUiState]. */
class NearbyPlacesScreen(
    carContext: CarContext,
    getNearbyPois: GetNearbyPoisUseCase,
    private val category: PoiCategory,
    private val center: GeoPoint = SearchConfig.DEFAULT_CENTER,
) : Screen(carContext) {

    private val stateHolder = NearbyPlacesStateHolder(
        getNearbyPois = getNearbyPois,
        scope = lifecycleScope,
        center = center,
        category = category,
        radiusMeters = SearchConfig.radiusMeters(category),
    )

    // Hosts limit how often a screen may refresh, so only invalidate when the state changed.
    private var renderedState: NearbyPlacesUiState? = null

    init {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateHolder.uiState.collect { state ->
                    if (state != renderedState) invalidate()
                }
            }
        }
    }

    override fun onGetTemplate(): Template {
        val state = stateHolder.uiState.value
        renderedState = state

        val template = PlaceListMapTemplate.Builder()
            .setTitle(category.title(carContext))
            .setHeaderAction(Action.BACK)
            .setAnchor(Place.Builder(center.toCarLocation()).build())

        // Errors stay in this template: switching template types would count as a new step.
        when (state) {
            NearbyPlacesUiState.Loading -> template.setLoading(true)
            is NearbyPlacesUiState.Success -> template.setItemList(placeList(state.places))
            is NearbyPlacesUiState.Error -> template
                .setItemList(ItemList.Builder().setNoItemsMessage(state.reason.message()).build())
                .setActionStrip(retryActionStrip())
        }
        return template.build()
    }

    private fun placeList(places: List<NearbyPoi>): ItemList {
        val list = ItemList.Builder().setNoItemsMessage(carContext.getString(R.string.nearby_empty))
        // The host decides how many places fit; they're nearest first, so the closest are kept.
        places.take(placeListLimit()).forEach { list.addItem(it.toRow()) }
        return list.build()
    }

    private fun NearbyPoi.toRow(): Row = Row.Builder()
        .setTitle(poi.displayName(carContext))
        .addText(distanceText(distanceMeters, suffix = poi.address ?: poi.operator))
        .setMetadata(
            Metadata.Builder()
                .setPlace(
                    Place.Builder(poi.location.toCarLocation())
                        .setMarker(PlaceMarker.Builder().build())
                        .build()
                )
                .build()
        )
        .build()

    private fun retryActionStrip(): ActionStrip = ActionStrip.Builder()
        .addAction(
            Action.Builder()
                .setTitle(carContext.getString(R.string.action_retry))
                .setOnClickListener { stateHolder.retry() }
                .build()
        )
        .build()

    private fun placeListLimit(): Int =
        if (carContext.carAppApiLevel >= CarAppApiLevels.LEVEL_2) {
            carContext.getCarService(ConstraintManager::class.java)
                .getContentLimit(ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST)
        } else {
            DEFAULT_PLACE_LIST_LIMIT
        }

    private fun ErrorReason.message(): String = carContext.getString(
        when (this) {
            ErrorReason.NO_CONNECTION -> R.string.error_no_connection
            ErrorReason.SERVICE -> R.string.error_service
        }
    )

    private companion object {
        const val DEFAULT_PLACE_LIST_LIMIT = 6
    }
}

private fun GeoPoint.toCarLocation(): CarLocation = CarLocation.create(latitude, longitude)
