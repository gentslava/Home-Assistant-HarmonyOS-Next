export function toJson(obj: object): string {
  return JSON.stringify(obj);
}

export function fromJson<T>(s: string): T {
  return JSON.parse(s) as T;
}
