package com.egr.squashgo.ui

import androidx.lifecycle.ViewModel
import com.egr.squashgo.core.designsystem.theme.ThemeResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin holder that lets the root @Composable obtain the Hilt-provided [ThemeResolver].
 *
 * Composables cannot use `@Inject` directly; routing the resolver through a [ViewModel]
 * with [HiltViewModel] is the cheapest way to bridge Hilt and Compose for an
 * app-scoped singleton.
 */
@HiltViewModel
class ThemeRootViewModel @Inject constructor(
    val resolver: ThemeResolver,
) : ViewModel()
