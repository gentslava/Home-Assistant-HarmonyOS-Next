import { AppInfo } from '../app/AppInfo';

@Entry
@ComponentV2
struct About {
  @Local private versionName: string = '';

  aboutToAppear(): void {
    AppInfo.getVersionName().then((v: string) => {
      this.versionName = v;
    });
  }

  build() {
    Column() {
      Image($r('app.media.logoMonochrome'))
        .width(40)
        .height(40)
        .objectFit(ImageFit.Contain)
        .margin({ bottom: 10 });

      Text($r('app.string.app_name'))
        .fontSize(18)
        .fontWeight(FontWeight.Medium)
        .textAlign(TextAlign.Center)
        .width('100%')
        .margin({ bottom: 10 });

      Text($r('app.string.about_support_text'))
        .fontSize(13)
        .opacity(0.85)
        .textAlign(TextAlign.Center)
        .width('100%')
        .margin({ bottom: 12 });

      Text($r('app.string.about_contact_line1'))
        .fontSize(13)
        .opacity(0.95)
        .textAlign(TextAlign.Center)
        .width('100%')
        .margin({ bottom: 6 });

      Text($r('app.string.about_contact_line2'))
        .fontSize(13)
        .opacity(0.95)
        .textAlign(TextAlign.Center)
        .width('100%')
        .margin({ bottom: 12 });

      // ✅ Никаких `${$r(...)}` — ресурс отдельно, строка отдельно
      Row() {
        Text($r('app.string.about_version_label'))
          .fontSize(13)
          .opacity(0.75);

        Text(this.versionName.length > 0 ? this.versionName : '-')
          .fontSize(13)
          .opacity(0.75)
          .margin({ left: 6 });
      }
      .justifyContent(FlexAlign.Center)
      .width('100%');
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .alignItems(HorizontalAlign.Center)
    .padding({ left: 26, right: 26, top: 18, bottom: 18 });
  }
}
