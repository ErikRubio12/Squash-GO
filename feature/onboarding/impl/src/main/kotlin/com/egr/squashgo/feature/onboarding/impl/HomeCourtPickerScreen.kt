package com.egr.squashgo.feature.onboarding.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egr.squashgo.core.model.Court

@Composable
fun HomeCourtPickerScreen(
    onCourtsSaved: () -> Unit,
    viewModel: HomeCourtPickerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is HomeCourtPickerUiState.Saved ||
            uiState is HomeCourtPickerUiState.AlreadyComplete
        ) {
            onCourtsSaved()
        }
    }

    when (val state = uiState) {
        is HomeCourtPickerUiState.Loading,
        is HomeCourtPickerUiState.AlreadyComplete,
        is HomeCourtPickerUiState.Saved,
        -> CenteredSpinner()

        is HomeCourtPickerUiState.Error -> ErrorState(
            error = state.error,
            onRetry = viewModel::retryLoad,
        )

        is HomeCourtPickerUiState.Ready -> ReadyState(
            state = state,
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onToggleCourt = viewModel::toggleCourt,
            onSetPrimary = viewModel::setPrimary,
            onSave = viewModel::save,
        )
    }
}

@Composable
private fun CenteredSpinner() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(
    error: HomeCourtPickerError,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.home_court_retry))
            }
        }
    }
}

@Composable
private fun ReadyState(
    state: HomeCourtPickerUiState.Ready,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggleCourt: (String) -> Unit,
    onSetPrimary: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.home_court_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                R.string.home_court_subtitle,
                HomeCourtPickerViewModel.MAX_HOME_COURTS,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text(stringResource(R.string.home_court_search_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                R.string.home_court_selection_count,
                state.selectedCourtIds.size,
                HomeCourtPickerViewModel.MAX_HOME_COURTS,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.validationError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(state.validationError.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.filteredCourts.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.home_court_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(state.filteredCourts, key = { it.id }) { court ->
                    val isSelected = court.id in state.selectedCourtIds
                    val isPrimary = court.id == state.primaryCourtId
                    val reachedMax = !isSelected &&
                        state.selectedCourtIds.size >= HomeCourtPickerViewModel.MAX_HOME_COURTS
                    CourtRow(
                        court = court,
                        isSelected = isSelected,
                        isPrimary = isPrimary,
                        isDisabled = reachedMax,
                        onToggle = { onToggleCourt(court.id) },
                        onSetPrimary = { onSetPrimary(court.id) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving && state.selectedCourtIds.isNotEmpty(),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp),
                )
            } else {
                Text(stringResource(R.string.home_court_save))
            }
        }
    }
}

@Composable
private fun CourtRow(
    court: Court,
    isSelected: Boolean,
    isPrimary: Boolean,
    isDisabled: Boolean,
    onToggle: () -> Unit,
    onSetPrimary: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isDisabled,
                onClick = onToggle,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                enabled = !isDisabled,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = court.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = court.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isSelected) {
                if (isPrimary) {
                    AssistChip(
                        onClick = {},
                        label = { Text(stringResource(R.string.home_court_primary_chip)) },
                        enabled = false,
                    )
                } else {
                    TextButton(onClick = onSetPrimary) {
                        Text(stringResource(R.string.home_court_make_primary))
                    }
                }
            }
        }
    }
}

@StringRes
private fun HomeCourtPickerError.messageRes(): Int = when (this) {
    HomeCourtPickerError.MinNotMet -> R.string.home_court_error_min_not_met
    HomeCourtPickerError.Network -> R.string.home_court_error_network
    HomeCourtPickerError.NotAuthenticated -> R.string.home_court_error_not_authenticated
}
