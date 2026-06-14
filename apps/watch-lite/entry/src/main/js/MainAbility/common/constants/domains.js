/*
 * Domain -> UI hint (icon + accent color) and EntityCard -> list item mapping.
 * ES5.1 only (var/function, no arrow/spread). See ../../README / apps/watch-lite/AGENTS.md.
 */

var MAP = {
    light:  { icon: '/common/icons/light.png',  color: '#F2C94C' },
    switch: { icon: '/common/icons/switch.png', color: '#27AE60' },
    lock:   { icon: '/common/icons/lock.png',   color: '#8E44AD' }
};

export function uiFor(domain) {
    return MAP[domain] || { icon: '/common/icons/unknown.png', color: '#9AA0A6' };
}

/** EntityCard (from the companion, see docs/p2p-protocol.md) -> the item shape pages render. */
export function cardToItem(card) {
    var ui = uiFor(card.domain);
    return {
        id: card.entity_id,
        name: card.name,
        state: card.state,
        domain: card.domain,
        iconSrc: ui.icon,
        color: ui.color,
        primary: card.primary || null,
        secondary: card.secondary || []
    };
}

/** Map a list of EntityCards to items. */
export function cardsToItems(cards) {
    var out = [];
    var list = cards || [];
    for (var i = 0; i < list.length; i++) {
        out.push(cardToItem(list[i]));
    }
    return out;
}
