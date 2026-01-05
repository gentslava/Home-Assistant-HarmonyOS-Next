import { AppStorageV2 } from '@ohos.arkui.StateManagement';
import {
  ArcList, ArcListItem,
  ArcListAttribute, ArcListItemAttribute,
  ComponentContent, LengthMetrics,
} from '@kit.ArkUI';

import { AppKeys } from '../app/AppKeys';
import { HomeAssistantStore } from '../presentation/store/HomeAssistantStore';

import { SystemTile } from '../presentation/ui/components/SystemTile';
import { HeaderParams, SystemHeaderBuilder } from '../presentation/ui/components/SystemHeader';

@Entry
@ComponentV2
struct Settings {
  @Local store: HomeAssistantStore =
    AppStorageV2.connect(HomeAssistantStore, AppKeys.STORE_KEY, () => new HomeAssistantStore())!;

  private readonly scroller: Scroller = new Scroller();

  private headerContent(): ComponentContent<HeaderParams> {
    const p: HeaderParams = { title: 'Settings' };
    return new ComponentContent<HeaderParams>(
      this.getUIContext(),
      wrapBuilder<[HeaderParams]>(SystemHeaderBuilder),
      p
    );
  }

  build() {
    ArcList({
      scroller: this.scroller,
      header: this.headerContent()
    }) {
      ArcListItem() {
        Row() {
          SystemTile({
            title: 'Connection',
            subtitle: 'Local / Cloud (coming soon)',
            iconText: 'C',
            iconBgColor: 0xFF4A4A4A,
            showChevron: false
          });
        }
        .width('100%')
        .justifyContent(FlexAlign.Center);
      }

      ArcListItem() {
        Row() {
          SystemTile({
            title: 'Server',
            subtitle: 'Configure on phone',
            iconText: 'S',
            iconBgColor: 0xFF4A4A4A,
            showChevron: false
          });
        }
        .width('100%')
        .justifyContent(FlexAlign.Center);
      }

      ArcListItem() {
        Row() {
          SystemTile({
            title: 'Debug mode',
            subtitle: 'Mock data on emulator',
            iconText: 'D',
            iconBgColor: 0xFF4A4A4A,
            showChevron: false
          });
        }
        .width('100%')
        .justifyContent(FlexAlign.Center);
      }

      ArcListItem() {
        Row() {
          SystemTile({
            title: 'About',
            subtitle: 'Home Assistant Wearable',
            iconText: 'i',
            iconBgColor: 0xFF4A4A4A,
            showChevron: false
          });
        }
        .width('100%')
        .justifyContent(FlexAlign.Center);
      }
    }
    .space(LengthMetrics.vp(6))
    .width('100%')
    .height('100%')
    .focusable(true)
    .focusOnTouch(true)
    .defaultFocus(true)
    .backgroundColor($r("app.color.page_background"));
  }
}
