/*
 * Wear Engine P2P client (lite wearable, FA, ES5.1 / JerryScript — callbacks only).
 *
 * Sends JSON requests to the Android companion and resolves replies by `id` (see
 * docs/p2p-protocol.md). No Promise/async on JerryScript — pure callbacks.
 *
 * Setup: the companion's package + signing fingerprint must match here AND be allow-listed in
 * config.json (metaData.customizeData "supportLists"). Fill PHONE_FINGERPRINT below.
 *
 * The SDK (wearenginesdk/wearengine.js) is the official lite-wearable Wear Engine wrapper; it is
 * only present on a real watch runtime (not the DevEco previewer).
 */
import { P2pClient, Message, Builder } from '../wearenginesdk/wearengine.js';

var PHONE_PKG = 'ru.gentslava.homeassistant.companion';
var PHONE_FINGERPRINT = 'PUT_COMPANION_FINGERPRINT_HERE';
var PROTOCOL_VERSION = 1;

var p2p = new P2pClient();
var pending = {};   // id -> { onResponse, onError, timer }
var seq = 0;
var registered = false;

function nextId(type) {
    seq = (seq + 1) % 1000000;
    return (type || 'req').toLowerCase() + '_' + Date.now() + '_' + seq;
}

function settle(id, key, arg) {
    var p = pending[id];
    if (!p) return;
    if (p.timer) clearTimeout(p.timer);
    delete pending[id];
    p[key](arg);
}

/** Call once (e.g. in page onInit) before request(). */
export function initP2p() {
    p2p.setPeerPkgName(PHONE_PKG);
    p2p.setPeerFingerPrint(PHONE_FINGERPRINT);
    if (registered) return;
    p2p.registerReceiver({
        onSuccess: function () { registered = true; },
        onFailure: function () { registered = false; },
        onReceiveMessage: function (data) {
            if (data && data.isFileType) return;
            var text = (typeof data === 'string') ? data : (data && data.message);
            if (!text) return;
            var msg;
            try { msg = JSON.parse(text); } catch (e) { return; }
            if (msg && msg.id && pending[msg.id]) settle(msg.id, 'onResponse', msg);
        }
    });
}

/**
 * Send a request and wait for the reply with the same id.
 * payload: { type, ...fields }; v + id are added here.
 * callbacks: { onResponse(replyObj), onError(reason) }.
 */
export function request(payload, callbacks, timeoutMs) {
    var id = nextId(payload && payload.type);
    payload.v = PROTOCOL_VERSION;
    payload.id = id;

    pending[id] = {
        onResponse: (callbacks && callbacks.onResponse) || function () {},
        onError: (callbacks && callbacks.onError) || function () {},
        timer: setTimeout(function () { settle(id, 'onError', 'timeout'); }, timeoutMs || 8000)
    };

    var builder = new Builder();
    builder.setDescription(JSON.stringify(payload));   // UTF-8 JSON string
    var message = new Message();
    message.builder = builder;                         // manual assignment (per SDK demos)

    p2p.send(message, {
        onSuccess: function () {},
        onFailure: function () { settle(id, 'onError', 'send_failed'); },
        onSendResult: function (rc) { if (rc && rc.code !== 207) settle(id, 'onError', 'result_' + rc.code); },
        onSendProgress: function () {}
    });
    return id;
}

/** Probe whether the companion app is installed on the phone. cb(code): 205 = yes, 204 = no. */
export function ping(cb) {
    p2p.ping({
        onSuccess: function () {},
        onFailure: function () { cb(204); },
        onPingResult: function (rc) { cb(rc && rc.code); }
    });
}

export function disposeP2p() {
    p2p.unregisterReceiver({ onSuccess: function () { registered = false; } });
}
