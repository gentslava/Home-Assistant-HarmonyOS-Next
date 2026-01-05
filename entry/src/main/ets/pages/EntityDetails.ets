import router from '@ohos.router';
import { AppStorageV2 } from '@ohos.arkui.StateManagement';
import {
  ArcList, ArcListItem,
  ArcListAttribute,
  ComponentContent, LengthMetrics
} from '@kit.ArkUI';

import { AppKeys } from '../app/AppKeys';
import { HomeAssistantStore } from '../presentation/store/HomeAssistantStore';
import { EntityCard, EntityAction } from '../domain/model/EntityCard';

import { SystemTile } from '../presentation/ui/components/SystemTile';
import { HeaderParams, SystemHeaderBuilder } from '../presentation/ui/components/SystemHeader';

interface EntityDetailsParams {
  entity_id: string;
}

@Entry
@ComponentV2
struct EntityDetails {
  @Local store: HomeAssistantStore =
    AppStorageV2.connect(HomeAssistantStore, AppKeys.STORE_KEY, () => new HomeAssistantStore())!;

  private readonly scroller: Scroller = new Scroller();

  @Local private entityId: string = '';

  aboutToAppear(): void {
    const uiRouter = this.getUIContext().getRouter();

    const paramsObj: object = uiRouter.getParams();
    const params: EntityDetailsParams = paramsObj as EntityDetailsParams;

    this.entityId = params ? params.entity_id : '';
  }

  private getCard(): EntityCard | undefined {
    return this.store.cards.find((entity: EntityCard) => entity.entity_id === this.entityId);
  }

  private titleText(): string {
    const entity: EntityCard | undefined = this.getCard();
    return entity ? entity.name : 'Entity';
  }

  private headerContent(): ComponentContent<HeaderParams> {
    const p: HeaderParams = { title: this.titleText() };
    return new ComponentContent<HeaderParams>(
      this.getUIContext(),
      wrapBuilder<[HeaderParams]>(SystemHeaderBuilder),
      p
    );
  }

  private stateText(): string {
    const entity: EntityCard | undefined = this.getCard();
    return entity ? entity.state : '-';
  }

  private primaryLabel(): string {
    const entity: EntityCard | undefined = this.getCard();
    return entity ? entity.primary.label : '-';
  }

  private secondaryActions(): EntityAction[] {
    const entity: EntityCard | undefined = this.getCard();
    return entity ? entity.secondary : [];
  }

  private runPrimary(): void {
    const entity: EntityCard | undefined = this.getCard();
    if (!entity) return;
    this.store.runAction(entity.primary);
  }

  private runSecondary(a: EntityAction): void {
    this.store.runAction(a);
  }

  private doRefresh(): void {
    if (this.store.isSyncing) return;
    if (this.entityId.length === 0) return;
    this.store.refreshEntity(this.entityId);
  }

  build() {
    Stack() {
      Refresh({ refreshing: this.store.isSyncing }) {
        ArcList({
          scroller: this.scroller,
          header: this.headerContent()
        }) {
          // State — одна строка, фикс. высота (как вы просили)
          ArcListItem() {
            Row() {
              Text('State: ')
                .fontSize(14)
                .opacity(0.7);
              Text(this.stateText())
                .fontSize(14)
                .fontWeight(FontWeight.Medium);
            }
            .width('100%')
            .margin({ top: -60 })
            .justifyContent(FlexAlign.Center)
            .alignItems(VerticalAlign.Center);
          };

          // Primary
          ArcListItem() {
            if (!this.getCard()) {
              // empty
            } else {
              Row() {
                SystemTile({
                  title: this.primaryLabel(),
                  subtitle: '',
                  iconRes: $r('app.media.ic_entity_primary'),
                  iconText: '⏻',
                  iconBgColor: 0xFF1E88FF,
                  showChevron: true
                });
              }
              .width('100%')
              .justifyContent(FlexAlign.Center)
              .onClick(() => this.runPrimary());
            }
          }

          // Secondary
          ForEach(this.secondaryActions(), (a: EntityAction) => {
            ArcListItem() {
              Row() {
                SystemTile({
                  title: a.label,
                  subtitle: '',
                  iconRes: $r('app.media.ic_entity_secondary'),
                  iconText: '↻',
                  iconBgColor: 0xFF1E88FF,
                  showChevron: true
                });
              }
              .width('100%')
              .justifyContent(FlexAlign.Center)
              .onClick(() => this.runSecondary(a));
            }
          }, (a: EntityAction) => `${a.domain}.${a.service}.${a.label}`);
        }
        .space(LengthMetrics.vp(6))
        .width('100%')
        .height('100%')
        .focusable(true)
        .focusOnTouch(true)
        .defaultFocus(true);
      }
      .onRefreshing(() => {
        this.doRefresh();
      });
    }
    .width('100%')
    .height('100%')
    .backgroundColor($r("app.color.page_background"));
  }
}
