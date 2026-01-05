export class Logger {
  static d(tag: string, msg: string) { console.info(`[${tag}] ${msg}`); }
  static e(tag: string, msg: string) { console.error(`[${tag}] ${msg}`); }
}
