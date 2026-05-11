package com.egr.squashgo.feature.profile.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.egr.squashgo.core.designsystem.component.AppCard
import com.egr.squashgo.core.designsystem.component.EmptyState
import com.egr.squashgo.core.designsystem.component.LoadingIndicator
import com.egr.squashgo.core.designsystem.component.PrimaryButton
import com.egr.squashgo.core.designsystem.component.SecondaryButton
import com.egr.squashgo.core.designsystem.tokens.BrandPalette
import com.egr.squashgo.feature.profile.impl.model.DesignSystemFetchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesignSystemDemoScreen(
    onBack: () -> Unit,
    viewModel: DesignSystemDemoViewModel = hiltViewModel(),
) {
    val tokens by viewModel.tokens
    val snackbarHostState = remember { SnackbarHostState() }
    val lastFetch by viewModel.lastFetch
    val successTemplate = stringResource(R.string.design_system_demo_fetch_success)
    val noActiveMsg = stringResource(R.string.design_system_demo_fetch_no_active)
    val errorMsg = stringResource(R.string.design_system_demo_fetch_error)

    LaunchedEffect(lastFetch) {
        val message = when (val result = lastFetch) {
            null -> null
            is DesignSystemFetchResult.Success -> successTemplate.format(result.brandName)
            DesignSystemFetchResult.NoActivePalette -> noActiveMsg
            is DesignSystemFetchResult.Error -> errorMsg
        }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.consumeLastFetch()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.design_system_demo_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.design_system_demo_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item { CurrentBrandHeader(brandName = tokens.brandName) }
            item { ColorSwatchesRow() }
            item { PaletteSwitcherSection(viewModel) }
            item { RemoteSyncSection(viewModel) }
            item { WidgetsCatalogHeader() }
            item { PrimaryButtonSample() }
            item { SecondaryButtonSample() }
            item { AppCardSample() }
            item { EmptyStateSample() }
            item { LoadingIndicatorSample() }
        }
    }
}

@Composable
private fun CurrentBrandHeader(brandName: String) {
    Column {
        Text(
            text = stringResource(R.string.design_system_demo_current_brand),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = brandName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ColorSwatchesRow() {
    Column {
        Text(
            text = stringResource(R.string.design_system_demo_swatches_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        val scheme = MaterialTheme.colorScheme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Swatch("primary", scheme.primary, scheme.onPrimary, Modifier.weight(1f))
            Swatch("container", scheme.primaryContainer, scheme.onPrimaryContainer, Modifier.weight(1f))
            Swatch("secondary", scheme.secondary, scheme.onSecondary, Modifier.weight(1f))
            Swatch("surface", scheme.surface, scheme.onSurface, Modifier.weight(1f))
            Swatch("error", scheme.error, scheme.onError, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Swatch(
    label: String,
    background: Color,
    foreground: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(background),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "A",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = foreground,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PaletteSwitcherSection(viewModel: DesignSystemDemoViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.design_system_demo_switch_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        viewModel.palettes.forEach { palette: BrandPalette ->
            PrimaryButton(
                text = palette.name,
                onClick = { viewModel.applyPalette(palette) },
            )
        }
        SecondaryButton(
            text = stringResource(R.string.design_system_demo_reset),
            onClick = viewModel::reset,
        )
    }
}

@Composable
private fun RemoteSyncSection(viewModel: DesignSystemDemoViewModel) {
    val isSyncing by viewModel.isSyncing
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.design_system_demo_remote_section_label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.design_system_demo_remote_section_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(
            text = if (isSyncing) {
                stringResource(R.string.design_system_demo_remote_syncing)
            } else {
                stringResource(R.string.design_system_demo_remote_fetch)
            },
            onClick = viewModel::fetchFromRemote,
            enabled = !isSyncing,
        )
    }
}

@Composable
private fun WidgetsCatalogHeader() {
    Text(
        text = stringResource(R.string.design_system_demo_widgets_title),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 16.dp),
    )
}

@Composable
private fun PrimaryButtonSample() {
    PrimaryButton(
        text = stringResource(R.string.design_system_demo_primary_button),
        onClick = {},
    )
}

@Composable
private fun SecondaryButtonSample() {
    SecondaryButton(
        text = stringResource(R.string.design_system_demo_secondary_button),
        onClick = {},
    )
}

@Composable
private fun AppCardSample() {
    AppCard {
        Text(
            text = stringResource(R.string.design_system_demo_appcard_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.design_system_demo_appcard_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyStateSample() {
    Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
        EmptyState(
            title = stringResource(R.string.design_system_demo_empty_title),
            body = stringResource(R.string.design_system_demo_empty_body),
        )
    }
}

@Composable
private fun LoadingIndicatorSample() {
    Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
        LoadingIndicator()
    }
}
