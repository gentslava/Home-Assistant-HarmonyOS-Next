import router from '@system.router';
import getStore from '../../common/store.js';

function ensureStore() {
    const st = getStore();
    if (!st.settings) {
        st.settings = {};
    }

    // defaults
    const s = st.settings;
    if (s.debugMode === undefined) s.debugMode = false;    // off by default: show honest "Offline", not mock data
    if (s.connectionMode === undefined) s.connectionMode = 'local'; // local/cloud (future)
    return st;
}

function colorGray() { return '#5A5A5A'; }
function colorBlue() { return '#2F80ED'; }
function colorGreen() { return '#27AE60'; }

export default {
    data: {
        items: []
    },

    onInit() {
        ensureStore();
        this._buildItems();
    },

    onShow() {
        // when coming back from About or elsewhere
        this._buildItems();
    },

    onSwipe(e) {
        if (!e || !e.direction) return;

        if (e.direction === 'right') {
            router.replace({ uri: 'pages/index/index' });
            return;
        }

        if (e.direction === 'down') {
            // refresh UI from store
            this._buildItems();
            return;
        }
    },

    _buildItems() {
        const store = ensureStore();
        const s = store.settings;

        const debugState = s.debugMode ? 'On (mock data)' : 'Off';
        const connectionState = (s.connectionMode === 'local')
            ? 'Local (cloud soon)'
            : 'Cloud (soon)';

        this.items = [
            {
                id: 'connection',
                name: 'Connection',
                state: connectionState,
                iconSrc: '/common/icons/connection.png',
                color: colorGray(),
                showChevron: false
            },
            {
                id: 'server',
                name: 'Server',
                state: 'Configure on phone',
                iconSrc: '/common/icons/server.png',
                color: colorGray(),
                showChevron: false
            },
            {
                id: 'debug',
                name: 'Debug mode',
                state: debugState,
                iconSrc: '/common/icons/debug.png',
                color: s.debugMode ? colorGreen() : colorGray(),
                showChevron: false
            },
            {
                id: 'about',
                name: 'About',
                state: 'Home Assistant Wearable',
                iconSrc: '/common/icons/info.png',
                color: colorBlue(),
                showChevron: true
            }
        ];
    },

    onItem(item) {
        const id = item && item.id ? String(item.id) : '';
        const store = ensureStore();

        if (id === 'debug') {
            store.settings.debugMode = !store.settings.debugMode;

            // если хочешь, чтобы debugMode влиял на данные на главной:
            // например, при debugMode=false — потом будешь грузить реальное HA через phone.
            this._buildItems();
            return;
        }

        if (id === 'connection') {
            // пока просто переключаем local/cloud (coming soon)
            store.settings.connectionMode = (store.settings.connectionMode === 'local') ? 'cloud' : 'local';
            this._buildItems();
            return;
        }

        if (id === 'about') {
            router.replace({ uri: 'pages/about/index' });
            return;
        }

        // server: nothing yet
    }
};
