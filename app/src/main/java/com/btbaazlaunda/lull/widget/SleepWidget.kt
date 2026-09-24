package com.btbaazlaunda.lull.widget

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.btbaazlaunda.lull.R
import com.btbaazlaunda.lull.appContainer
import com.btbaazlaunda.lull.ui.describe
import com.btbaazlaunda.lull.ui.theme.DarkColors
import com.btbaazlaunda.lull.ui.theme.LightColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** One-tap home screen switch: an icon when small, icon and status when wider. */
class SleepWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(Small, Wide))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val store = context.appContainer.store
        val initial = store.state.first()
        provideContent {
            val state by store.state.collectAsState(initial)
            GlanceTheme(colors = WidgetColors) {
                SleepWidgetContent(sleeping = state.sleeping)
            }
        }
    }

    private companion object {
        val Small = DpSize(48.dp, 48.dp)
        val Wide = DpSize(140.dp, 48.dp)
        val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)
    }

    @Composable
    private fun SleepWidgetContent(sleeping: Boolean) {
        val context = LocalContext.current
        val colors = GlanceTheme.colors
        val background = if (sleeping) colors.primary else colors.widgetBackground
        val content = if (sleeping) colors.onPrimary else colors.onSurface
        val wide = LocalSize.current.width >= Wide.width
        val status = context.getString(if (sleeping) R.string.widget_status_on else R.string.widget_status_off)
        val title = context.getString(R.string.widget_title)

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .cornerRadius(24.dp)
                .background(background)
                .clickable(actionRunCallback<ToggleSleepAction>())
                .padding(horizontal = 16.dp)
                .semantics { contentDescription = "$title, $status" },
            horizontalAlignment = if (wide) Alignment.Start else Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(if (sleeping) R.drawable.ic_moon else R.drawable.ic_sun),
                contentDescription = null,
                colorFilter = ColorFilter.tint(content),
                modifier = GlanceModifier.size(28.dp),
            )
            if (wide) {
                Spacer(GlanceModifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = TextStyle(color = content, fontSize = 15.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                    Text(text = status, style = TextStyle(color = content, fontSize = 12.sp), maxLines = 1)
                }
            }
        }
    }
}

class SleepWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SleepWidget()
}

class ToggleSleepAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val message = context.appContainer.controller.toggle().describe(context) ?: return
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
