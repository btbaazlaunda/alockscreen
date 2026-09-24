package com.btbaazlaunda.lull.ui.home

import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.btbaazlaunda.lull.R
import com.btbaazlaunda.lull.ui.HomeUiState
import com.btbaazlaunda.lull.ui.theme.DaySky
import com.btbaazlaunda.lull.ui.theme.DuskSky
import com.btbaazlaunda.lull.ui.theme.NightSky
import com.btbaazlaunda.lull.ui.theme.SkyPalette
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SkyHero(state: HomeUiState, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val sky = animatedSky(
        when {
            state.sleeping -> NightSky
            isSystemInDarkTheme() -> DuskSky
            else -> DaySky
        },
    )
    val starAlpha by animateFloatAsState(
        targetValue = if (state.sleeping) 1f else 0f,
        animationSpec = tween(durationMillis = 1200),
        label = "stars",
    )
    val twinkle = twinkleClock(running = state.sleeping)
    val contentAlpha by animateFloatAsState(if (state.loaded) 1f else 0f, label = "content")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(Brush.verticalGradient(listOf(sky.top, sky.middle, sky.bottom)))
                if (starAlpha > 0f) {
                    val t = twinkle.value
                    for (star in Stars) {
                        val flicker = 0.6f + 0.4f * sin((t + star.phase) * 2f * PI.toFloat())
                        drawCircle(
                            color = Color.White,
                            radius = star.radius.dp.toPx(),
                            center = Offset(star.x * size.width, star.y * size.height),
                            alpha = starAlpha * flicker,
                        )
                    }
                }
            }
            .statusBarsPadding()
            .padding(top = 20.dp, bottom = 64.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = contentAlpha },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
                color = sky.content.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(24.dp))
            SleepButton(
                sleeping = state.sleeping,
                busy = state.busy,
                enabled = state.loaded && !state.busy,
                sky = sky,
                onToggle = onToggle,
            )
            Spacer(Modifier.height(20.dp))
            AnimatedContent(
                targetState = state.sleeping,
                transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(300)) },
                label = "title",
            ) { sleeping ->
                Text(
                    text = stringResource(if (sleeping) R.string.hero_sleeping_title else R.string.hero_awake_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = sky.content,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = heroSubtitle(state),
                style = MaterialTheme.typography.bodyLarge,
                color = sky.content.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepButton(
    sleeping: Boolean,
    busy: Boolean,
    enabled: Boolean,
    sky: SkyPalette,
    onToggle: () -> Unit,
) {
    val view = LocalView.current
    val scale by animateFloatAsState(
        targetValue = if (busy) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press",
    )
    val label = stringResource(R.string.sleep_button_label)
    val stateText = stringResource(if (sleeping) R.string.sleep_state_on else R.string.sleep_state_off)

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
        Box(
            Modifier
                .size(240.dp)
                .drawBehind {
                    drawCircle(Brush.radialGradient(listOf(sky.glow.copy(alpha = 0.55f), sky.glow.copy(alpha = 0f))))
                },
        )
        Surface(
            checked = sleeping,
            onCheckedChange = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                onToggle()
            },
            enabled = enabled,
            shape = CircleShape,
            color = sky.button,
            modifier = Modifier
                .size(168.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .semantics {
                    role = Role.Switch
                    contentDescription = label
                    stateDescription = stateText
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Crossfade(targetState = sleeping, animationSpec = tween(500), label = "icon") { asleep ->
                    Icon(
                        imageVector = if (asleep) Icons.Rounded.Bedtime else Icons.Rounded.WbSunny,
                        contentDescription = null,
                        tint = sky.icon,
                        modifier = Modifier.size(76.dp),
                    )
                }
            }
        }
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(188.dp),
                color = sky.glow,
                strokeWidth = 3.dp,
                trackColor = Color.Transparent,
            )
        }
    }
}

@Composable
private fun heroSubtitle(state: HomeUiState): String {
    if (!state.loaded) return ""
    if (state.sleeping) {
        val context = LocalContext.current
        val now by produceState(System.currentTimeMillis(), state.sleepStartedAt) {
            while (true) {
                value = System.currentTimeMillis()
                delay(30_000)
            }
        }
        val since = remember(state.sleepStartedAt) {
            DateFormat.getTimeFormat(context).format(Date(state.sleepStartedAt))
        }
        return stringResource(R.string.hero_sleeping_since, since, formatDuration(now - state.sleepStartedAt))
    }
    return if (state.lastSleepDurationMs > 0) {
        stringResource(R.string.hero_last_sleep, formatDuration(state.lastSleepDurationMs))
    } else {
        stringResource(R.string.hero_awake_hint)
    }
}

@Composable
private fun formatDuration(ms: Long): String {
    val totalMinutes = (ms / 60_000).coerceAtLeast(0)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.duration_hours_minutes, hours, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
}

@Composable
private fun animatedSky(target: SkyPalette): SkyPalette {
    val spec = tween<Color>(durationMillis = 900, easing = FastOutSlowInEasing)
    val top by animateColorAsState(target.top, spec, label = "top")
    val middle by animateColorAsState(target.middle, spec, label = "middle")
    val bottom by animateColorAsState(target.bottom, spec, label = "bottom")
    val content by animateColorAsState(target.content, spec, label = "content")
    val button by animateColorAsState(target.button, spec, label = "button")
    val icon by animateColorAsState(target.icon, spec, label = "icon")
    val glow by animateColorAsState(target.glow, spec, label = "glow")
    return SkyPalette(top, middle, bottom, content, button, icon, glow)
}

/** A 0..1 clock that loops only while the stars are out, so the awake screen stays idle. */
@Composable
private fun twinkleClock(running: Boolean): State<Float> {
    if (!running) return remember { mutableFloatStateOf(0f) }
    return rememberInfiniteTransition(label = "twinkle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 6_000, easing = LinearEasing), RepeatMode.Restart),
        label = "twinkle",
    )
}

private class Star(val x: Float, val y: Float, val radius: Float, val phase: Float)

private val Stars: List<Star> = Random(seed = 7).let { random ->
    List(56) { Star(random.nextFloat(), random.nextFloat() * 0.9f, 0.5f + random.nextFloat() * 1.3f, random.nextFloat()) }
}
