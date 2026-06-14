import router from '@system.router';
import getStore from '../../common/store.js';

function ensureStore() {
    const st = getStore();
    if (!st.statesById) {
        st.statesById = {};
    }
    return st;
}

function safeNavEntity() {
    const nav = getStore().navEntity;
    if (nav) return nav;
    return {};
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

        // Берём state из store, если есть
        const store = ensureStore();
        const saved = store.statesById[this.entityId];
        this.state = saved !== undefined ? saved : (p.state ? String(p.state) : '—');

        this._rebuildActions();
    },

    onSwipe(e) {
        if (!e || !e.direction) return;

        if (e.direction === 'right') {
            router.replace({ uri: 'pages/index/index' });
            return;
        }

        if (e.direction === 'down') {
            this.refresh();
            return;
        }
    },

    refresh() {
        // TODO: P2P -> phone -> HA -> state
        console.log('refresh entity ' + this.entityId);
    },

    _rebuildActions() {
        const ICON_POWER = '/common/icons/power.png';
        const ICON_LOCK = '/common/icons/lock.png';
        const st = String(this.state || '');

        if (this.domain === 'light' || this.domain === 'switch') {
            if (st === 'on') {
                this.actions = [
                    { id: 'turn_off', name: 'Turn off', state: 'Switch state', iconSrc: ICON_POWER, color: actionColorBlue() }
                ];
                return;
            }
            if (st === 'off') {
                this.actions = [
                    { id: 'turn_on', name: 'Turn on', state: 'Switch state', iconSrc: ICON_POWER, color: actionColorGreen() }
                ];
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
                this.actions = [
                    { id: 'unlock', name: 'Unlock', state: 'Change state', iconSrc: ICON_LOCK, color: actionColorGreen() }
                ];
                return;
            }
            if (st === 'unlocked') {
                this.actions = [
                    { id: 'lock', name: 'Lock', state: 'Change state', iconSrc: ICON_LOCK, color: actionColorRed() }
                ];
                return;
            }
            this.actions = [
                { id: 'lock', name: 'Lock', state: 'Change state', iconSrc: ICON_LOCK, color: actionColorRed() },
                { id: 'unlock', name: 'Unlock', state: 'Change state', iconSrc: ICON_LOCK, color: actionColorGreen() }
            ];
            return;
        }

        this.actions = [
            { id: 'toggle', name: 'Toggle', state: 'Call service', iconSrc: ICON_POWER, color: actionColorGray() }
        ];
    },

    onAction(action) {
        if (this.busy) return;
        this.busy = true;

        const id = action && action.id ? String(action.id) : '';
        const self = this;

        // optimistic next state
        let nextState = this.state;

        if (this.domain === 'light' || this.domain === 'switch') {
            if (id === 'turn_on') nextState = 'on';
            if (id === 'turn_off') nextState = 'off';
        } else if (this.domain === 'lock') {
            if (id === 'lock') nextState = 'locked';
            if (id === 'unlock') nextState = 'unlocked';
        } else if (id === 'toggle') {
            nextState = (String(this.state) === 'on') ? 'off' : 'on';
        }

        // 1) обновляем локально
        this.state = nextState;

        // 2) обновляем STORE (чтобы main экран тоже знал)
        const store = ensureStore();
        store.statesById[this.entityId] = nextState;

        // 3) обновляем NAV cache
        store.navEntity = {
            entityId: this.entityId,
            name: this.name,
            state: nextState,
            iconSrc: this.iconSrc
        };

        // 4) deferred rebuild (Lite-safe)
        setTimeout(function () {
            self._rebuildActions();
            setTimeout(function () {
                self.busy = false;
            }, 220);
        }, 0);
    }
};
