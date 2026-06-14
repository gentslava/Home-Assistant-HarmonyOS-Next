export default class MockHomeAssistantRepository {
    sync() {
        // Возвращаем Promise как в настоящем репозитории
        return Promise.resolve([
            {
                entity_id: 'light.kitchen',
                name: 'Kitchen',
                state: 'off',
                domain: 'light'
            },
            {
                entity_id: 'switch.router',
                name: 'Router',
                state: 'on',
                domain: 'switch'
            },
            {
                entity_id: 'lock.front_door',
                name: 'Front door',
                state: 'locked',
                domain: 'lock'
            }
        ]);
    }

    runAction(action) {
        // Всегда успешно
        return Promise.resolve(true);
    }
}
