export interface HeaderParams {
  title: string;
}

@Builder
export function SystemHeaderBuilder(p: HeaderParams) {
  Row() {
    Text(p.title)
      .fontSize(20)
      .fontWeight(FontWeight.Medium)
      .maxLines(1)
      .textOverflow({ overflow: TextOverflow.Ellipsis });
  }
  .width('100%')
  .justifyContent(FlexAlign.Center)
  .padding({ top: 10, bottom: 8, left: 16, right: 16 });
}
