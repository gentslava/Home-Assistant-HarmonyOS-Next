import { HomeAssistantRepository } from '../../domain/repository/HomeAssistantRepository';
import { WearEngineP2pClient } from '../p2p/WearEngineP2pClient';
import { uid } from '../../core/utils/Id';
import { toJson, fromJson } from '../../core/json/Json';

import { EntityCard, EntityAction } from '../../domain/model/EntityCard';
import { SyncRequest, SyncResponse, SyncEntityRequest, SyncEntityResponse, CallService, Ack, AnyMsg } from '../../domain/model/P2pMessages';

interface PendingMsg {
  resolve: (value: AnyMsg) => void;
  reject: (err: Error) => void;
  timeoutId: number;
}

export class P2pHomeAssistantRepository implements HomeAssistantRepository {
  private p2p: WearEngineP2pClient;
  private pending: Map<string, PendingMsg> = new Map<string, PendingMsg>();

  constructor(p2p: WearEngineP2pClient) {
    this.p2p = p2p;
    this.p2p.setIncomingHandler((payload: string) => this.onIncoming(payload));
  }

  async init(): Promise<void> {
    await this.p2p.registerReceiver();
  }

  async sync(): Promise<EntityCard[]> {
    const id: string = uid('sync');

    const req: SyncRequest = { v: 1, id: id, type: 'SYNC_REQUEST' };
    const msg: AnyMsg = await this.sendAndWait(id, req);

    if (msg.type === 'ACK' && !msg.ok) {
      return [];
    }

    const resp: SyncResponse = msg as SyncResponse;
    return resp.cards ?? [];
  }

  async syncEntityCard(entityId: string): Promise<EntityCard | undefined> {
    if (entityId.length === 0) return undefined;

    const id: string = uid('get_card');

    const req: SyncEntityRequest = { v: 1, id: id, type: 'SYNC_ENTITY_REQUEST', entity_id: entityId };
    const msg: AnyMsg = await this.sendAndWait(id, req);

    if (msg.type === 'ACK' && !msg.ok) {
      return undefined;
    }

    const resp: SyncEntityResponse = msg as SyncEntityResponse;
    return resp.card;
  }

  async callAction(action: EntityAction): Promise<void> {
    const id: string = uid('svc');

    const call: CallService = {
      v: 1,
      id: id,
      type: 'CALL_SERVICE',
      domain: action.domain,
      service: action.service,
      data: action.data
    };

    const msg: AnyMsg = await this.sendAndWait(id, call);
    const ack: Ack = msg as Ack;

    if (!ack.ok) throw new Error(ack.error ?? 'Service call failed');
  }

  private async sendAndWait(id: string, msg: AnyMsg): Promise<AnyMsg> {
    const wait: Promise<AnyMsg> = this.waitFor(id, 8000);
    await this.p2p.sendMessage(toJson(msg));
    return await wait;
  }

  private onIncoming(payload: string): void {
    const msg: AnyMsg = fromJson<AnyMsg>(payload);
    const id: string = msg.id;

    const p: PendingMsg | undefined = this.pending.get(id);
    if (!p) return;

    clearTimeout(p.timeoutId);
    this.pending.delete(id);
    p.resolve(msg);
  }

  private waitFor(id: string, timeoutMs: number): Promise<AnyMsg> {
    return new Promise<AnyMsg>(
      (resolve: (value: AnyMsg) => void, reject: (err: Error) => void) => {
        const timeoutId: number = setTimeout(() => {
          this.pending.delete(id);
          reject(new Error(`Timeout: ${id}`));
        }, timeoutMs);

        const pending: PendingMsg = { resolve, reject, timeoutId };
        this.pending.set(id, pending);
      }
    );
  }
}
