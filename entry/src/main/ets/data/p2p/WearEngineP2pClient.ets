// entry/src/main/ets/data/p2p/WearEngineP2pClient.ets
import { wearEngine } from '@kit.WearEngine';
import util from '@ohos.util';
import { common } from '@kit.AbilityKit';

import { PeerDeviceResolver } from './PeerDeviceResolver';

export type IncomingHandler = (payload: string) => void;

export class WearEngineP2pClient {
  private readonly ctx: common.UIAbilityContext;
  private readonly p2pClient: wearEngine.P2pClient;
  private readonly deviceClient: wearEngine.DeviceClient;

  private incomingHandler: IncomingHandler | null = null;
  private receiverRegistered: boolean = false;

  // Удалённое приложение (Android companion)
  // Можно задать позже через setRemoteApp(...)
  private remoteBundleName: string = '';
  private remoteFingerprint: string = '';

  constructor(ctx: common.UIAbilityContext) {
    this.ctx = ctx;
    this.p2pClient = wearEngine.getP2pClient(this.ctx);
    this.deviceClient = wearEngine.getDeviceClient(this.ctx);
  }

  /**
   * На случай, если bundleName/fingerprint не хардкодите в коде.
   * Задайте это из конфигурации/констант при старте приложения.
   */
  setRemoteApp(bundleName: string, fingerprint: string): void {
    this.remoteBundleName = bundleName;
    this.remoteFingerprint = fingerprint;
  }

  async hasConnectedPeer(): Promise<boolean> {
    const devices: wearEngine.Device[] = await this.deviceClient.getConnectedDevices();
    return devices.length > 0;
  }

  async init(): Promise<void> {
    // Сейчас инициализация не требуется.
    // Если в вашей версии WearEngine нужны authorizations — добавите здесь.
    return;
  }

  setIncomingHandler(handler: IncomingHandler): void {
    this.incomingHandler = handler;
  }

  async registerReceiver(): Promise<void> {
    if (this.receiverRegistered) return;

    this.ensureRemoteAppConfigured();

    const device: wearEngine.Device =
      await PeerDeviceResolver.resolveFirstConnected(this.deviceClient);

    const appParam: wearEngine.P2pAppParam = this.buildAppParam();

    const callback: (msg: wearEngine.P2pMessage) => void = (msg: wearEngine.P2pMessage): void => {
      const textDecoderOptions: util.TextDecoderOptions = {
        fatal: false,
        ignoreBOM: true
      };

      const decodeOptions: util.DecodeToStringOptions = {
        stream: false
      };

      const decoder: util.TextDecoder = util.TextDecoder.create('utf-8', textDecoderOptions);
      const content: Uint8Array = msg.content;
      const text: string = decoder.decodeToString(content, decodeOptions);
      if (this.incomingHandler) {
        this.incomingHandler(text);
      }
    };

    await this.p2pClient.registerMessageReceiver(device.randomId, appParam, callback);
    this.receiverRegistered = true;
  }

  async sendMessage(payload: string): Promise<void> {
    this.ensureRemoteAppConfigured();

    const device: wearEngine.Device =
      await PeerDeviceResolver.resolveFirstConnected(this.deviceClient);

    const appParam: wearEngine.P2pAppParam = this.buildAppParam();

    const encoder: util.TextEncoder = new util.TextEncoder();
    const message: wearEngine.P2pMessage = {
      content: encoder.encodeInto(payload)
    };

    try {
      await this.p2pClient.sendMessage(device.randomId, appParam, message);
    } catch (error) {
      // TODO: Implement error handling.
    }
  }

  private buildAppParam(): wearEngine.P2pAppParam {
    const appInfo: wearEngine.AppInfo = {
      bundleName: this.remoteBundleName,
      fingerprint: this.remoteFingerprint
    };

    const appParam: wearEngine.P2pAppParam = {
      remoteApp: appInfo
    };

    return appParam;
  }

  private ensureRemoteAppConfigured(): void {
    if (this.remoteBundleName.length === 0) {
      throw new Error('WearEngineP2pClient: remoteBundleName is not set');
    }
    if (this.remoteFingerprint.length === 0) {
      throw new Error('WearEngineP2pClient: remoteFingerprint is not set');
    }
  }
}
