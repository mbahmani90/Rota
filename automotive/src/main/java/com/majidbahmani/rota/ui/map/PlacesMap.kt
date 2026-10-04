package com.majidbahmani.rota.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.NearbyPoi
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Talks to the MapLibre map. A plain class, not Compose state: it keeps the latest data and
 * applies it once the map and its style are ready (the style loads asynchronously).
 */
class PlacesMapController {
    private var map: MapLibreMap? = null
    private var style: Style? = null
    private var center: GeoPoint? = null
    private var places: List<NearbyPoi> = emptyList()
    private var placeNames: Map<String, String> = emptyMap()
    private var selectedPlaceId: String? = null

    fun zoomIn() {
        map?.animateCamera(CameraUpdateFactory.zoomIn())
    }

    fun zoomOut() {
        map?.animateCamera(CameraUpdateFactory.zoomOut())
    }

    internal fun onMapReady(map: MapLibreMap) {
        this.map = map
    }

    internal fun onStyleLoaded(style: Style) {
        this.style = style
        style.addPlaceLayers()
        applyPlaces()
        applySelection()
        showAll()
    }

    /** New results: draw them and show all of them with the search centre. */
    internal fun showPlaces(center: GeoPoint, places: List<NearbyPoi>, placeNames: Map<String, String>) {
        this.center = center
        this.places = places
        this.placeNames = placeNames
        applyPlaces()
        showAll()
    }

    /** Highlight the place without changing the zoom; null clears the highlight. */
    internal fun select(placeId: String?) {
        selectedPlaceId = placeId
        applySelection()
        focusSelected()
    }

    /** The map's size changed: keep the selected place in view, otherwise show all places. */
    internal fun onSizeChanged() {
        if (!focusSelected()) showAll()
    }

    private fun applyPlaces() {
        val style = style ?: return
        center?.let {
            style.getSourceAs<GeoJsonSource>(CENTER_SOURCE)?.setGeoJson(Point.fromLngLat(it.longitude, it.latitude))
        }
        style.getSourceAs<GeoJsonSource>(PLACES_SOURCE)?.setGeoJson(places.toFeatureCollection(placeNames))
    }

    private fun applySelection() {
        style?.getLayerAs<CircleLayer>(SELECTED_LAYER)
            ?.setFilter(Expression.eq(Expression.get(PROPERTY_ID), selectedPlaceId.orEmpty()))
    }

    private fun showAll() {
        val center = center ?: return
        if (places.isEmpty()) return
        map?.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsOf(center, places), BOUNDS_PADDING_PX))
    }

    /**
     * Keeps the selected place on screen without zooming: the camera only pans when the place is
     * outside the visible area (e.g. selected from the list). Returns false when nothing is selected.
     */
    private fun focusSelected(): Boolean {
        val place = places.firstOrNull { it.poi.id == selectedPlaceId } ?: return false
        val map = map ?: return true
        val position = place.poi.location.toLatLng()
        if (!map.projection.visibleRegion.latLngBounds.contains(position)) {
            map.animateCamera(CameraUpdateFactory.newLatLng(position))
        }
        return true
    }
}

/** A controller object, not state: `remember` only keeps the same instance across recompositions. */
@Composable
fun rememberPlacesMapController(): PlacesMapController = remember { PlacesMapController() }

/**
 * MapLibre with free OpenStreetMap vector tiles: unlike the Google Maps SDK, it doesn't need
 * Google Play services, which AAOS doesn't provide for maps. Places are drawn as GeoJSON layers.
 * Stateless: all screen state comes in as parameters (from the ViewModel).
 */
@Composable
fun PlacesMap(
    controller: PlacesMapController,
    center: GeoPoint,
    places: List<NearbyPoi>,
    placeNames: Map<String, String>,
    selectedPlaceId: String?,
    onPlaceClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Change it after the map's size changed (e.g. the list folded) to re-fit the view. */
    layoutVersion: Int = 0,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnPlaceClick by rememberUpdatedState(onPlaceClick)

    // An Android View, not state; it must stay the same instance (and never go into a ViewModel).
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply { onCreate(null) }
    }

    // MapView needs the host's lifecycle; adding the observer replays events up to the current state.
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.getMapAsync { map ->
            map.uiSettings.setRotateGesturesEnabled(false)
            map.uiSettings.setTiltGesturesEnabled(false)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(center.toLatLng(), DEFAULT_ZOOM))
            controller.onMapReady(map)
            map.setStyle(Style.Builder().fromUri(STYLE_URL)) { loaded -> controller.onStyleLoaded(loaded) }
            map.addOnMapClickListener { point ->
                val screenPoint = map.projection.toScreenLocation(point)
                val id = map.queryRenderedFeatures(screenPoint, PLACES_LAYER)
                    .firstOrNull()
                    ?.getStringProperty(PROPERTY_ID)
                id?.let(currentOnPlaceClick)
                id != null
            }
        }
    }

    // Each effect forwards one kind of change to the controller.
    LaunchedEffect(center, places, placeNames) { controller.showPlaces(center, places, placeNames) }
    LaunchedEffect(selectedPlaceId) { controller.select(selectedPlaceId) }
    LaunchedEffect(layoutVersion) { if (layoutVersion > 0) controller.onSizeChanged() }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun Style.addPlaceLayers() {
    addSource(GeoJsonSource(CENTER_SOURCE))
    addSource(GeoJsonSource(PLACES_SOURCE))
    addLayer(
        CircleLayer(CENTER_LAYER, CENTER_SOURCE).withProperties(
            circleRadius(9f),
            circleColor("#2196F3"),
            circleStrokeWidth(3f),
            circleStrokeColor("#FFFFFF"),
        ),
    )
    addLayer(
        CircleLayer(PLACES_LAYER, PLACES_SOURCE).withProperties(
            circleRadius(10f),
            circleColor("#FF7043"),
            circleStrokeWidth(2f),
            circleStrokeColor("#FFFFFF"),
        ),
    )
    addLayer(
        CircleLayer(SELECTED_LAYER, PLACES_SOURCE)
            .withFilter(Expression.eq(Expression.get(PROPERTY_ID), ""))
            .withProperties(
                circleRadius(15f),
                circleColor("#FFCA28"),
                circleStrokeWidth(3f),
                circleStrokeColor("#FFFFFF"),
            ),
    )
    addLayer(
        SymbolLayer(LABELS_LAYER, PLACES_SOURCE).withProperties(
            textField(Expression.get(PROPERTY_NAME)),
            // The style's font: labels need glyphs the tile server actually has.
            textFont(arrayOf("Noto Sans Regular")),
            textSize(14f),
            textColor("#FFFFFF"),
            textHaloColor("#000000"),
            textHaloWidth(1.5f),
            textAnchor("top"),
            textOffset(arrayOf(0f, 1.2f)),
        ),
    )
}

private fun List<NearbyPoi>.toFeatureCollection(names: Map<String, String>): FeatureCollection =
    FeatureCollection.fromFeatures(
        map { place ->
            Feature.fromGeometry(Point.fromLngLat(place.poi.location.longitude, place.poi.location.latitude)).apply {
                addStringProperty(PROPERTY_ID, place.poi.id)
                addStringProperty(PROPERTY_NAME, names[place.poi.id].orEmpty())
            }
        },
    )

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

private fun boundsOf(center: GeoPoint, places: List<NearbyPoi>): LatLngBounds = LatLngBounds.Builder()
    .include(center.toLatLng())
    .includes(places.map { it.poi.location.toLatLng() })
    .build()

// OpenFreeMap: free OpenStreetMap vector tiles without a key; dark for the car.
private const val STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

private const val CENTER_SOURCE = "center-source"
private const val PLACES_SOURCE = "places-source"
private const val CENTER_LAYER = "center-layer"
private const val PLACES_LAYER = "places-layer"
private const val SELECTED_LAYER = "selected-place-layer"
private const val LABELS_LAYER = "place-labels-layer"
private const val PROPERTY_ID = "id"
private const val PROPERTY_NAME = "name"

private const val DEFAULT_ZOOM = 14.0
private const val BOUNDS_PADDING_PX = 120
