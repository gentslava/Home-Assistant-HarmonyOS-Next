import util from '@ohos.util';

import { EntityCard, EntityAction } from '../../domain/model/EntityCard';
import { HomeAssistantRepository } from '../../domain/repository/HomeAssistantRepository';

export class MockHomeAssistantRepository implements HomeAssistantRepository {
  private entities: EntityCard[] = [];

  constructor() {
    this.entities = this.generateRandomEntities();
  }

  async sync(): Promise<EntityCard[]> {
    this.entities = this.generateRandomEntities();
    return this.entities;
  }

  async syncEntityCard(entityId: string): Promise<EntityCard | undefined> {
    const idx: number = this.entities.findIndex((entity: EntityCard) => entity.entity_id === entityId);
    if (idx < 0) return undefined;

    // эмуляция “обновления с сервера” только для этой сущности
    const roll: number = Math.random();
    if (roll < 0.7) { // 70% случаев меняем state, чтобы было видно refresh
      const card: EntityCard = this.entities[idx];

      if (card.domain === 'lock') {
        const nextLock: string = (card.state === 'locked') ? 'unlocked' : 'locked';
        this.entities[idx] = this.withState(card, nextLock);
      } else if (card.domain === 'light' || card.domain === 'switch') {
        const next: string = (card.state === 'on') ? 'off' : 'on';
        this.entities[idx] = this.withState(card, next);
      }
    }

    return this.entities[idx];
  }

  async callAction(action: EntityAction): Promise<void> {
    const targetId: string = this.getEntityIdFromAction(action);
    if (targetId.length === 0) return;

    const idx: number = this.entities.findIndex((entity: EntityCard) => entity.entity_id === targetId);
    if (idx < 0) return;

    const card: EntityCard = this.entities[idx];

    if (card.domain === 'light' || card.domain === 'switch') {
      const next: string = (card.state === 'on') ? 'off' : 'on';
      this.entities[idx] = this.withState(card, next);
      return;
    }

    if (card.domain === 'lock') {
      const nextLock: string = (card.state === 'locked') ? 'unlocked' : 'locked';
      this.entities[idx] = this.withState(card, nextLock);
      return;
    }
  }

  // ===== generators =====

  private generateRandomEntities(): EntityCard[] {
    const out: EntityCard[] = [];

    const lightsCount: number = this.randomIntInclusive(1, 2);
    const locksCount: number = this.randomIntInclusive(1, 2);
    const switchesCount: number = this.randomIntInclusive(1, 2);

    let i: number = 1;

    for (let n: number = 0; n < lightsCount; n++) {
      out.push(this.makeLight(i));
      i++;
    }
    for (let n: number = 0; n < locksCount; n++) {
      out.push(this.makeLock(i));
      i++;
    }
    for (let n: number = 0; n < switchesCount; n++) {
      out.push(this.makeSwitch(i));
      i++;
    }

    return out;
  }

  private makeLight(i: number): EntityCard {
    const id: string = this.uuid();
    const state: string = this.randomBool() ? 'on' : 'off';

    const primary: EntityAction = this.makeServiceAction(
      'light',
      'toggle',
      state === 'on' ? 'app.string.Entity_light_disable_label' : 'app.string.Entity_light_enable_label',
      id
    );

    const secondary: EntityAction[] = [];
    secondary.push(this.makeServiceAction('light', 'turn_on', 'Turn on', id));
    secondary.push(this.makeServiceAction('light', 'turn_off', 'Turn off', id));

    const card: EntityCard = {
      entity_id: id,
      name: `Light ${i}`,
      domain: 'light',
      state: state,
      primary: primary,
      secondary: secondary
    };

    return card;
  }

  private makeLock(i: number): EntityCard {
    const id: string = this.uuid();
    const state: string = this.randomBool() ? 'locked' : 'unlocked';

    const primary: EntityAction = this.makeServiceAction(
      'lock',
      'toggle',
      state === 'locked' ? 'app.string.Entity_lock_disable_label' : 'app.string.Entity_lock_enable_label',
      id
    );

    const secondary: EntityAction[] = [];
    secondary.push(this.makeServiceAction('lock', 'lock', 'Lock', id));
    secondary.push(this.makeServiceAction('lock', 'unlock', 'Unlock', id));

    const card: EntityCard = {
      entity_id: id,
      name: `Lock ${i}`,
      domain: 'lock',
      state: state,
      primary: primary,
      secondary: secondary
    };

    return card;
  }

  private makeSwitch(i: number): EntityCard {
    const id: string = this.uuid();
    const state: string = this.randomBool() ? 'on' : 'off';

    const primary: EntityAction = this.makeServiceAction(
      'switch',
      'toggle',
      state === 'on' ? 'app.string.Entity_switch_disable_label' : 'app.string.Entity_switch_enable_label',
      id
    );

    const secondary: EntityAction[] = [];
    secondary.push(this.makeServiceAction('switch', 'turn_on', 'Turn on', id));
    secondary.push(this.makeServiceAction('switch', 'turn_off', 'Turn off', id));

    const card: EntityCard = {
      entity_id: id,
      name: `Switch ${i}`,
      domain: 'switch',
      state: state,
      primary: primary,
      secondary: secondary
    };

    return card;
  }

  private withState(card: EntityCard, newState: string): EntityCard {
    const updated: EntityCard = {
      entity_id: card.entity_id,
      name: card.name,
      domain: card.domain,
      state: newState,
      primary: this.rebuildPrimary(card.domain, card.entity_id, newState),
      secondary: card.secondary
    };
    return updated;
  }

  private rebuildPrimary(domain: string, entityId: string, state: string): EntityAction {
    if (domain === 'lock') {
      return this.makeServiceAction(
        'lock',
        'toggle',
        state === 'locked' ? 'Unlock' : 'Lock',
        entityId
      );
    }
    return this.makeServiceAction(
      domain,
      'toggle',
      state === 'on' ? 'Turn off' : 'Turn on',
      entityId
    );
  }

  private makeServiceAction(domain: string, service: string, label: string, entityId: string): EntityAction {
    const data: Record<string, string> = this.makeData(entityId);

    const a: EntityAction = {
      kind: 'SERVICE',
      domain: domain,
      service: service,
      label: label,
      data: data
    };

    return a;
  }

  private makeData(entityId: string): Record<string, string> {
    // ✅ Без indexed signature: используем явное приведение к Record
    const d: Record<string, string> = ({ "entity_id": entityId } as Record<string, string>);
    return d;
  }

  private getEntityIdFromAction(action: EntityAction): string {
    // data: Record<string,string>
    const data: Record<string, string> = action.data;
    const id: string = data['entity_id'] ? data['entity_id'] : '';
    return id;
  }

  private uuid(): string {
    return util.generateRandomUUID(true);
  }

  private randomBool(): boolean {
    return Math.random() >= 0.5;
  }

  private randomIntInclusive(min: number, max: number): number {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }
}
