package com.egr.squashgo.feature.play.impl

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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egr.squashgo.core.model.MatchType
import com.egr.squashgo.feature.play.impl.model.ChallengeDetailError
import com.egr.squashgo.feature.play.impl.model.ChallengeDetailUiState
import com.egr.squashgo.feature.play.impl.model.ExpiryDisplay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: ChallengeDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(challengeId) {
        viewModel.load(challengeId)
    }

    LaunchedEffect(uiState) {
        if (uiState is ChallengeDetailUiState.Done) {
            onDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.challenge_detail_title)) },
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
                is ChallengeDetailUiState.Loading,
                is ChallengeDetailUiState.Done,
                -> CenteredSpinner()

                is ChallengeDetailUiState.Error -> ErrorState(
                    error = state.error,
                    onRetry = viewModel::retry,
                )

                is ChallengeDetailUiState.Ready -> ReadyContent(
                    state = state,
                    onAccept = viewModel::accept,
                    onDecline = viewModel::decline,
                )
            }
        }
    }
}

@Composable
private fun ReadyContent(
    state: ChallengeDetailUiState.Ready,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.challenge_detail_from),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = state.challenger.player.displayName,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tierDisplay(state.challenger),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        stringResource(
                            when (state.challenge.matchType) {
                                MatchType.CASUAL -> R.string.play_match_type_casual
                                MatchType.RANKED -> R.string.play_match_type_ranked
                            },
                        ),
                    )
                },
            )
            Text(
                text = expiryText(state.expiry),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.challenge_detail_message),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        val message = state.challenge.message
        if (message.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.challenge_detail_no_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
            )
        } else {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (state.actionError) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.challenge_detail_action_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f),
                enabled = !state.isActing,
            ) {
                Text(stringResource(R.string.challenge_detail_decline))
            }
            Button(
                onClick = onAccept,
                modifier = Modifier.weight(1f),
                enabled = !state.isActing,
            ) {
                if (state.isActing) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(20.dp),
                    )
                } else {
                    Text(stringResource(R.string.challenge_detail_accept))
                }
            }
        }
    }
}

@Composable
private fun expiryText(expiry: ExpiryDisplay): String = when (expiry) {
    ExpiryDisplay.WithinHour -> stringResource(R.string.challenge_detail_expires_soon)
    is ExpiryDisplay.Hours -> pluralStringResource(
        R.plurals.challenge_detail_expires_in_hours,
        expiry.count,
        expiry.count,
    )
    is ExpiryDisplay.Days -> pluralStringResource(
        R.plurals.challenge_detail_expires_in_days,
        expiry.count,
        expiry.count,
    )
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
    error: ChallengeDetailError,
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
private fun ChallengeDetailError.messageRes(): Int = when (this) {
    ChallengeDetailError.Network -> R.string.play_error_generic
    ChallengeDetailError.NotFound -> R.string.play_error_generic
    ChallengeDetailError.NotAuthenticated -> R.string.play_error_not_authenticated
}
