package ru.gentslava.homeassistant.companion.p2p

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import ru.gentslava.homeassistant.companion.bridge.HaBridge
import ru.gentslava.homeassistant.companion.ha.HaClient
import ru.gentslava.homeassistant.companion.ha.HaConfig

/**
 * Foreground service that owns the Wear Engine P2P bridge.
 *
 * Hosting the transport here (not in the Activity) means it survives Activity recreation
 * (rotation, theme change) and keeps the watch reachable while the app is backgrounded — the
 * companion's whole job is to be an always-available bridge. Start it once HA is configured;
 * [WearEngineP2pService] then keeps a receiver registered and reconnects on its own.
 */
class HaBridgeService : Service() {
    private var p2p: WearEngineP2pService? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
        val bridge = HaBridge(HaClient(HaConfig(this)))
        p2p = WearEngineP2pService(this, bridge).also { it.start() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        p2p?.stop()
        p2p = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Home Assistant bridge",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Keeps your watch connected to Home Assistant" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Home Assistant")
            .setContentText("Bridging your watch to Home Assistant")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "ha_bridge"
        private const val NOTIF_ID = 1

        /** Start (or no-op if already running) the bridge service. Safe to call repeatedly. */
        fun start(context: Context) {
            val intent = Intent(context, HaBridgeService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, HaBridgeService::class.java))
        }
    }
}
