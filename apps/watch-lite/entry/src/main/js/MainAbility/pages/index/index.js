import router from '@system.router';
import app from '@system.app';
import getStore from '../../common/store.js';

function ensureStore() {
    const s = getStore();
    if (!s.statesById) {
        s.statesById = {};
    }
    return s;
}

export default {
    data: {
        items: []
    },

    onInit() {
        ensureStore();

        // твои моки
        this.items = [
            { id: 'light.1',  name: 'Light 1',  state: 'on',  iconSrc: '/common/icons/light.png',  color: '#F2C94C' },
            { id: 'light.2',  name: 'Light 2',  state: 'off', iconSrc: '/common/icons/light.png',  color: '#F2C94C' },
            { id: 'lock.3',   name: 'Lock 3',   state: 'locked', iconSrc: '/common/icons/lock.png', color: '#8E44AD' },
            { id: 'switch.4', name: 'Switch 4', state: 'off', iconSrc: '/common/icons/switch.png', color: '#27AE60' }
        ];

        this._applyStoreStates();
    },

    // ВАЖНО: на Lite это срабатывает при возврате через replace/back
    onShow() {
        this._applyStoreStates();
    },

    _applyStoreStates() {
        const store = ensureStore();
        const map = store.statesById || {};

        // Обновляем state у items из store (без изменения структуры)
        for (let i = 0; i < this.items.length; i++) {
            const id = this.items[i].id;
            if (map[id] !== undefined) {
                this.items[i].state = map[id];
            }
        }

        // Иногда Lite не перерисовывает после мутации элементов массива.
        // Поэтому меняем ссылку на массив, чтобы гарантировать ререндер:
        this.items = this.items.slice(0);
    },

    openEntity(item) {
        const store = ensureStore();
        const saved = store.statesById[item.id];

        // то, что entity экран читает
        store.navEntity = {
            entityId: item.id,
            name: item.name,
            state: saved !== undefined ? saved : item.state,
            iconSrc: item.iconSrc
        };

        router.replace({ uri: 'pages/entity/index' });
    },

    onSwipe(event) {
        if (!event || !event.direction) return;

        // вправо — закрыть
        if (event.direction === 'right') {
            app.terminate();
            return;
        }

        // вниз — refresh
        if (event.direction === 'down') {
            this.refresh();
            return;
        }
    },

    openSettings() {
        router.replace({ uri: 'pages/settings/index' });
    }
};
