package ru.gentslava.homeassistant.companion.p2p

import android.content.Context
import android.util.Log
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.WearEngineException
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.Message
import com.huawei.wearengine.p2p.Receiver
import com.huawei.wearengine.p2p.SendCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.gentslava.homeassistant.companion.bridge.HaBridge
import java.nio.charset.StandardCharsets

/**
 * Wear Engine P2P transport for the companion (HMS SDK). Receives JSON requests from the watch app,
 * hands them to [HaBridge], and sends the JSON reply back. The bridge does all HA logic — this class
 * only moves bytes.
 *
 * Requires AppGallery Connect setup (Wear Engine enabled + the companion's SHA-256 fingerprint) and
 * the watch app's [PEER_PKG] + [PEER_FINGERPRINT] in the allow-list. See app README, Phase 1e.
 */
class WearEngineP2pService(
    context: Context,
    private val bridge: HaBridge,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val authClient = HiWear.getAuthClient(context)
    private val deviceClient = HiWear.getDeviceClient(context)
    private val p2pClient = HiWear.getP2pClient(context).apply {
        setPeerPkgName(PEER_PKG)
        setPeerFingerPrint(PEER_FINGERPRINT)
    }

    // Written from HMS callbacks, read from send coroutines on Dispatchers.Default — needs a
    // memory barrier so sendRaw never reads a stale null after the device is found.
    @Volatile
    private var device: Device? = null

    @Volatile
    private var registered = false

    @Volatile
    private var running = false

    // Backoff for the reconnect loop so a watch that's away doesn't cause 5s CPU wakeups forever.
    @Volatile
    private var retryDelayMs = RETRY_MIN_MS

    private val receiver = Receiver { message: Message ->
        if (message.type != Message.MESSAGE_TYPE_DATA) return@Receiver
        val request = String(message.data, StandardCharsets.UTF_8)
        scope.launch {
            val reply = runCatching { bridge.handle(request) }.getOrNull() ?: return@launch
            sendRaw(reply)
        }
    }

    /** Request permission, then keep a receiver registered on the connected watch (idempotent). */
    fun start() {
        if (running) return
        running = true
        Log.i(TAG, "start: requesting Wear Engine permission")
        authClient.requestPermission(object : AuthCallback {
            override fun onOk(permissions: Array<out Permission>) {
                Log.i(TAG, "permission granted: ${permissions.joinToString { it.name }}")
                ensureRegistered()
            }
            override fun onCancel() { Log.e(TAG, "Wear Engine permission cancelled") }
        }, Permission.DEVICE_MANAGER)
            .addOnSuccessListener { Log.i(TAG, "requestPermission task accepted") }
            .addOnFailureListener { logErr("requestPermission", it) }
    }

    fun stop() {
        running = false
        registered = false
        device = null
        p2pClient.unregisterReceiver(receiver)
            .addOnFailureListener { logErr("unregisterReceiver", it) }
        scope.cancel()
    }

    /**
     * Resolve the connected watch and (re)register the receiver. Re-runnable: handles the watch
     * connecting after start, dropping, or reconnecting on a new [Device]. Retries with a fixed
     * backoff while no connected watch is found or registration fails.
     */
    private fun ensureRegistered() {
        if (!running) return
        deviceClient.getBondedDevices()
            .addOnSuccessListener { devices ->
                val connected = devices.firstOrNull { it.isConnected }
                if (connected == null) {
                    Log.w(TAG, "no connected watch; retry in ${retryDelayMs}ms")
                    device = null
                    registered = false
                    scheduleRetry()
                    return@addOnSuccessListener
                }
                if (connected != device || !registered) {
                    device = connected
                    p2pClient.registerReceiver(connected, receiver)
                        .addOnSuccessListener {
                            registered = true
                            retryDelayMs = RETRY_MIN_MS // connected — reset backoff
                            Log.i(TAG, "P2P receiver registered on ${connected.name}")
                        }
                        .addOnFailureListener {
                            registered = false
                            logErr("registerReceiver", it)
                            scheduleRetry()
                        }
                }
            }
            .addOnFailureListener {
                logErr("getBondedDevices", it)
                scheduleRetry()
            }
    }

    private fun scheduleRetry() {
        if (!running) return
        val d = retryDelayMs
        retryDelayMs = (retryDelayMs * 2).coerceAtMost(RETRY_MAX_MS) // exponential backoff
        scope.launch {
            delay(d)
            ensureRegistered()
        }
    }

    private fun sendRaw(json: String) {
        val d = device ?: return
        val message = Message.Builder()
            .setPayload(json.toByteArray(StandardCharsets.UTF_8))
            .build()
        p2pClient.send(d, message, object : SendCallback {
            override fun onSendResult(resultCode: Int) {
                if (resultCode != SEND_SUCCESS) Log.w(TAG, "send result=$resultCode")
            }
            override fun onSendProgress(progress: Long) = Unit
        }).addOnFailureListener { logErr("send", it) }
    }

    private fun logErr(op: String, e: Throwable) {
        val code = (e as? WearEngineException)?.errorCode
        Log.e(TAG, "$op failed (code=$code)", e)
    }

    private companion object {
        const val TAG = "WearEngineP2p"
        const val SEND_SUCCESS = 207
        const val RETRY_MIN_MS = 5_000L  // first reconnect attempt delay
        const val RETRY_MAX_MS = 60_000L // backoff ceiling — at most one wakeup per minute when away

        // The watch app this companion pairs with. bundleName is fixed; fingerprint must match the
        // watch app's signing cert — fill it in (see README, Phase 1e).
        const val PEER_PKG = "ru.gentslava.homeassistant"
        const val PEER_FINGERPRINT = "DF21A3C09F7954579305F85C64F80CAD86F79853EE3A887C1DEC95D218DF3A37"
    }
}
