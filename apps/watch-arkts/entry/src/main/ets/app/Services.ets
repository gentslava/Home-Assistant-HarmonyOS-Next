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
  private static ctx: common.UIAbilityContext | null = null;

  static async initWithFallback(ctx: common.UIAbilityContext): Promise<void> {
    Services.ctx = ctx;
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

  /**
   * If we're currently on the Mock repo (real P2P wasn't available at startup), try to (re)connect
   * the real repo. Returns true if it upgraded. This is what stops the app from being stuck on Mock
   * fake data forever once the phone/companion becomes reachable — called before each sync.
   */
  static async tryUpgradeToReal(): Promise<boolean> {
    if (!(Services.repo instanceof MockHomeAssistantRepository)) return false;
    if (!Services.ctx) return false;
    const real: HomeAssistantRepository | null =
      await Services.tryInitRealRepo(Services.ctx, 1200).catch(() => null);
    if (real) {
      Services.repo = real;
      return true;
    }
    return false;
  }

  private static async tryInitRealRepo(ctx: common.UIAbilityContext, timeoutMs: number): Promise<HomeAssistantRepository> {
    const p2p: WearEngineP2pClient = new WearEngineP2pClient(ctx);

    // Companion (Android) bundle + signing fingerprint. Fill the fingerprint from AppGallery Connect.
    // Until it's real, hasConnectedPeer()/init() fail and Services falls back to Mock (by design).
    p2p.setRemoteApp('ru.gentslava.homeassistant.companion', 'AE3A8E87933A4B3AC5BEE17C694F0A0B5FA7A515AB2261D4DA8B1F619A664FA1');

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
