package com.egr.squashgo.feature.onboarding.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egr.squashgo.feature.onboarding.impl.model.ProfileSetupError
import com.egr.squashgo.feature.onboarding.impl.model.ProfileSetupUiState

@Composable
fun ProfileSetupScreen(
    onProfileSaved: () -> Unit,
    viewModel: ProfileSetupViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is ProfileSetupUiState.Saved || uiState is ProfileSetupUiState.AlreadyComplete) {
            onProfileSaved()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val state = uiState) {
            is ProfileSetupUiState.Loading,
            is ProfileSetupUiState.AlreadyComplete,
            -> CircularProgressIndicator()

            is ProfileSetupUiState.Editing,
            is ProfileSetupUiState.Saving,
            is ProfileSetupUiState.Saved,
            is ProfileSetupUiState.Error,
            -> ProfileForm(
                displayName = displayName,
                state = state,
                onDisplayNameChange = viewModel::onDisplayNameChange,
                onSave = viewModel::save,
                onRetry = viewModel::retryLoad,
            )
        }
    }
}

@Composable
private fun ProfileForm(
    displayName: String,
    state: ProfileSetupUiState,
    onDisplayNameChange: (String) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    val isSaving = state is ProfileSetupUiState.Saving
    val blockingError = state is ProfileSetupUiState.Error &&
        state.error == ProfileSetupError.NotAuthenticated
    val inlineError = (state as? ProfileSetupUiState.Error)?.error

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.profile_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_setup_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = displayName,
            onValueChange = onDisplayNameChange,
            label = { Text(stringResource(R.string.profile_setup_display_name_label)) },
            supportingText = {
                Text(
                    stringResource(
                        R.string.profile_setup_display_name_hint,
                        ProfileSetupViewModel.DISPLAY_NAME_MIN,
                        ProfileSetupViewModel.DISPLAY_NAME_MAX,
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !isSaving && !blockingError,
            isError = inlineError == ProfileSetupError.InvalidLength,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        )

        if (inlineError != null && inlineError != ProfileSetupError.NotAuthenticated) {
            Text(
                text = stringResource(inlineError.messageRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        if (blockingError) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.profile_setup_error_not_authenticated),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.profile_setup_retry))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving && !blockingError && displayName.isNotBlank(),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp),
                )
            } else {
                Text(stringResource(R.string.profile_setup_continue))
            }
        }
    }
}

@StringRes
private fun ProfileSetupError.messageRes(): Int = when (this) {
    ProfileSetupError.InvalidLength -> R.string.profile_setup_error_invalid_length
    ProfileSetupError.Network -> R.string.profile_setup_error_network
    ProfileSetupError.NotAuthenticated -> R.string.profile_setup_error_not_authenticated
}
