import { EntityCard, EntityAction } from '../../domain/model/EntityCard';
import { Services } from '../../app/Services';

@ObservedV2
export class HomeAssistantStore {
  @Trace cards: EntityCard[] = [];
  @Trace isSyncing: boolean = false;
  @Trace lastError: string = '';

  async sync(): Promise<void> {
    if (!Services.repo) {
      this.lastError = 'Repository not initialized';
      return;
    }

    try {
      this.isSyncing = true;
      this.lastError = '';
      this.cards = await Services.repo.sync();
    } catch (e) {
      this.lastError = `${e}`;
    } finally {
      this.isSyncing = false;
    }
  }

  async refreshEntity(entityId: string): Promise<void> {
    if (this.isSyncing) return;
    if (entityId.length === 0) return;

    if (!Services.repo) {
      this.lastError = 'Repository not initialized';
      return;
    }

    try {
      this.isSyncing = true;
      const updated: EntityCard | undefined = await Services.repo.syncEntityCard(entityId);
      if (!updated) return;

      const next: EntityCard[] = [];
      let replaced: boolean = false;

      for (let i: number = 0; i < this.cards.length; i++) {
        const entity: EntityCard = this.cards[i];
        if (entity.entity_id === entityId) {
          next.push(updated);
          replaced = true;
        } else {
          next.push(entity);
        }
      }

      if (!replaced) {
        next.push(updated);
      }

      this.cards = next;
    } finally {
      this.isSyncing = false;
    }
  }

  async runAction(action: EntityAction): Promise<void> {
    if (!Services.repo) {
      this.lastError = 'Repository not initialized';
      return;
    }

    try {
      this.lastError = '';
      await Services.repo.callAction(action);
    } catch (e) {
      this.lastError = `${e}`;
    }
  }
}
