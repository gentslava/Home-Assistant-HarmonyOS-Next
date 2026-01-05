import util from '@ohos.util';

export function uid(prefix: string = 'm'): string {
  const uuid: string = util.generateRandomUUID(true);
  return `${prefix}_${uuid}`;
}
