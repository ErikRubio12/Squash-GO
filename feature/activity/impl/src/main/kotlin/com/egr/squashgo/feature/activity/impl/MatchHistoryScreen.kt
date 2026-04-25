package com.egr.squashgo.feature.activity.impl

import androidx.annotation.StringRes
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
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egr.squashgo.core.ui.locale.currentLocale
import com.egr.squashgo.feature.activity.impl.model.MatchHistoryError
import com.egr.squashgo.feature.activity.impl.model.MatchHistoryUiState
import com.egr.squashgo.feature.activity.impl.model.MatchRow
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchHistoryScreen(
    onBack: () -> Unit,
    viewModel: MatchHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val state = uiState) {
                is MatchHistoryUiState.Loading -> CenteredSpinner()
                is MatchHistoryUiState.Empty -> EmptyState()
                is MatchHistoryUiState.Error -> ErrorState(
                    error = state.kind,
                    onRetry = viewModel::refresh,
                )
                is MatchHistoryUiState.Ready -> MatchList(rows = state.rows)
            }
        }
    }
}

@Composable
private fun MatchList(rows: List<MatchRow>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(rows, key = { it.matchId }) { row ->
            MatchCard(row = row)
        }
    }
}

@Composable
private fun MatchCard(row: MatchRow) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.opponentName
                        ?: stringResource(R.string.activity_unknown_opponent),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.activity_score_format,
                        row.userGames,
                        row.opponentGames,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = relativeDateDisplay(row.playedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ResultChip(didWin = row.didWin)
        }
    }
}

@Composable
private fun ResultChip(didWin: Boolean) {
    val labelRes = if (didWin) R.string.activity_result_win else R.string.activity_result_loss
    val container = if (didWin) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.errorContainer
    }
    val content = if (didWin) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(stringResource(labelRes)) },
        colors = AssistChipDefaults.assistChipColors(
            disabledContainerColor = container,
            disabledLabelColor = content,
        ),
    )
}

@Composable
private fun relativeDateDisplay(instant: Instant?): String {
    if (instant == null) return ""
    val locale = currentLocale()
    val zone = ZoneId.systemDefault()
    val matchDate = instant.toJavaInstant().atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val days = ChronoUnit.DAYS.between(matchDate, today).toInt()
    return when {
        days <= 0 -> stringResource(R.string.activity_date_today)
        days == 1 -> stringResource(R.string.activity_date_yesterday)
        days in 2..7 -> pluralStringResource(R.plurals.activity_date_days_ago, days, days)
        else -> matchDate.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale),
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.activity_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
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
    error: MatchHistoryError,
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
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.activity_retry))
            }
        }
    }
}

@StringRes
private fun MatchHistoryError.messageRes(): Int = when (this) {
    MatchHistoryError.Network -> R.string.activity_error_generic
    MatchHistoryError.NotAuthenticated -> R.string.activity_error_not_authenticated
}
