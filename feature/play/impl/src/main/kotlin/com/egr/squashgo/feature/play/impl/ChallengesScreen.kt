package com.egr.squashgo.feature.play.impl

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egr.squashgo.core.model.ChallengeStatus
import com.egr.squashgo.feature.play.impl.model.ChallengeCard
import com.egr.squashgo.feature.play.impl.model.ChallengesError
import com.egr.squashgo.feature.play.impl.model.ChallengesUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengesScreen(
    onMatchResult: (String) -> Unit,
    onMatchConfirm: (String) -> Unit,
    onIncomingTap: (String) -> Unit,
    viewModel: ChallengesViewModel = hiltViewModel(),
) {
    @Suppress("UNUSED_VARIABLE") // Reserved for future Active Matches wiring.
    val unusedMatchResult = onMatchResult
    @Suppress("UNUSED_VARIABLE")
    val unusedMatchConfirm = onMatchConfirm

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingCancel by remember { mutableStateOf<ChallengeCard?>(null) }

    val cancelErrorMessage = stringResource(R.string.challenges_cancel_error)
    LaunchedEffect(uiState) {
        val state = uiState
        if (state is ChallengesUiState.Ready && state.cancelError) {
            snackbarHostState.showSnackbar(cancelErrorMessage)
            viewModel.dismissCancelError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.challenges_title)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val state = uiState) {
                is ChallengesUiState.Loading -> CenteredSpinner()

                is ChallengesUiState.Error -> ErrorState(
                    error = state.error,
                    onRetry = viewModel::refresh,
                )

                is ChallengesUiState.Ready -> Column(Modifier.fillMaxSize()) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.challenges_tab_incoming)) },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.challenges_tab_outgoing)) },
                        )
                    }
                    when (selectedTab) {
                        0 -> IncomingList(
                            cards = state.incoming,
                            onTap = onIncomingTap,
                        )
                        else -> OutgoingList(
                            cards = state.outgoing,
                            cancellingId = state.cancellingId,
                            onCancelRequest = { pendingCancel = it },
                        )
                    }
                }
            }
        }
    }

    val cancelTarget = pendingCancel
    if (cancelTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = { Text(stringResource(R.string.challenges_cancel_confirm_title)) },
            text = { Text(stringResource(R.string.challenges_cancel_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cancelChallenge(cancelTarget.challenge.id)
                    pendingCancel = null
                }) {
                    Text(stringResource(R.string.challenges_cancel_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = null }) {
                    Text(stringResource(R.string.challenges_cancel_confirm_no))
                }
            },
        )
    }
}

@Composable
private fun IncomingList(
    cards: List<ChallengeCard>,
    onTap: (String) -> Unit,
) {
    if (cards.isEmpty()) {
        EmptyState(messageRes = R.string.challenges_empty_incoming)
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(cards, key = { it.challenge.id }) { card ->
            ChallengeRow(
                card = card,
                onClick = { onTap(card.challenge.id) },
                trailing = null,
            )
        }
    }
}

@Composable
private fun OutgoingList(
    cards: List<ChallengeCard>,
    cancellingId: String?,
    onCancelRequest: (ChallengeCard) -> Unit,
) {
    if (cards.isEmpty()) {
        EmptyState(messageRes = R.string.challenges_empty_outgoing)
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(cards, key = { it.challenge.id }) { card ->
            ChallengeRow(
                card = card,
                onClick = null,
                trailing = {
                    when (card.challenge.status) {
                        ChallengeStatus.PENDING -> {
                            val isCancelling = cancellingId == card.challenge.id
                            TextButton(
                                onClick = { onCancelRequest(card) },
                                enabled = !isCancelling,
                            ) {
                                if (isCancelling) {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.height(16.dp),
                                    )
                                } else {
                                    Text(stringResource(R.string.challenges_cancel))
                                }
                            }
                        }
                        ChallengeStatus.ACCEPTED -> {
                            Text(
                                text = stringResource(R.string.challenges_outgoing_match_in_progress),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        else -> Unit
                    }
                },
            )
        }
    }
}

@Composable
private fun ChallengeRow(
    card: ChallengeCard,
    onClick: (() -> Unit)?,
    trailing: (@Composable () -> Unit)?,
) {
    val cardModifier = Modifier
        .fillMaxWidth()
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.other?.player?.displayName
                            ?: stringResource(R.string.challenges_unknown_player),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                    )
                    val tier = card.other?.let { tierDisplay(it) }
                    if (tier != null) {
                        Text(
                            text = tier,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                StatusChip(status = card.challenge.status)
            }
            val message = card.challenge.message
            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    trailing()
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: ChallengeStatus) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(stringResource(status.labelRes())) },
    )
}

@Composable
private fun EmptyState(@StringRes messageRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(messageRes),
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
    error: ChallengesError,
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
                Text(stringResource(R.string.play_retry))
            }
        }
    }
}

@StringRes
private fun ChallengeStatus.labelRes(): Int = when (this) {
    ChallengeStatus.PENDING -> R.string.play_status_pending
    ChallengeStatus.ACCEPTED -> R.string.play_status_accepted
    ChallengeStatus.DECLINED -> R.string.play_status_declined
    ChallengeStatus.CANCELLED -> R.string.play_status_cancelled
    ChallengeStatus.EXPIRED -> R.string.play_status_expired
    ChallengeStatus.COMPLETED -> R.string.play_status_completed
}

@StringRes
private fun ChallengesError.messageRes(): Int = when (this) {
    ChallengesError.Network -> R.string.play_error_generic
    ChallengesError.NotAuthenticated -> R.string.play_error_not_authenticated
}
