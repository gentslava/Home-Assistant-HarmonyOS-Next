import router from '@system.router';
import getStore from '../../common/store.js';
import { callAction, syncEntity } from '../../common/services/haRepository.js';

function ensureStore() {
    const st = getStore();
    if (!st.statesById) st.statesById = {};
    return st;
}

function safeNavEntity() {
    const nav = getStore().navEntity;
    return nav || {};
}

function domainFromEntityId(entityId) {
    if (!entityId) return '';
    const s = String(entityId);
    const idx = s.indexOf('.');
    return idx > 0 ? s.slice(0, idx) : '';
}

function actionColorBlue() { return '#2F80ED'; }
function actionColorGreen() { return '#27AE60'; }
function actionColorGray() { return '#5A5A5A'; }
function actionColorRed() { return '#EB5757'; }

export default {
    data: {
        entityId: '',
        name: 'Entity',
        state: '—',
        iconSrc: '/common/icons/unknown.png',
        domain: '',
        statusText: '',
        actions: [],
        busy: false
    },

    onInit() {
        ensureStore();
        const p = safeNavEntity();

        this.entityId = p.entityId ? String(p.entityId) : '';
        this.name = p.name ? String(p.name) : 'Entity';
        this.iconSrc = p.iconSrc ? String(p.iconSrc) : '/common/icons/unknown.png';
        this.domain = domainFromEntityId(this.entityId);

        const store = ensureStore();
        const saved = store.statesById[this.entityId];
        this.state = saved !== undefined ? saved : (p.state ? String(p.state) : '—');

        this._rebuildActions();
    },

    onSwipe(e) {
        if (!e || !e.direction) return;
        if (e.direction === 'right') { router.replace({ uri: 'pages/index/index' }); return; }
        if (e.direction === 'down') { this.refresh(); return; }
    },

    // Pull the real current state for this entity from HA (via the companion).
    refresh() {
        const self = this;
        if (!this.entityId) return;
        syncEntity(this.entityId, function (card) {
            if (!card) return;
            self.state = card.state;
            ensureStore().statesById[self.entityId] = card.state;
            self._rebuildActions();
        }, function () { /* offline: keep last known */ });
    },

    _rebuildActions() {
        const ICON_POWER = '/common/icons/power.png';
        const ICON_LOCK = '/common/icons/lock.png';
        const st = String(this.state || '');

        if (this.domain === 'light' || this.domain === 'switch') {
            if (st === 'on') {
                this.actions = [{ id: 'turn_off', name: 'Turn off', state: 'Switch state', iconSrc: ICON_POWER, color: actionColorBlue() }];
                return;
            }
            if (st === 'off') {
                this.actions = [{ id: 'turn_on', name: 'Turn on', state: 'Switch state', iconSrc: ICON_POWER, color: actionColorGreen() }];
                return;
            }
            this.actions = [
                { id: 'turn_off', name: 'Turn off', state: 'Switch state', iconSrc: ICON_POWER, color: actionColorBlue() },
                { id: 'turn_on', name: 'Turn on', state: 'Switch state', iconSrc: ICON_POWER, color: actionColorGreen() }
            ];
            return;
        }

        if (this.domain === 'lock') {
            if (st === 'locked') {
                this.actions = [{ id: 'unlock', name: 'Unlock', state: 'Change state', iconSrc: ICON_LOCK, color: actionColorGreen() }];
                return;
            }
            if (st === 'unlocked') {
                this.actions = [{ id: 'lock', name: 'Lock', state: 'Change state', iconSrc: ICON_LOCK, color: actionColorRed() }];
                return;
            }
            this.actions = [
                { id: 'lock', name: 'Lock', state: 'Change state', iconSrc: ICON_LOCK, color: actionColorRed() },
                { id: 'unlock', name: 'Unlock', state: 'Change state', iconSrc: ICON_LOCK, color: actionColorGreen() }
            ];
            return;
        }

        this.actions = [{ id: 'toggle', name: 'Toggle', state: 'Call service', iconSrc: ICON_POWER, color: actionColorGray() }];
    },

    onAction(action) {
        if (this.busy) return;
        this.busy = true;
        this.statusText = '';

        const self = this;
        const service = action && action.id ? String(action.id) : '';
        const prevState = this.state;

        // 1) optimistic local + store update (so the list reflects it immediately)
        let nextState = this.state;
        if (this.domain === 'light' || this.domain === 'switch') {
            if (service === 'turn_on') nextState = 'on';
            if (service === 'turn_off') nextState = 'off';
            if (service === 'toggle') nextState = (String(this.state) === 'on') ? 'off' : 'on';
        } else if (this.domain === 'lock') {
            if (service === 'lock') nextState = 'locked';
            if (service === 'unlock') nextState = 'unlocked';
        }
        this._setState(nextState);

        // 2) call HA via the companion; confirm or roll back
        callAction(
            { domain: this.domain, service: service, data: { entity_id: this.entityId } },
            function () {
                syncEntity(self.entityId, function (card) {
                    if (card) self._setState(card.state);
                    self._finish();
                }, function () { self._finish(); });
            },
            function (reason) {
                self._setState(prevState);            // roll back optimistic
                self.statusText = 'Action failed';
                self._finish();
            }
        );
    },

    _setState(next) {
        this.state = next;
        const store = ensureStore();
        store.statesById[this.entityId] = next;
        store.navEntity = {
            entityId: this.entityId,
            name: this.name,
            state: next,
            iconSrc: this.iconSrc,
            domain: this.domain
        };
    },

    _finish() {
        const self = this;
        setTimeout(function () {
            self._rebuildActions();
            setTimeout(function () { self.busy = false; }, 220);
        }, 0);
    }
};
