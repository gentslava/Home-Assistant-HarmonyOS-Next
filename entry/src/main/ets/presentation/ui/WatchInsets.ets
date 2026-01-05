export class WatchInsets {
  // Huawei в гайде для round screen рекомендует безопасные поля (порядка 26vp по горизонтали). :contentReference[oaicite:3]{index=3}
  static readonly SAFE_H: number = 26;     // left/right
  static readonly SAFE_TOP: number = 20;   // top
  static readonly SAFE_BOTTOM: number = 22;// bottom
  static readonly GAP: number = 10;        // internal spacing
}
