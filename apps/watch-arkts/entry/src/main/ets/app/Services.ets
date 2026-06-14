import { common } from '@kit.AbilityKit';
import { AppStorageV2 } from '@ohos.arkui.StateManagement';

import { AppKeys } from './AppKeys';
import { HomeAssistantStore } from '../presentation/store/HomeAssistantStore';

import { WearEngineP2pClient } from '../data/p2p/WearEngineP2pClient';
import { P2pHomeAssistantRepository } from '../data/repository/P2pHomeAssistantRepository';
import { MockHomeAssistantRepository } from '../data/repository/MockHomeAssistantRepository';
import { HomeAssistantRepository } from '../domain/repository/HomeAssistantRepository';

export class Services {
  static repo: HomeAssistantRepository | null = null;

  static async initWithFallback(ctx: common.UIAbilityContext): Promise<void> {
    // Не даём инициализации повиснуть на эмуляторе
    // const repo: HomeAssistantRepository = new MockHomeAssistantRepository();
    const repo: HomeAssistantRepository = await Services.tryInitRealRepo(ctx, 1200)
      .catch(() => new MockHomeAssistantRepository());

    Services.repo = repo;

    // Обновим UI сразу после выбора репозитория
    const store: HomeAssistantStore =
      AppStorageV2.connect(HomeAssistantStore, AppKeys.STORE_KEY, () => new HomeAssistantStore())!;
    await store.sync();
  }

  private static async tryInitRealRepo(ctx: common.UIAbilityContext, timeoutMs: number): Promise<HomeAssistantRepository> {
    const p2p: WearEngineP2pClient = new WearEngineP2pClient(ctx);

    // ВАЖНО: тут должны быть ваши реальные значения (bundleName/fingerprint)
    // p2p.setRemoteApp('com.yourcompany.ha.bridge', 'YOUR_FINGERPRINT');

    await p2p.init();

    // если нет подключённых устройств — сразу падаем в мок
    const hasPeer: boolean = await Services.withTimeout<boolean>(p2p.hasConnectedPeer(), timeoutMs, false);
    if (!hasPeer) {
      throw new Error('No connected peer device');
    }

    const repo: P2pHomeAssistantRepository = new P2pHomeAssistantRepository(p2p);
    await Services.withTimeout<void>(repo.init(), timeoutMs, undefined);

    return repo;
  }

  private static async withTimeout<T>(p: Promise<T>, timeoutMs: number, fallback: T): Promise<T> {
    return await new Promise<T>((resolve: (v: T) => void) => {
      const tid: number = setTimeout(() => resolve(fallback), timeoutMs);
      p.then((v: T) => {
        clearTimeout(tid);
        resolve(v);
      }).catch(() => {
        clearTimeout(tid);
        resolve(fallback);
      });
    });
  }
}
