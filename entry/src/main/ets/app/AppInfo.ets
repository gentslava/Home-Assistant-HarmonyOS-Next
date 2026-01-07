import { bundleManager } from '@kit.AbilityKit'; // HarmonyOS NEXT

interface SelfBundleInfo {
  versionName?: string;
}

export class AppInfo {
  private static cachedVersionName: string = '';

  static async getVersionName(): Promise<string> {
    if (AppInfo.cachedVersionName.length > 0) {
      return AppInfo.cachedVersionName;
    }

    try {
      const info: SelfBundleInfo = await bundleManager.getBundleInfoForSelf(
        bundleManager.BundleFlag.GET_BUNDLE_INFO_DEFAULT
      ) as SelfBundleInfo;

      const v: string = info.versionName ? info.versionName : '';
      AppInfo.cachedVersionName = v;
      return v;
    } catch (e) {
      AppInfo.cachedVersionName = '';
      return '';
    }
  }
}
