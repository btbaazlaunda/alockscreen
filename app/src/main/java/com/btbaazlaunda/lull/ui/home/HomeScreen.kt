package com.btbaazlaunda.lull.ui.home

import android.content.res.Configuration
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.btbaazlaunda.lull.R
import com.btbaazlaunda.lull.core.Feature
import com.btbaazlaunda.lull.shizuku.ShizukuStatus
import com.btbaazlaunda.lull.ui.FeatureToggle
import com.btbaazlaunda.lull.ui.HomeUiState
import com.btbaazlaunda.lull.ui.MainViewModel
import com.btbaazlaunda.lull.ui.canRequestAddTile
import com.btbaazlaunda.lull.ui.describe
import com.btbaazlaunda.lull.ui.descriptionRes
import com.btbaazlaunda.lull.ui.labelRes
import com.btbaazlaunda.lull.ui.requestAddTile
import com.btbaazlaunda.lull.ui.requestPinWidget
import com.btbaazlaunda.lull.ui.theme.LullTheme
import kotlinx.coroutines.launch

private val SheetOverlap = 32.dp

@Composable
fun HomeRoute(viewModel: MainViewModel = viewModel(factory = MainViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        // Shizuku may have been installed, started or authorised while we were away.
        viewModel.refreshSetup()
        onPauseOrDispose { }
    }
    HomeScreen(
        state = state,
        onToggleSleep = viewModel::toggleSleep,
        onFeatureChange = viewModel::setIncluded,
        onAllowShizuku = viewModel::requestShizukuPermission,
        onOutcomeShown = viewModel::outcomeShown,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onToggleSleep: () -> Unit,
    onFeatureChange: (Feature, Boolean) -> Unit,
    onAllowShizuku: () -> Unit,
    onOutcomeShown: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    state.outcome?.let { outcome ->
        LaunchedEffect(outcome) {
            outcome.describe(context)?.let { snackbarHostState.showSnackbar(it, withDismissAction = true) }
            onOutcomeShown()
        }
    }

    // The sky sits behind the status bar, so its icons follow the sky rather than the theme.
    val activity = LocalActivity.current
    val lightSky = !state.sleeping && !isSystemInDarkTheme()
    LaunchedEffect(activity, lightSky) {
        activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = lightSky }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SkyHero(state = state, onToggle = onToggleSleep)
            Surface(
                shape = RoundedCornerShape(topStart = SheetOverlap, topEnd = SheetOverlap),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = -SheetOverlap),
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 600.dp)
                            .padding(horizontal = 16.dp)
                            .padding(top = 24.dp, bottom = 16.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AnimatedVisibility(visible = state.loaded && state.shizuku != ShizukuStatus.READY) {
                            SetupCard(status = state.shizuku, onAllow = onAllowShizuku)
                        }
                        SectionHeader(R.string.section_features)
                        SegmentedGroup {
                            state.features.forEach { FeatureRow(it, onFeatureChange) }
                        }
                        SectionHeader(R.string.section_shortcuts)
                        SegmentedGroup {
                            ShortcutRow(
                                icon = Icons.Rounded.Widgets,
                                title = R.string.shortcut_widget,
                                description = R.string.shortcut_widget_description,
                                onAdd = {
                                    if (!context.requestPinWidget()) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.shortcut_widget_manual))
                                        }
                                    }
                                },
                            )
                            ShortcutRow(
                                icon = Icons.Rounded.ToggleOn,
                                title = R.string.shortcut_tile,
                                description = if (canRequestAddTile) {
                                    R.string.shortcut_tile_description
                                } else {
                                    R.string.shortcut_tile_manual
                                },
                                onAdd = if (canRequestAddTile) ({ context.requestAddTile() }) else null,
                            )
                        }
                        Text(
                            text = stringResource(R.string.footer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(start = 8.dp, top = 12.dp)
            .semantics { heading() },
    )
}

/** Rows separated by thin gaps inside one rounded container. */
@Composable
private fun SegmentedGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.clip(RoundedCornerShape(24.dp)),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}

@Composable
private fun FeatureRow(item: FeatureToggle, onChange: (Feature, Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.toggleable(
            value = item.included,
            role = Role.Switch,
            onValueChange = { onChange(item.feature, it) },
        ),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = { IconBadge(item.feature.icon, active = item.included) },
        headlineContent = { Text(stringResource(item.feature.labelRes)) },
        supportingContent = { Text(stringResource(item.feature.descriptionRes)) },
        trailingContent = {
            Switch(
                checked = item.included,
                onCheckedChange = null,
                thumbContent = if (item.included) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else {
                    null
                },
            )
        },
    )
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    @StringRes title: Int,
    @StringRes description: Int,
    onAdd: (() -> Unit)?,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        leadingContent = { IconBadge(icon, active = false) },
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(description)) },
        trailingContent = if (onAdd != null) {
            { FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.shortcut_add)) } }
        } else {
            null
        },
    )
}

@Composable
private fun IconBadge(icon: ImageVector, active: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (active) colors.primaryContainer else colors.surfaceContainerHighest,
        contentColor = if (active) colors.onPrimaryContainer else colors.onSurfaceVariant,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        }
    }
}

private val Feature.icon: ImageVector
    get() = when (this) {
        Feature.WIFI -> Icons.Rounded.Wifi
        Feature.MOBILE_DATA -> Icons.Rounded.SignalCellularAlt
        Feature.BLUETOOTH -> Icons.Rounded.Bluetooth
        Feature.BATTERY_SAVER -> Icons.Rounded.BatterySaver
        Feature.DO_NOT_DISTURB -> Icons.Rounded.DoNotDisturbOn
    }

private val PreviewFeatures = Feature.entries.map { FeatureToggle(it, it in Feature.DEFAULTS) }

@Preview(name = "Awake, setup needed")
@Composable
private fun HomeAwakePreview() {
    LullTheme {
        HomeScreen(
            state = HomeUiState(loaded = true, features = PreviewFeatures, shizuku = ShizukuStatus.NOT_RUNNING),
            onToggleSleep = {},
            onFeatureChange = { _, _ -> },
            onAllowShizuku = {},
            onOutcomeShown = {},
        )
    }
}

@Preview(name = "Sleeping", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeSleepingPreview() {
    LullTheme {
        HomeScreen(
            state = HomeUiState(
                loaded = true,
                sleeping = true,
                sleepStartedAt = System.currentTimeMillis() - 26_000_000,
                features = PreviewFeatures,
            ),
            onToggleSleep = {},
            onFeatureChange = { _, _ -> },
            onAllowShizuku = {},
            onOutcomeShown = {},
        )
    }
}
