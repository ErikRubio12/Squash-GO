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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egr.squashgo.core.domain.rating.TierMapper
import com.egr.squashgo.core.domain.repository.PlayerWithRating
import com.egr.squashgo.core.model.MatchType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeCreateScreen(
    challengedId: String,
    courtId: String?,
    onChallengeSent: () -> Unit,
    onBack: () -> Unit,
    viewModel: ChallengeCreateViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(challengedId, courtId) {
        viewModel.load(challengedId, courtId)
    }

    LaunchedEffect(uiState) {
        if (uiState is ChallengeCreateUiState.Sent) {
            onChallengeSent()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.challenge_create_title)) },
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
                is ChallengeCreateUiState.Loading,
                is ChallengeCreateUiState.Sent,
                -> CenteredSpinner()

                is ChallengeCreateUiState.Error -> ErrorState(
                    error = state.error,
                    onRetry = viewModel::retry,
                )

                is ChallengeCreateUiState.Ready -> ReadyContent(
                    state = state,
                    onMatchTypeChange = viewModel::onMatchTypeChange,
                    onMessageChange = viewModel::onMessageChange,
                    onSend = viewModel::send,
                )
            }
        }
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
    error: ChallengeCreateError,
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

@Composable
private fun ReadyContent(
    state: ChallengeCreateUiState.Ready,
    onMatchTypeChange: (MatchType) -> Unit,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        VersusHeader(me = state.me, opponent = state.opponent)

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.challenge_create_match_type_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.matchType == MatchType.CASUAL,
                onClick = { onMatchTypeChange(MatchType.CASUAL) },
                label = { Text(stringResource(R.string.play_match_type_casual)) },
            )
            FilterChip(
                selected = state.matchType == MatchType.RANKED,
                onClick = { onMatchTypeChange(MatchType.RANKED) },
                label = { Text(stringResource(R.string.play_match_type_ranked)) },
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                when (state.matchType) {
                    MatchType.RANKED -> R.string.challenge_create_ranked_hint
                    MatchType.CASUAL -> R.string.challenge_create_casual_hint
                },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChange,
            label = { Text(stringResource(R.string.challenge_create_message_label)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
            maxLines = 4,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(
                R.string.challenge_create_message_counter,
                state.message.length,
                ChallengeCreateViewModel.MAX_MESSAGE_LENGTH,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            textAlign = TextAlign.End,
        )

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(state.error.messageRes()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onSend,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSending,
        ) {
            if (state.isSending) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp),
                )
            } else {
                Text(stringResource(R.string.challenge_create_send))
            }
        }
    }
}

@Composable
private fun VersusHeader(
    me: PlayerWithRating,
    opponent: PlayerWithRating,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerCard(
            labelRes = R.string.challenge_create_you_label,
            player = me,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.challenge_create_vs),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PlayerCard(
            labelRes = R.string.challenge_create_opponent_label,
            player = opponent,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PlayerCard(
    @StringRes labelRes: Int,
    player: PlayerWithRating,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = player.player.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = tierDisplay(player),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
internal fun tierDisplay(playerWithRating: PlayerWithRating): String {
    val rating = playerWithRating.rating
    if (rating.isProvisional) return stringResource(R.string.play_calibrating)
    val tier = TierMapper.displayString(TierMapper.fromElo(rating.eloScore))
    return stringResource(R.string.play_tier_format, tier, rating.eloScore)
}

@StringRes
private fun ChallengeCreateError.messageRes(): Int = when (this) {
    ChallengeCreateError.Network -> R.string.challenge_create_error_network
    ChallengeCreateError.NotAuthenticated -> R.string.play_error_not_authenticated
}
