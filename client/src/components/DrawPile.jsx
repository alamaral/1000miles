import React from 'react'
import CardComponent from './CardComponent'

export default function DrawPile({ deckSize, onClick }) {
  return (
    <div style={{ textAlign: 'center' }}>
      <div style={{ fontSize: 24, color: '#aed581', marginBottom: 4 }}>Draw Pile</div>
      <CardComponent faceDown onClick={onClick} />
      <div style={{ fontSize: 24, color: '#aed581', marginTop: 4 }}>
        {deckSize} cards
      </div>
    </div>
  )
}
