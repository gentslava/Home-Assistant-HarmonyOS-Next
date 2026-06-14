import { EntityCard, EntityAction } from '../model/EntityCard';

export interface HomeAssistantRepository {
  sync(): Promise<EntityCard[]>;
  syncEntityCard(entityId: string): Promise<EntityCard | undefined>;
  callAction(action: EntityAction): Promise<void>;
}
