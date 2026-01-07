import { IconBadge } from './IconBadge';

@ComponentV2
export struct SystemTile {
  @Param title: string | Resource = '';
  @Param subtitle: string | Resource = '';
  @Param iconText: string = '?';
  @Param iconRes: Resource | undefined = undefined;
  @Param iconBgColor: number = 0xFF1E88FF;
  @Param showChevron: boolean = true;
  @Param bgColor: number = 0xFF262626;

  build() {
    Row() {
      IconBadge({ text: this.iconText, res: this.iconRes, bgColor: this.iconBgColor, badgeSize: 44 });

      Column() {
        Text(this.title)
          .fontSize(18)
          .maxLines(1)
          .fontColor(0xFFFFFFFF)
          .textOverflow({ overflow: TextOverflow.Ellipsis });

        if (this.subtitle) {
          Text(this.subtitle)
            .fontSize(13)
            .opacity(0.65)
            .maxLines(1)
            .fontColor(0xFFFFFFFF)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .margin({ top: 2 });
        }
      }
      .layoutWeight(1)
      .margin({ left: 4, right: 4 });

      if (this.showChevron) {
        Image($r("app.media.chevron_right"))
          .width(30)
          .height(30)
          .opacity(0.65)
          .objectFit(ImageFit.Contain)
          .margin({ right: 0 });
      }
    }
    .width('89%')
    .borderRadius('50%')
    .backgroundColor(this.bgColor)
    .padding(8)
    .alignItems(VerticalAlign.Center);
  }
}
