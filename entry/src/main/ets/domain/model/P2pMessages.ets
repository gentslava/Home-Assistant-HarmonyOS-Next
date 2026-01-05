// entry/src/main/ets/domain/model/P2pMessages.ets
import { EntityCard } from './EntityCard';

export type MsgType = 'SYNC_REQUEST' | 'SYNC_RESPONSE' | 'SYNC_ENTITY_REQUEST' | 'SYNC_ENTITY_RESPONSE' | 'CALL_SERVICE' | 'ACK';

export interface BaseMsg {
  v: number;
  id: string;
  type: MsgType;
}

export interface SyncRequest extends BaseMsg {
  type: 'SYNC_REQUEST';
}

export interface SyncResponse extends BaseMsg {
  type: 'SYNC_RESPONSE';
  cards: EntityCard[];
}

export interface SyncEntityRequest extends BaseMsg {
  type: 'SYNC_ENTITY_REQUEST';
  entity_id: string;
}

export interface SyncEntityResponse extends BaseMsg {
  type: 'SYNC_ENTITY_RESPONSE';
  card?: EntityCard;
}

export interface CallService extends BaseMsg {
  type: 'CALL_SERVICE';
  domain: string;
  service: string;
  data: Record<string, string>;
}

export interface Ack extends BaseMsg {
  type: 'ACK';
  ok: boolean;
  error?: string;
}

export type AnyMsg = SyncRequest | SyncResponse | SyncEntityRequest | SyncEntityResponse | CallService | Ack;
