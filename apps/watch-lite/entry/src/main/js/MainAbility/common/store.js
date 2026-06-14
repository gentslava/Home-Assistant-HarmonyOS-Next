/*
 * Общий стор приложения.
 *
 * Движок lite-wearable на GT6 — JerryScript (ECMAScript 5.1) — НЕ определяет
 * `globalThis` (это ES2020), поэтому прежний `globalThis.__STORE__` падал с
 * `ReferenceError`. Состояние держим в этом модуле. Локальные импорты на lite
 * официально поддерживаются: `import x from '../../common/x.js'`.
 *
 * На случай, если сборщик lite даёт каждой странице свою копию модуля, мы ещё и
 * "якорим" стор на экземпляре приложения через getApp().data (когда API есть),
 * чтобы все страницы видели один объект. Если getApp() недоступен — работает
 * как модуль-синглтон. В худшем случае (нет ни того, ни другого) — стор на
 * страницу: приложение НЕ падает, просто состояние не шарится между экранами.
 */

function createStore() {
    return {
        statesById: {}, // entityId -> state
        settings: {},
        navEntity: null
    };
}

var localStore = createStore();

export default function getStore() {
    if (typeof getApp === 'function') {
        var app = getApp();
        if (app) {
            if (!app.data) {
                app.data = {};
            }
            if (!app.data.haStore) {
                app.data.haStore = localStore;
            }
            return app.data.haStore;
        }
    }
    return localStore;
}
