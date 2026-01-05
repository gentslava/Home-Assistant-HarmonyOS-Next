import UIAbility from '@ohos.app.ability.UIAbility';
import window from '@ohos.window';
import { AppStorageV2 } from '@ohos.arkui.StateManagement';

import { AppKeys } from '../app/AppKeys';
import { HomeAssistantStore } from '../presentation/store/HomeAssistantStore';
import { Services } from '../app/Services';

export default class EntryAbility extends UIAbility {
  async onWindowStageCreate(windowStage: window.WindowStage): Promise<void> {
    AppStorageV2.connect(HomeAssistantStore, AppKeys.STORE_KEY, () => new HomeAssistantStore());

    try {
      await windowStage.loadContent('pages/Index');
    } catch (error) {
      // TODO: Implement error handling.
    }

    Services.initWithFallback(this.context);
  }
}
