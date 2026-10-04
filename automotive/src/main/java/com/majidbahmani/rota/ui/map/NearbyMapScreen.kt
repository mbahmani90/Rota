package com.majidbahmani.rota.ui.map

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.rota.R
import com.majidbahmani.rota.core.domain.model.NearbyPoi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.ui.settings.DataSourceDialogRoute
import com.majidbahmani.rota.ui.settings.title
import com.majidbahmani.rota.viewmodel.NearbyMapUiState
import com.majidbahmani.rota.viewmodel.NearbyMapUiState.ErrorReason
import com.majidbahmani.rota.viewmodel.NearbyMapViewModel

@Composable
fun NearbyMapRoute(
    modifier: Modifier = Modifier,
    viewModel: NearbyMapViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    NearbyMapScreen(
        uiState = uiState,
        onCategorySelected = viewModel::onCategorySelected,
        onRetry = viewModel::retry,
        onPlaceSelected = viewModel::onPlaceSelected,
        onChangeDataSource = viewModel::onChangeDataSource,
        onTogglePanel = viewModel::onTogglePanel,
        onPanelAnimationFinished = viewModel::onPanelAnimationFinished,
        modifier = modifier,
    )
    if (uiState.showDataSourceDialog) {
        DataSourceDialogRoute(onDismiss = viewModel::onDataSourceDialogDismissed)
    }
}

@Composable
fun NearbyMapScreen(
    uiState: NearbyMapUiState,
    onCategorySelected: (PoiCategory) -> Unit,
    onRetry: () -> Unit,
    onPlaceSelected: (String) -> Unit,
    onChangeDataSource: () -> Unit,
    onTogglePanel: () -> Unit,
    onPanelAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapController = rememberPlacesMapController()
    val placeNames = uiState.places.associate { it.poi.id to it.poi.displayName() }
    val panelWidth by animateDpAsState(
        targetValue = if (uiState.panelExpanded) PANEL_WIDTH else 0.dp,
        label = "panelWidth",
        // The map has its new size only once the animation ends.
        finishedListener = { onPanelAnimationFinished() },
    )

    Row(modifier = modifier.fillMaxSize()) {
        PlacesPanel(
            uiState = uiState,
            selectedPlaceId = uiState.selectedPlaceId,
            onCategorySelected = onCategorySelected,
            onRetry = onRetry,
            onChangeDataSource = onChangeDataSource,
            onPlaceClick = { onPlaceSelected(it.poi.id) },
            // Fixed inner width, clipped by the animated outer width: content doesn't reflow while folding.
            modifier = Modifier.width(panelWidth).fillMaxHeight(),
        )
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            PlacesMap(
                controller = mapController,
                center = uiState.center,
                places = uiState.places,
                placeNames = placeNames,
                selectedPlaceId = uiState.selectedPlaceId,
                onPlaceClick = onPlaceSelected,
                layoutVersion = uiState.mapLayoutVersion,
                modifier = Modifier.fillMaxSize(),
            )
            PanelToggleButton(
                expanded = uiState.panelExpanded,
                onClick = onTogglePanel,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
            )
            ZoomButtons(
                onZoomIn = mapController::zoomIn,
                onZoomOut = mapController::zoomOut,
                modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp),
            )
        }
    }
}

@Composable
private fun PanelToggleButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(if (expanded) R.string.action_hide_list else R.string.action_show_list)
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.size(ZOOM_BUTTON_SIZE).semantics { contentDescription = description },
    ) {
        Text(if (expanded) "‹" else "›", fontSize = 30.sp)
    }
}

@Composable
private fun PlacesPanel(
    uiState: NearbyMapUiState,
    selectedPlaceId: String?,
    onCategorySelected: (PoiCategory) -> Unit,
    onRetry: () -> Unit,
    onChangeDataSource: () -> Unit,
    onPlaceClick: (NearbyPoi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface) {
        // Anchored to the start and wider than the folding Surface, which clips it.
        Column(modifier = Modifier.wrapContentWidth(Alignment.Start, unbounded = true).width(PANEL_WIDTH)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PoiCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category == uiState.category,
                        onClick = { onCategorySelected(category) },
                        label = { Text(category.title()) },
                    )
                }
            }
            DataSourceRow(uiState, onChangeDataSource)
            HorizontalDivider()
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    uiState.error != null -> ErrorContent(uiState.error, onRetry, Modifier.align(Alignment.Center))
                    uiState.places.isEmpty() -> Text(
                        text = stringResource(R.string.nearby_empty),
                        modifier = Modifier.align(Alignment.Center),
                    )
                    else -> PlaceList(uiState.places, selectedPlaceId, onPlaceClick)
                }
            }
        }
    }
}

@Composable
private fun DataSourceRow(uiState: NearbyMapUiState, onChangeDataSource: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = uiState.dataSource?.let { stringResource(R.string.data_source_in_use, it.title()) }.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TextButton(onClick = onChangeDataSource) {
            Text(stringResource(R.string.action_change))
        }
    }
}

@Composable
private fun PlaceList(
    places: List<NearbyPoi>,
    selectedPlaceId: String?,
    onPlaceClick: (NearbyPoi) -> Unit,
) {
    val listState = rememberLazyListState()
    // A place tapped on the map scrolls into view in the list.
    LaunchedEffect(selectedPlaceId) {
        val index = places.indexOfFirst { it.poi.id == selectedPlaceId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    LazyColumn(state = listState) {
        items(items = places, key = { it.poi.id }) { place ->
            val selected = place.poi.id == selectedPlaceId
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { onPlaceClick(place) }
                    // Large touch targets for use in a car.
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = place.poi.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(distanceText(place.distanceMeters), place.poi.address ?: place.poi.operator)
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun ZoomButtons(onZoomIn: () -> Unit, onZoomOut: () -> Unit, modifier: Modifier = Modifier) {
    val zoomIn = stringResource(R.string.action_zoom_in)
    val zoomOut = stringResource(R.string.action_zoom_out)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(
            onClick = onZoomIn,
            modifier = Modifier.size(ZOOM_BUTTON_SIZE).semantics { contentDescription = zoomIn },
        ) {
            Text("+", fontSize = 26.sp)
        }
        FilledTonalButton(
            onClick = onZoomOut,
            modifier = Modifier.size(ZOOM_BUTTON_SIZE).semantics { contentDescription = zoomOut },
        ) {
            Text("−", fontSize = 26.sp)
        }
    }
}

@Composable
private fun ErrorContent(reason: ErrorReason, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(
                when (reason) {
                    ErrorReason.NO_CONNECTION -> R.string.error_no_connection
                    ErrorReason.SERVICE -> R.string.error_service
                }
            )
        )
        Button(onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) {
            Text(stringResource(R.string.action_retry))
        }
    }
}

private val PANEL_WIDTH = 420.dp
private val ZOOM_BUTTON_SIZE = 64.dp
