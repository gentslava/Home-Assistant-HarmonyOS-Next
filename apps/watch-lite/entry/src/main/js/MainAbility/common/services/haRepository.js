/*
 * Repository over the P2P client: real HA data through the Android companion.
 * ES5.1 only, callbacks (no Promise). Speaks the v1 protocol (docs/p2p-protocol.md).
 *
 * Call initP2p() (from p2pClient.js) once before using these — pages do it in onInit.
 */
import { request } from './p2pClient.js';

/** All entities. onOk(cards[]), onErr(reason). */
export function sync(onOk, onErr) {
    request({ type: 'SYNC_REQUEST' }, {
        onResponse: function (msg) {
            if (msg && msg.type === 'SYNC_RESPONSE') {
                onOk(msg.cards || []);
            } else {
                onOk([]);   // ACK{ok:false} or unexpected -> treat as empty
            }
        },
        onError: onErr || function () {}
    });
}

/** One entity after an action. onOk(card|null), onErr(reason). */
export function syncEntity(entityId, onOk, onErr) {
    request({ type: 'SYNC_ENTITY_REQUEST', entity_id: entityId }, {
        onResponse: function (msg) {
            onOk((msg && msg.type === 'SYNC_ENTITY_RESPONSE') ? (msg.card || null) : null);
        },
        onError: onErr || function () {}
    });
}

/** Invoke a service. action = { domain, service, data }. onOk(), onErr(reason). */
export function callAction(action, onOk, onErr) {
    request({
        type: 'CALL_SERVICE',
        domain: action.domain,
        service: action.service,
        data: action.data || {}
    }, {
        onResponse: function (msg) {
            if (msg && msg.type === 'ACK' && msg.ok === false) {
                (onErr || function () {})(msg.error || 'failed');
            } else {
                (onOk || function () {})();
            }
        },
        onError: onErr || function () {}
    });
}
