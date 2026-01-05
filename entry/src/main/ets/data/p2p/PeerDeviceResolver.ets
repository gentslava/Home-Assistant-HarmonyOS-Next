import { wearEngine } from '@kit.WearEngine';

export class PeerDeviceResolver {
  static async resolveFirstConnected(deviceClient: wearEngine.DeviceClient): Promise<wearEngine.Device> {
    const devices: wearEngine.Device[] = await deviceClient.getConnectedDevices();
    if (devices.length === 0) {
      throw new Error('No connected peer devices');
    }
    return devices[0];
  }
}
