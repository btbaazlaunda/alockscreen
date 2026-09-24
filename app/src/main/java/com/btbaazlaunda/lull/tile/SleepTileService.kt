package com.btbaazlaunda.lull.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.btbaazlaunda.lull.R
import com.btbaazlaunda.lull.appContainer
import com.btbaazlaunda.lull.core.SleepState
import com.btbaazlaunda.lull.core.ToggleOutcome
import com.btbaazlaunda.lull.ui.MainActivity
import com.btbaazlaunda.lull.ui.describe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SleepTileService : TileService() {

    private val scope = MainScope()
    private var observer: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        observer = scope.launch { appContainer.store.state.collect(::render) }
    }

    override fun onStopListening() {
        observer?.cancel()
        super.onStopListening()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        val container = appContainer
        container.appScope.launch {
            val outcome = container.controller.toggle()
            withContext(Dispatchers.Main) {
                if (outcome == ToggleOutcome.SetupRequired && openApp()) return@withContext
                outcome.describe(this@SleepTileService)?.let {
                    Toast.makeText(this@SleepTileService, it, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun render(state: SleepState) {
        val tile = qsTile ?: return
        val status = getString(if (state.sleeping) R.string.state_on else R.string.state_off)
        tile.state = if (state.sleeping) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        tile.subtitle = status
        tile.stateDescription = status
        tile.updateTile()
    }

    /** Collapses the shade and opens Lull so the user can finish setup. */
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp(): Boolean = runCatching {
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }.isSuccess
}
