package com.egr.squashgo.feature.onboarding.impl

import android.net.Uri
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
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    // Handle magic link deep link: squashgo://auth-callback#access_token=...
    LaunchedEffect(deepLinkUri) {
        if (deepLinkUri == null) return@LaunchedEffect
        val fragment = deepLinkUri.fragment ?: return@LaunchedEffect
        val params = fragment.split("&").associate {
            val (key, value) = it.split("=", limit = 2)
            key to value
        }
        val token = params["access_token"]
        if (token != null) {
            viewModel.handleAccessToken(token)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Squash & Go",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Find rivals. Play matches. Climb the ranks.",
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
                    label = { Text("Email address") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    enabled = state !is LoginUiState.SendingLink,
                )
                if (state is LoginUiState.Error) {
                    Text(
                        text = state.message,
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
                        Text("Send Magic Link")
                    }
                }
            }

            is LoginUiState.LinkSent -> {
                Text(
                    text = "Check your email",
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "We sent a login link to $email.\nTap the link in the email to sign in.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = viewModel::sendMagicLink,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Resend Link")
                }
            }

            is LoginUiState.Verifying -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Signing you in...",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            is LoginUiState.Success -> {
                // Handled by LaunchedEffect
            }
        }
    }
}
