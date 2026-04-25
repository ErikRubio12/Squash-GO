package com.egr.squashgo.feature.onboarding.impl

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egr.squashgo.feature.onboarding.impl.model.LoginError
import com.egr.squashgo.feature.onboarding.impl.model.LoginUiState

@Composable
fun LoginScreen(
    deepLinkUri: Uri?,
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is LoginUiState.Success) {
            onLoginSuccess()
        }
    }

    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri == null) return@LaunchedEffect
        val fragment = deepLinkUri.fragment ?: return@LaunchedEffect
        val params = fragment.split("&").mapNotNull {
            val parts = it.split("=", limit = 2)
            if (parts.size == 2) parts[0] to parts[1] else null
        }.toMap()
        val accessToken = params["access_token"] ?: return@LaunchedEffect
        val refreshToken = params["refresh_token"] ?: return@LaunchedEffect
        val expiresIn = params["expires_in"]?.toLongOrNull() ?: 3600L
        viewModel.handleDeepLinkTokens(accessToken, refreshToken, expiresIn)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.login_app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_tagline),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(48.dp))

        when (val state = uiState) {
            is LoginUiState.Idle,
            is LoginUiState.SendingLink,
            is LoginUiState.Error,
            -> {
                OutlinedTextField(
                    value = email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text(stringResource(R.string.login_email_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = state !is LoginUiState.SendingLink,
                    isError = state is LoginUiState.Error,
                )
                if (state is LoginUiState.Error) {
                    Text(
                        text = stringResource(state.error.messageRes()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = viewModel::sendMagicLink,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = email.isNotBlank() && state !is LoginUiState.SendingLink,
                ) {
                    if (state is LoginUiState.SendingLink) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(20.dp),
                        )
                    } else {
                        Text(stringResource(R.string.login_send_magic_link))
                    }
                }
            }

            is LoginUiState.LinkSent -> {
                Text(
                    text = stringResource(R.string.login_check_email_title),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.login_check_email_body, email),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = viewModel::sendMagicLink,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.login_resend_link))
                }
            }

            is LoginUiState.Verifying -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.login_signing_in),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is LoginUiState.Success -> {
                // Handled by LaunchedEffect
            }
        }
    }
}

@StringRes
private fun LoginError.messageRes(): Int = when (this) {
    LoginError.InvalidEmail -> R.string.login_error_invalid_email
    LoginError.Network -> R.string.login_error_network
    LoginError.RateLimited -> R.string.login_error_rate_limited
    LoginError.Server -> R.string.login_error_server
    LoginError.Unknown -> R.string.login_error_unknown
}
