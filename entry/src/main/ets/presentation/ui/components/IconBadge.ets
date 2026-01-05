@ComponentV2
export struct IconBadge {
  @Param text: string = '?';
  @Param res: Resource | undefined = undefined;
  @Param bgColor: number = 0xFF1E88FF;
  @Param badgeSize: number = 44;

  build() {
    Row() {
      if (this.res) {
        Image(this.res)
          .width(18)
          .height(18)
          .objectFit(ImageFit.Contain);
      } else {
        Text(this.text)
          .fontSize(18)
          .fontColor(Color.White);
      }
    }
    .width(this.badgeSize)
    .height(this.badgeSize)
    .borderRadius(this.badgeSize / 2)
    .justifyContent(FlexAlign.Center)
    .alignItems(VerticalAlign.Center)
    .backgroundColor(this.bgColor);
  }
}
