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

    private var device: Device? = null

    private val receiver = Receiver { message: Message ->
        if (message.type != Message.MESSAGE_TYPE_DATA) return@Receiver
        val request = String(message.data, StandardCharsets.UTF_8)
        scope.launch {
            val reply = runCatching { bridge.handle(request) }.getOrNull() ?: return@launch
            sendRaw(reply)
        }
    }

    /** Request permission, find the connected watch, and start receiving. */
    fun start() {
        authClient.requestPermission(object : AuthCallback {
            override fun onOk(permissions: Array<out Permission>) = findDeviceAndRegister()
            override fun onCancel() { Log.e(TAG, "Wear Engine permission cancelled") }
        }, Permission.DEVICE_MANAGER)
    }

    fun stop() {
        p2pClient.unregisterReceiver(receiver)
            .addOnFailureListener { logErr("unregisterReceiver", it) }
        scope.cancel()
    }

    private fun findDeviceAndRegister() {
        deviceClient.getBondedDevices()
            .addOnSuccessListener { devices ->
                device = devices.firstOrNull { it.isConnected }
                val d = device
                if (d == null) {
                    Log.w(TAG, "no connected watch")
                    return@addOnSuccessListener
                }
                p2pClient.registerReceiver(d, receiver)
                    .addOnSuccessListener { Log.i(TAG, "P2P receiver registered") }
                    .addOnFailureListener { logErr("registerReceiver", it) }
            }
            .addOnFailureListener { logErr("getBondedDevices", it) }
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

        // The watch app this companion pairs with. bundleName is fixed; fingerprint must match the
        // watch app's signing cert — fill it in (see README, Phase 1e).
        const val PEER_PKG = "ru.gentslava.homeassistant"
        const val PEER_FINGERPRINT = "DF21A3C09F7954579305F85C64F80CAD86F79853EE3A887C1DEC95D218DF3A37"
    }
}
