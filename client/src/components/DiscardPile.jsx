import React, { useState } from 'react'
import CardComponent from './CardComponent'

export default function DiscardPile({ topCard, onDrop }) {
  const [dragOver, setDragOver] = useState(false)

  const handleDragOver = (e) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    setDragOver(true)
  }

  const handleDragLeave = () => setDragOver(false)

  const handleDrop = (e) => {
    e.preventDefault()
    setDragOver(false)
    const cardIndex = parseInt(e.dataTransfer.getData('text/plain'), 10)
    if (!isNaN(cardIndex) && onDrop) {
      onDrop(cardIndex)
    }
  }

  return (
    <div
      data-drop-target="discard"
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      style={{ textAlign: 'center' }}
    >
      <div style={{ fontSize: 24, color: '#aed581', marginBottom: 4 }}>Discard</div>
      {topCard ? (
        <div style={{
          border: dragOver ? '3px solid #ff6f00' : '3px solid transparent',
          borderRadius: 10,
          transition: 'border-color 0.15s'
        }}>
          <CardComponent card={topCard} />
        </div>
      ) : (
        <div style={{
          width: 160, height: 220, borderRadius: 8,
          border: dragOver ? '3px solid #ff6f00' : '2px dashed rgba(255,255,255,0.2)',
          background: dragOver ? 'rgba(255,111,0,0.15)' : 'transparent',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 28, color: dragOver ? '#ff6f00' : '#666',
          transition: 'border-color 0.15s, background 0.15s'
        }}>
          {dragOver ? 'Drop to discard' : 'Discard'}
        </div>
      )}
    </div>
  )
}
