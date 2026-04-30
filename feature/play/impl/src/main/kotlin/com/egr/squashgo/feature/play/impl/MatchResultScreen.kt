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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.egr.squashgo.feature.play.impl.model.GameInput
import com.egr.squashgo.feature.play.impl.model.MatchResultError
import com.egr.squashgo.feature.play.impl.model.MatchResultUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchResultScreen(
    matchId: String,
    onResultSubmitted: () -> Unit,
    onBack: () -> Unit,
    viewModel: MatchResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(matchId) {
        viewModel.load(matchId)
    }

    LaunchedEffect(uiState) {
        if (uiState is MatchResultUiState.Done) {
            onResultSubmitted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.match_result_title)) },
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
                is MatchResultUiState.Loading -> LoadingView()
                is MatchResultUiState.Error -> ErrorView(state.error, onRetry = viewModel::retry)
                is MatchResultUiState.Done -> LoadingView()
                is MatchResultUiState.Ready -> ReadyForm(
                    state = state,
                    onScoreChange = viewModel::updateScore,
                    onSubmit = viewModel::submit,
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
            Text(stringResource(R.string.match_result_loading))
        }
    }
}

@Composable
private fun ErrorView(error: MatchResultError, onRetry: () -> Unit) {
    val messageRes = when (error) {
        MatchResultError.Network -> R.string.match_result_load_error
        MatchResultError.NotAuthenticated -> R.string.play_error_not_authenticated
        MatchResultError.NotParticipant -> R.string.match_result_error_not_participant
        MatchResultError.AlreadySubmitted -> R.string.match_result_error_already_submitted
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
        if (error == MatchResultError.Network) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text(stringResource(R.string.play_retry))
            }
        }
    }
}

@Composable
private fun ReadyForm(
    state: MatchResultUiState.Ready,
    onScoreChange: (index: Int, isPlayerA: Boolean, raw: String) -> Unit,
    onSubmit: () -> Unit,
) {
    val leftLabel: String
    val rightLabel: String
    if (state.youAreA) {
        leftLabel = "${state.youName} (${stringResource(R.string.challenge_create_you_label)})"
        rightLabel = state.opponentName
    } else {
        leftLabel = state.opponentName
        rightLabel = "${state.youName} (${stringResource(R.string.challenge_create_you_label)})"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        ColumnsHeader(leftLabel = leftLabel, rightLabel = rightLabel)

        Spacer(Modifier.height(8.dp))

        state.games.forEachIndexed { index, game ->
            GameRow(
                index = index,
                game = game,
                leftLabel = leftLabel,
                rightLabel = rightLabel,
                enabled = !state.isSubmitting,
                onScoreChange = onScoreChange,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (state.showInvalidScoreMessage) {
            Text(
                text = stringResource(R.string.match_result_invalid),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (state.submitError) {
            Text(
                text = stringResource(R.string.match_result_submit_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(
                    if (state.isSubmitting) R.string.match_result_submitting
                    else R.string.match_result_submit,
                ),
            )
        }
    }
}

@Composable
private fun ColumnsHeader(leftLabel: String, rightLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(GAME_LABEL_WIDTH))
        Text(
            text = leftLabel,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = rightLabel,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GameRow(
    index: Int,
    game: GameInput,
    leftLabel: String,
    rightLabel: String,
    enabled: Boolean,
    onScoreChange: (index: Int, isPlayerA: Boolean, raw: String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.match_result_game_label, index + 1),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(GAME_LABEL_WIDTH),
        )
        ScoreField(
            value = game.playerAScore,
            cdLabel = leftLabel,
            gameNumber = index + 1,
            enabled = enabled,
            onValueChange = { onScoreChange(index, true, it) },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        ScoreField(
            value = game.playerBScore,
            cdLabel = rightLabel,
            gameNumber = index + 1,
            enabled = enabled,
            onValueChange = { onScoreChange(index, false, it) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ScoreField(
    value: String,
    cdLabel: String,
    gameNumber: Int,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cd = stringResource(R.string.match_result_score_input_cd, cdLabel, gameNumber)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        placeholder = {
            Text(
                text = stringResource(R.string.match_result_score_placeholder),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        modifier = modifier.semantics { contentDescription = cd },
    )
}

private val GAME_LABEL_WIDTH = 72.dp
