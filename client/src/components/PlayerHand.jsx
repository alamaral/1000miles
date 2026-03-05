import React from 'react'
import CardComponent from './CardComponent'

const MIN_CARDS = 7
const GAP = 8
// Available width inside the hand area (design width minus container + hand padding)
const HAND_INNER = 1700 - 24 - 32
const CARD_W = Math.floor((HAND_INNER - (MIN_CARDS - 1) * GAP) / MIN_CARDS)
const CARD_H = Math.round(CARD_W * (220 / 160))

export default function PlayerHand({ hand, canDrag }) {
  return (
    <div style={{
      display: 'flex',
      gap: GAP,
      justifyContent: 'center',
      padding: '12px 0',
    }}>
      {hand.map((card, i) => (
        <CardComponent
          key={i}
          card={card}
          draggable={canDrag}
          dragIndex={i}
          style={{ width: CARD_W, height: CARD_H }}
        />
      ))}
    </div>
  )
}
