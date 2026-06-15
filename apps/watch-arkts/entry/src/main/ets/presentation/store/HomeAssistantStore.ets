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

    // If we fell back to Mock at startup, try to (re)connect the real P2P repo now so the watch
    // doesn't stay on fake data once the phone becomes reachable.
    await Services.tryUpgradeToReal();

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
    } catch (e) {
      this.lastError = `${e}`;
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
      // Reflect the new state in the UI: Mock updates in place, real P2P returns the
      // authoritative card. Without this the details screen looks unresponsive after a tap.
      const entityId: string = action.data['entity_id'] ? action.data['entity_id'] : '';
      if (entityId.length > 0) await this.refreshEntity(entityId);
    } catch (e) {
      this.lastError = `${e}`;
    }
  }
}
