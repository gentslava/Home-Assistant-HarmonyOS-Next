import router from '@system.router';
import app from '@system.app';
import getStore from '../../common/store.js';
import { initP2p } from '../../common/services/p2pClient.js';
import { sync } from '../../common/services/haRepository.js';
import { cardsToItems } from '../../common/constants/domains.js';

function ensureStore() {
    var s = getStore();
    if (!s.statesById) s.statesById = {};
    if (!s.settings) s.settings = {};
    return s;
}

// Offline/dev fallback — kept working per apps/watch-lite/AGENTS.md (mock path).
function mockItems() {
    return [
        { id: 'light.kitchen', name: 'Kitchen', state: 'off', domain: 'light', iconSrc: '/common/icons/light.png', color: '#F2C94C', primary: null, secondary: [] },
        { id: 'switch.router', name: 'Router', state: 'on', domain: 'switch', iconSrc: '/common/icons/switch.png', color: '#27AE60', primary: null, secondary: [] },
        { id: 'lock.front_door', name: 'Front door', state: 'locked', domain: 'lock', iconSrc: '/common/icons/lock.png', color: '#8E44AD', primary: null, secondary: [] }
    ];
}

export default {
    data: {
        items: [],
        statusText: ''
    },

    onInit() {
        ensureStore();
        initP2p();
        this.loadEntities();
    },

    // On Lite this fires when returning via replace/back — re-apply optimistic states.
    onShow() {
        this._applyStoreStates();
    },

    loadEntities() {
        var self = this;
        self.statusText = 'Loading…';
        self.items = [];
        sync(function (cards) {
            var items = cardsToItems(cards);
            self._mergeStoreStates(items);
            self.items = items;
            self.statusText = items.length ? '' : 'No entities';
        }, function () {
            // No companion reachable. Show offline, or mock if debug mode is on.
            var store = ensureStore();
            if (store.settings.debugMode) {
                self.items = mockItems();
                self.statusText = 'Mock (offline)';
            } else {
                self.items = [];
                self.statusText = 'Offline — open the phone app';
            }
        });
    },

    refresh() {
        this.loadEntities();
    },

    _mergeStoreStates(items) {
        var map = ensureStore().statesById || {};
        for (var i = 0; i < items.length; i++) {
            if (map[items[i].id] !== undefined) items[i].state = map[items[i].id];
        }
    },

    _applyStoreStates() {
        var map = ensureStore().statesById || {};
        for (var i = 0; i < this.items.length; i++) {
            var id = this.items[i].id;
            if (map[id] !== undefined) this.items[i].state = map[id];
        }
        // Lite sometimes skips re-render after array mutation — reassign the reference.
        this.items = this.items.slice(0);
    },

    openEntity(item) {
        var store = ensureStore();
        var saved = store.statesById[item.id];
        store.navEntity = {
            entityId: item.id,
            name: item.name,
            state: saved !== undefined ? saved : item.state,
            iconSrc: item.iconSrc,
            domain: item.domain,
            primary: item.primary,
            secondary: item.secondary
        };
        router.replace({ uri: 'pages/entity/index' });
    },

    onSwipe(event) {
        if (!event || !event.direction) return;
        if (event.direction === 'right') { app.terminate(); return; }
        if (event.direction === 'down') { this.refresh(); return; }
    },

    openSettings() {
        router.replace({ uri: 'pages/settings/index' });
    }
};
