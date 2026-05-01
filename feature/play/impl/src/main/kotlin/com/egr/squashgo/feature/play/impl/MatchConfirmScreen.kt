package com.egr.squashgo.feature.play.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egr.squashgo.core.model.GameScore
import com.egr.squashgo.feature.play.impl.model.MatchConfirmError
import com.egr.squashgo.feature.play.impl.model.MatchConfirmUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchConfirmScreen(
    matchId: String,
    onConfirmed: () -> Unit,
    onBack: () -> Unit,
    viewModel: MatchConfirmViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(matchId) {
        viewModel.load(matchId)
    }

    LaunchedEffect(uiState) {
        if (uiState is MatchConfirmUiState.Done) {
            onConfirmed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.match_confirm_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.play_back),
                        )
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
                is MatchConfirmUiState.Loading -> LoadingView()
                is MatchConfirmUiState.Done -> LoadingView()
                is MatchConfirmUiState.Error -> ErrorView(state.error, onRetry = viewModel::retry)
                is MatchConfirmUiState.Ready -> ReadyView(
                    state = state,
                    onConfirm = viewModel::confirm,
                    onOpenDispute = viewModel::openDisputeDialog,
                    onDismissDispute = viewModel::dismissDisputeDialog,
                    onDisputeReasonChange = viewModel::updateDisputeReason,
                    onSubmitDispute = viewModel::submitDispute,
                )
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.match_confirm_loading))
        }
    }
}

@Composable
private fun ErrorView(error: MatchConfirmError, onRetry: () -> Unit) {
    val messageRes = when (error) {
        MatchConfirmError.Network -> R.string.match_confirm_load_error
        MatchConfirmError.NotAuthenticated -> R.string.play_error_not_authenticated
        MatchConfirmError.NotParticipant -> R.string.match_confirm_error_not_participant
        MatchConfirmError.NothingToConfirm -> R.string.match_confirm_error_nothing
        MatchConfirmError.OwnSubmission -> R.string.match_confirm_error_own
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (error == MatchConfirmError.Network) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.play_retry))
            }
        }
    }
}

@Composable
private fun ReadyView(
    state: MatchConfirmUiState.Ready,
    onConfirm: () -> Unit,
    onOpenDispute: () -> Unit,
    onDismissDispute: () -> Unit,
    onDisputeReasonChange: (String) -> Unit,
    onSubmitDispute: () -> Unit,
) {
    val games = state.match.score?.games.orEmpty()
    val winnerIsA = state.match.winnerId == state.match.playerAId
    val anyActionInProgress = state.isConfirming || state.isDisputing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (state.submitterName.isNotBlank()) {
            Text(
                text = stringResource(R.string.match_confirm_submitted_by, state.submitterName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
        }

        ColumnsHeader(
            leftLabel = state.playerAName,
            rightLabel = state.playerBName,
            leftIsWinner = winnerIsA,
            rightIsWinner = !winnerIsA,
        )

        Spacer(Modifier.height(8.dp))

        games.forEachIndexed { index, game ->
            GameRowReadOnly(index = index, game = game)
        }

        Spacer(Modifier.height(16.dp))

        if (state.confirmError) {
            Text(
                text = stringResource(R.string.match_confirm_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onOpenDispute,
                enabled = !anyActionInProgress,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.match_dispute_button))
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onConfirm,
                enabled = !anyActionInProgress,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(
                        if (state.isConfirming) R.string.match_confirm_confirming
                        else R.string.match_confirm_button,
                    ),
                )
            }
        }
    }

    if (state.disputeDialogOpen) {
        DisputeDialog(
            reason = state.disputeReason,
            isSubmitting = state.isDisputing,
            showError = state.disputeError,
            onReasonChange = onDisputeReasonChange,
            onSubmit = onSubmitDispute,
            onDismiss = onDismissDispute,
        )
    }
}

@Composable
private fun DisputeDialog(
    reason: String,
    isSubmitting: Boolean,
    showError: Boolean,
    onReasonChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    val canSubmit = reason.trim().isNotEmpty() && !isSubmitting
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(stringResource(R.string.match_dispute_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.match_dispute_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    enabled = !isSubmitting,
                    label = { Text(stringResource(R.string.match_dispute_reason_label)) },
                    supportingText = {
                        Text(
                            text = stringResource(
                                R.string.match_dispute_reason_counter,
                                reason.length,
                                MAX_REASON_LENGTH_DISPLAY,
                            ),
                        )
                    },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.match_dispute_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onSubmit, enabled = canSubmit) {
                Text(
                    text = stringResource(
                        if (isSubmitting) R.string.match_dispute_submitting
                        else R.string.match_dispute_submit,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text(stringResource(R.string.play_cancel))
            }
        },
    )
}

private const val MAX_REASON_LENGTH_DISPLAY = 240

@Composable
private fun ColumnsHeader(
    leftLabel: String,
    rightLabel: String,
    leftIsWinner: Boolean,
    rightIsWinner: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(GAME_LABEL_WIDTH))
        PlayerHeader(
            name = leftLabel,
            isWinner = leftIsWinner,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        PlayerHeader(
            name = rightLabel,
            isWinner = rightIsWinner,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlayerHeader(name: String, isWinner: Boolean, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        if (isWinner) {
            Text(
                text = stringResource(R.string.match_confirm_winner_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun GameRowReadOnly(index: Int, game: GameScore) {
    val winnerIsA = game.playerAScore > game.playerBScore
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.match_result_game_label, index + 1),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(GAME_LABEL_WIDTH),
        )
        ScoreCell(
            value = game.playerAScore,
            isWinner = winnerIsA,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        ScoreCell(
            value = game.playerBScore,
            isWinner = !winnerIsA,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScoreCell(value: Int, isWinner: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = value.toString(),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
        color = if (isWinner) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

private val GAME_LABEL_WIDTH = 72.dp
