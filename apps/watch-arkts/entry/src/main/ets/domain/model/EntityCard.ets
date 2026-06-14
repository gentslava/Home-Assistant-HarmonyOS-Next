export type ActionKind = 'SERVICE';

export interface EntityAction {
  kind: ActionKind;
  label: string;
  domain: string;
  service: string;
  // минимально: entity_id; позже можно расширять до любых payload
  data: Record<string, string>;
}

export interface EntityCard {
  entity_id: string;
  domain: string;
  name: string;
  state: string;
  icon?: string;
  primary: EntityAction;
  secondary: EntityAction[];
}
