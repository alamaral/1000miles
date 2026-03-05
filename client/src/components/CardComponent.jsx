import React from 'react'

const BORDER_HAZARD = '#f44336'
const BORDER_REMEDY = '#4caf50'
const BORDER_SAFETY = '#ffd700'
const BORDER_DISTANCE = '#4caf50'

const CARD_COLORS = {
  MILES_25: { bg: '#e8f5e9', border: BORDER_DISTANCE, text: '#2e7d32' },
  MILES_50: { bg: '#e8f5e9', border: BORDER_DISTANCE, text: '#2e7d32' },
  MILES_75: { bg: '#e8f5e9', border: BORDER_DISTANCE, text: '#2e7d32' },
  MILES_100: { bg: '#e8f5e9', border: BORDER_DISTANCE, text: '#2e7d32' },
  MILES_200: { bg: '#e8f5e9', border: BORDER_DISTANCE, text: '#1b5e20' },
  ACCIDENT: { bg: '#ffebee', border: BORDER_HAZARD, text: '#c62828' },
  OUT_OF_GAS: { bg: '#ffebee', border: BORDER_HAZARD, text: '#c62828' },
  FLAT_TIRE: { bg: '#ffebee', border: BORDER_HAZARD, text: '#c62828' },
  STOP: { bg: '#ffebee', border: BORDER_HAZARD, text: '#c62828' },
  SPEED_LIMIT: { bg: '#ffebee', border: BORDER_HAZARD, text: '#c62828' },
  REPAIRS: { bg: '#e3f2fd', border: BORDER_REMEDY, text: '#1565c0' },
  GASOLINE: { bg: '#e3f2fd', border: BORDER_REMEDY, text: '#1565c0' },
  SPARE_TIRE: { bg: '#e3f2fd', border: BORDER_REMEDY, text: '#1565c0' },
  ROLL: { bg: '#e3f2fd', border: BORDER_REMEDY, text: '#1565c0' },
  END_OF_LIMIT: { bg: '#e3f2fd', border: BORDER_REMEDY, text: '#1565c0' },
  DRIVING_ACE: { bg: '#fff8e1', border: BORDER_SAFETY, text: '#f57f17' },
  EXTRA_TANK: { bg: '#fff8e1', border: BORDER_SAFETY, text: '#f57f17' },
  PUNCTURE_PROOF: { bg: '#fff8e1', border: BORDER_SAFETY, text: '#f57f17' },
  RIGHT_OF_WAY: { bg: '#fff8e1', border: BORDER_SAFETY, text: '#f57f17' },
}

const CARD_ICONS = {
  MILES_25: '25',
  MILES_50: '50',
  MILES_75: '75',
  MILES_100: '100',
  MILES_200: '200',
  ACCIDENT: '\u26A0',
  OUT_OF_GAS: '\u26FD',
  FLAT_TIRE: '\uD83D\uDEDE',
  STOP: '\uD83D\uDED1',
  SPEED_LIMIT: '50',
  REPAIRS: '\uD83D\uDD27',
  GASOLINE: '\u26FD',
  SPARE_TIRE: '\uD83D\uDEDE',
  ROLL: '\uD83D\uDFE2',
  END_OF_LIMIT: '\u274C50',
  DRIVING_ACE: '\u2B50\uD83D\uDD27',
  EXTRA_TANK: '\u2B50\u26FD',
  PUNCTURE_PROOF: '\u2B50\uD83D\uDEDE',
  RIGHT_OF_WAY: '\u2B50\uD83D\uDFE2',
}

const CARD_LABELS = {
  MILES_25: '25 mi',
  MILES_50: '50 mi',
  MILES_75: '75 mi',
  MILES_100: '100 mi',
  MILES_200: '200 mi',
  ACCIDENT: 'Accident',
  OUT_OF_GAS: 'Out of Gas',
  FLAT_TIRE: 'Flat Tire',
  STOP: 'Stop',
  SPEED_LIMIT: 'Speed Limit',
  REPAIRS: 'Repairs',
  GASOLINE: 'Gasoline',
  SPARE_TIRE: 'Spare Tire',
  ROLL: 'Roll',
  END_OF_LIMIT: 'End Limit',
  DRIVING_ACE: 'Driving Ace',
  EXTRA_TANK: 'Extra Tank',
  PUNCTURE_PROOF: 'Puncture Proof',
  RIGHT_OF_WAY: 'Right of Way',
}

export default function CardComponent({ card, onClick, selected, small, faceDown, draggable, dragIndex, style: extraStyle }) {
  const handleDragStart = (e) => {
    if (dragIndex != null) {
      e.dataTransfer.setData('text/plain', String(dragIndex))
      e.dataTransfer.effectAllowed = 'move'
    }
  }

  if (faceDown) {
    return (
      <div
        onClick={onClick}
        style={{
          width: small ? 100 : 160,
          height: small ? 140 : 220,
          borderRadius: 8,
          background: 'linear-gradient(135deg, #1565c0, #0d47a1)',
          border: '2px solid #0d47a1',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: onClick ? 'pointer' : 'default',
          fontSize: small ? 32 : 48,
          color: '#fff',
          fontWeight: 'bold',
          ...extraStyle
        }}
      >
        MB
      </div>
    )
  }

  if (!card) return null

  const colors = CARD_COLORS[card] || { bg: '#eee', border: '#999', text: '#333' }
  const icon = CARD_ICONS[card] || '?'
  const label = CARD_LABELS[card] || card

  return (
    <div
      onClick={onClick}
      draggable={!!draggable}
      onDragStart={handleDragStart}
      data-drag-index={draggable && dragIndex != null ? dragIndex : undefined}
      style={{
        width: small ? 100 : 160,
        height: small ? 140 : 220,
        borderRadius: 8,
        background: colors.bg,
        border: `3px solid ${selected ? '#ffd700' : colors.border}`,
        boxShadow: selected ? '0 0 12px rgba(255,215,0,0.8)' : '0 2px 4px rgba(0,0,0,0.3)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        cursor: draggable ? 'grab' : (onClick ? 'pointer' : 'default'),
        transition: 'transform 0.15s, box-shadow 0.15s',
        transform: selected ? 'translateY(-8px)' : 'none',
        padding: small ? 4 : 8,
        ...extraStyle
      }}
    >
      <span style={{
        fontSize: small ? 28 : 44,
        lineHeight: 1
      }}>
        {icon}
      </span>
      <span style={{
        fontSize: small ? 14 : 20,
        fontWeight: 'bold',
        color: colors.text,
        textAlign: 'center',
        marginTop: small ? 4 : 8,
        lineHeight: 1.1
      }}>
        {label}
      </span>
    </div>
  )
}

export { CARD_LABELS, CARD_COLORS }
