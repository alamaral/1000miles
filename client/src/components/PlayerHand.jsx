import React, { useRef } from 'react'
import CardComponent from './CardComponent'

const MIN_CARDS = 7
const GAP = 8
// Available width inside the hand area (design width minus container + hand padding)
const HAND_INNER = 1700 - 24 - 32
const CARD_W = Math.floor((HAND_INNER - (MIN_CARDS - 1) * GAP) / MIN_CARDS)
const CARD_H = Math.round(CARD_W * (220 / 160))

export default function PlayerHand({ hand, canDrag, onReorder, dragIndexRef }) {
  const dragSrcRef = useRef(null)

  const handleDragStart = (e, index) => {
    dragSrcRef.current = index
    if (dragIndexRef) dragIndexRef.current = index
  }

  const handleDragOver = (e, index) => {
    e.preventDefault()
    const src = dragSrcRef.current
    if (src == null || src === index) return
    if (onReorder) {
      onReorder(src, index)
      dragSrcRef.current = index
      if (dragIndexRef) dragIndexRef.current = index
    }
  }

  const handleDragEnd = () => {
    dragSrcRef.current = null
  }

  return (
    <div style={{
      display: 'flex',
      gap: GAP,
      justifyContent: 'center',
      flexWrap: 'nowrap',
      padding: '12px 0',
    }}>
      {hand.map((card, i) => (
        <div
          key={i}
          onDragOver={(e) => handleDragOver(e, i)}
          onDragStart={(e) => handleDragStart(e, i)}
          onDragEnd={handleDragEnd}
        >
          <CardComponent
            card={card}
            draggable={canDrag}
            dragIndex={i}
            style={{ width: CARD_W, height: CARD_H, flexShrink: 1, minWidth: 0 }}
          />
        </div>
      ))}
    </div>
  )
}
