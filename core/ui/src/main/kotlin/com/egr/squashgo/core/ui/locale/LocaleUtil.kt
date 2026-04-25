package com.egr.squashgo.core.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale {
    val locales = LocalConfiguration.current.locales
    return if (locales.isEmpty) Locale.getDefault() else locales[0]
}
