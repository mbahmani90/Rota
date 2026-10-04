package com.majidbahmani.rota.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.majidbahmani.rota.R
import com.majidbahmani.rota.core.domain.model.PoiDataSource
import com.majidbahmani.rota.viewmodel.SettingsUiState
import com.majidbahmani.rota.viewmodel.SettingsViewModel

@Composable
fun DataSourceDialogRoute(onDismiss: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DataSourceDialog(
        uiState = uiState,
        onDataSourceSelect = viewModel::onDataSourceSelected,
        onDismiss = onDismiss,
    )
}

@Composable
fun DataSourceDialog(uiState: SettingsUiState, onDataSourceSelect: (PoiDataSource) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_data_source)) },
        text = {
            Column(Modifier.selectableGroup()) {
                PoiDataSource.entries.forEach { source ->
                    val enabled = source in uiState.available
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = source == uiState.selected,
                                enabled = enabled,
                                role = Role.RadioButton,
                                onClick = { onDataSourceSelect(source) },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // The row handles clicks, so the whole row is the touch target.
                        RadioButton(selected = source == uiState.selected, onClick = null, enabled = enabled)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(source.title(), style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (enabled) {
                                    source.description()
                                } else {
                                    stringResource(
                                        R.string.data_source_needs_key,
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        },
    )
}

@Composable
fun PoiDataSource.title(): String = stringResource(
    when (this) {
        PoiDataSource.OVERPASS -> R.string.data_source_overpass
        PoiDataSource.GOOGLE_PLACES -> R.string.data_source_google_places
    },
)

@Composable
private fun PoiDataSource.description(): String = stringResource(
    when (this) {
        PoiDataSource.OVERPASS -> R.string.data_source_overpass_description
        PoiDataSource.GOOGLE_PLACES -> R.string.data_source_google_places_description
    },
)
