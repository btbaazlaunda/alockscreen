package com.btbaazlaunda.lull.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.btbaazlaunda.lull.R
import com.btbaazlaunda.lull.shizuku.ShizukuGateway
import com.btbaazlaunda.lull.shizuku.ShizukuStatus
import com.btbaazlaunda.lull.ui.openShizuku
import com.btbaazlaunda.lull.ui.openShizukuListing
import com.btbaazlaunda.lull.ui.openUrl

/** Walks the user through the one-time Shizuku setup: install, start, allow. */
@Composable
fun SetupCard(status: ShizukuStatus, onAllow: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val step = when (status) {
        ShizukuStatus.NOT_INSTALLED -> 0
        ShizukuStatus.NOT_RUNNING -> 1
        else -> 2
    }

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Key, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.setup_body), style = MaterialTheme.typography.bodyMedium)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SetupStep(1, R.string.setup_step_install, done = step > 0, current = step == 0)
                SetupStep(
                    number = 2,
                    title = R.string.setup_step_start,
                    done = step > 1,
                    current = step == 1,
                    hint = R.string.setup_step_start_hint,
                )
                SetupStep(
                    number = 3,
                    title = R.string.setup_step_allow,
                    done = false,
                    current = step == 2,
                    hint = if (status == ShizukuStatus.PERMISSION_DENIED) R.string.setup_step_allow_denied_hint else null,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        when (status) {
                            ShizukuStatus.NOT_INSTALLED -> context.openShizukuListing()
                            ShizukuStatus.NOT_RUNNING, ShizukuStatus.PERMISSION_DENIED -> context.openShizuku()
                            ShizukuStatus.PERMISSION_REQUIRED -> onAllow()
                            ShizukuStatus.READY -> Unit
                        }
                    },
                ) {
                    Text(
                        stringResource(
                            when (status) {
                                ShizukuStatus.NOT_INSTALLED -> R.string.setup_action_install
                                ShizukuStatus.PERMISSION_REQUIRED -> R.string.setup_action_allow
                                else -> R.string.setup_action_open
                            },
                        ),
                    )
                }
                TextButton(onClick = { context.openUrl(ShizukuGateway.SETUP_GUIDE_URL) }) {
                    Text(stringResource(R.string.setup_action_guide))
                }
            }
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    @StringRes title: Int,
    done: Boolean,
    current: Boolean,
    @StringRes hint: Int? = null,
) {
    val colors = MaterialTheme.colorScheme
    val filled = done || current
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = if (filled) colors.primary else Color.Transparent,
            contentColor = if (filled) colors.onPrimary else LocalContentColor.current,
            border = if (filled) null else BorderStroke(1.5.dp, colors.outline),
            modifier = Modifier.size(26.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (done) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                } else {
                    Text(number.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.padding(top = 2.dp)) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (current && hint != null) {
                Text(
                    text = stringResource(hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.8f),
                )
            }
        }
    }
}
