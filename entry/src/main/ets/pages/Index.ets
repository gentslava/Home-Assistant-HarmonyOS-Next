import router from '@ohos.router';
import { AppStorageV2 } from '@ohos.arkui.StateManagement';
import {
  ArcList, ArcListItem,
  ArcListAttribute,
  ComponentContent, LengthMetrics,
} from '@kit.ArkUI';

import { AppKeys } from '../app/AppKeys';
import { HomeAssistantStore } from '../presentation/store/HomeAssistantStore';
import { EntityCard } from '../domain/model/EntityCard';

import { SystemTile } from '../presentation/ui/components/SystemTile';
import { HeaderParams, SystemHeaderBuilder } from '../presentation/ui/components/SystemHeader';

interface IndexDetailsParams {
  entity_id: string;
}

class RouteOptionEntityDetails implements router.RouterOptions {
  url: string;
  params?: IndexDetailsParams;

  constructor(id: string) {
    this.url = 'pages/EntityDetails';
    this.params = { entity_id: id };
  }
}

class RouteOptionSettings implements router.RouterOptions {
  url: string;

  constructor() {
    this.url = 'pages/Settings';
  }
}

@Entry
@ComponentV2
struct Index {
  @Local store: HomeAssistantStore =
    AppStorageV2.connect(HomeAssistantStore, AppKeys.STORE_KEY, () => new HomeAssistantStore())!;

  private readonly scroller: Scroller = new Scroller();

  private headerContent(): ComponentContent<HeaderParams> {
    const p: HeaderParams = { title: 'Home' };
    return new ComponentContent<HeaderParams>(
      this.getUIContext(),
      wrapBuilder<[HeaderParams]>(SystemHeaderBuilder),
      p
    );
  }

  private badgeText(domain: string, state?: string): string {
    if (domain.length > 0) {
      if (domain === 'light') {
        return '💡';
      }
      if (domain === 'lock') {
        return '🔒';
      }
      if (domain === 'switch') {
        return '🎚️';
      }
      return domain.substring(0, 1).toUpperCase();
    } else return '?';
  }

  private iconResForDomain(domain: string, state?: string): Resource | undefined {
    if (domain === 'light') {
      return $r('app.media.ic_domain_light');
    }
    if (domain === 'lock') {
      if (state === 'unlocked') return $r('app.media.ic_domain_lock_unlocked');
      return $r('app.media.ic_domain_lock_locked');
    }
    if (domain === 'switch') {
      if (state === 'on') return $r('app.media.ic_domain_switch_on');
      return $r('app.media.ic_domain_switch_off');
    }
    return undefined;
  }

  private iconBgColorForDomain(domain: string): number {
    if (domain === 'light') {
      return 0xFFF2C94C; // жёлтый
    }
    if (domain === 'lock') {
      return 0xFF9B51E0; // фиолетовый
    }
    if (domain === 'switch') {
      return 0xFF27AE60; // зелёный
    }
    return 0xFF4A4A4A; // дефолт/unknown
  }

  private openDetails(entityId: string): void {
    if (entityId.length === 0) return;
    const r = this.getUIContext().getRouter();
    r.pushUrl(new RouteOptionEntityDetails(entityId), () => {});
  }

  private openSettings(): void {
    const r = this.getUIContext().getRouter();
    r.pushUrl(new RouteOptionSettings(), () => {});
  }

  private async doRefresh(): Promise<void> {
    if (this.store.isSyncing) return;
    await this.store.sync();
  }

  build() {
    Stack() {
      Refresh({ refreshing: this.store.isSyncing }) {
        ArcList({
          scroller: this.scroller,
          header: this.headerContent()
        }) {
          ForEach(this.store.cards, (entity: EntityCard) => {
            ArcListItem() {
              Row() {
                SystemTile({
                  title: entity.name,
                  subtitle: entity.state,
                  iconRes: this.iconResForDomain(entity.domain, entity.state),
                  iconText: this.badgeText(entity.domain, entity.state),
                  iconBgColor: this.iconBgColorForDomain(entity.domain),
                  showChevron: true
                });
              }
              .width('100%')
              .justifyContent(FlexAlign.Center)
              .onClick(() => this.openDetails(entity.entity_id));
            }
          }, (entity: EntityCard) => entity.entity_id);

          ArcListItem() {
            Row() {
              SystemTile({
                title: 'Settings',
                subtitle: 'App preferences',
                iconText: '⚙',
                iconBgColor: 0xFF4A4A4A,
                showChevron: true
              });
            }
            .width('100%')
            .justifyContent(FlexAlign.Center)
            .onClick(() => this.openSettings());
          }
        }
        .space(LengthMetrics.vp(6))
        .width('100%')
        .height('100%')
        .focusable(true)
        .focusOnTouch(true)
        .defaultFocus(true);
      }
      .onRefreshing(() => {
        // onRefreshing ожидает void — запускаем async без await
        this.doRefresh();
      });
    }
    .width('100%')
    .height('100%')
    .backgroundColor($r("app.color.page_background"));
  }
}
