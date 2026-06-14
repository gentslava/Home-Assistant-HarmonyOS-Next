import { AppStorageV2 } from '@ohos.arkui.StateManagement';
import router from '@ohos.router';

import { AppKeys } from '../../../app/AppKeys';
import { HomeAssistantStore } from '../../store/HomeAssistantStore';
import { EntityCard, EntityAction } from '../../../domain/model/EntityCard';

// дефолты для @Param (без !)
class EmptyAction implements EntityAction {
  kind: 'SERVICE';
  label: string;
  domain: string;
  service: string;
  data: Record<string, string>;

  constructor() {
    this.kind = 'SERVICE';
    this.label = '-';
    this.domain = '';
    this.service = '';
    const d: Record<string, string> = {};
    d['entity_id'] = '';
    this.data = d;
  }
}

class EmptyCard implements EntityCard {
  entity_id: string;
  domain: string;
  name: string;
  state: string;
  icon?: string;
  primary: EntityAction;
  secondary: EntityAction[];

  constructor() {
    this.entity_id = '';
    this.domain = '';
    this.name = '';
    this.state = '';
    this.primary = new EmptyAction();
    this.secondary = [];
  }
}

// RouterOptions классом (без object-literal)
class EntityDetailsParams {
  entity_id: string;
  constructor(id: string) { this.entity_id = id; }
}

class RouteOptionEntityDetails implements router.RouterOptions {
  url: string;
  params?: EntityDetailsParams;

  constructor(id: string) {
    this.url = 'pages/EntityDetails';
    this.params = new EntityDetailsParams(id);
  }
}

@ComponentV2
export struct EntityRow {
  @Param card: EntityCard = new EmptyCard();

  private getStore(): HomeAssistantStore {
    return AppStorageV2.connect(HomeAssistantStore, AppKeys.STORE_KEY, () => new HomeAssistantStore())!;
  }

  private openDetails(): void {
    if (this.card.entity_id.length === 0) return;

    const r = this.getUIContext().getRouter();
    r.pushUrl(new RouteOptionEntityDetails(this.card.entity_id), () => {});
  }

  build() {
    Row() {
      Column() {
        Text(this.card.name)
          .fontSize(16)
          .maxLines(1);

        Text(this.card.state)
          .fontSize(12)
          .opacity(0.65)
          .maxLines(1);
      }
      .layoutWeight(1)
      .margin({ left: 12 });

      // Details (компактно)
      Button('>')
        .onClick(() => this.openDetails())
        .width(40)
        .height(40)
        .fontSize(18)
        .margin({ right: 8 });

      // Primary action
      Button(this.card.primary.label)
        .onClick(() => {
          if (this.card.entity_id.length === 0) return;
          this.getStore().runAction(this.card.primary);
        })
        .height(40)
        .padding({ left: 14, right: 14 })
        .margin({ right: 12 });
    }
    .width('100%')
    .height(68)
    .borderRadius(28)
    .backgroundColor(0x26262626);
  }
}
