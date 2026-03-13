import React, { useState, useEffect } from 'react'

// Check which card images exist in /cards/. Tries webp, apng, gif, png in order.
// Caches results so each image is probed at most once.
const imageCache = {}
const IMAGE_EXTS = ['webp', 'apng', 'gif', 'png']

function probeImage(name, exts, onFound, onNone) {
  if (exts.length === 0) { onNone(); return }
  const [ext, ...rest] = exts
  const path = `/cards/${name}.${ext}`
  const img = new Image()
  img.onload = () => onFound(path)
  img.onerror = () => probeImage(name, rest, onFound, onNone)
  img.src = path
}

function useCardImage(name) {
  const [src, setSrc] = useState(imageCache[name] || null)
  useEffect(() => {
    if (!name) return
    if (imageCache[name] !== undefined) { setSrc(imageCache[name]); return }
    probeImage(name, IMAGE_EXTS,
      (path) => { imageCache[name] = path; setSrc(path) },
      () => { imageCache[name] = null; setSrc(null) }
    )
  }, [name])
  return src
}

const BORDER_HAZARD = '#f44336'
const BORDER_REMEDY = '#4caf50'
const BORDER_SAFETY = '#ffd700'

const DISTANCE_COLORS = {
  MILES_25:  { bg: '#f5f5f5', border: '#388e3c', text: '#2e7d32', accent: '#43a047' },
  MILES_50:  { bg: '#f5f5f5', border: '#1565c0', text: '#1565c0', accent: '#1e88e5' },
  MILES_75:  { bg: '#f5f5f5', border: '#6a1b9a', text: '#6a1b9a', accent: '#8e24aa' },
  MILES_100: { bg: '#f5f5f5', border: '#e65100', text: '#e65100', accent: '#f4511e' },
  MILES_200: { bg: '#f5f5f5', border: '#c62828', text: '#c62828', accent: '#e53935' },
}

const CARD_COLORS = {
  MILES_25:  DISTANCE_COLORS.MILES_25,
  MILES_50:  DISTANCE_COLORS.MILES_50,
  MILES_75:  DISTANCE_COLORS.MILES_75,
  MILES_100: DISTANCE_COLORS.MILES_100,
  MILES_200: DISTANCE_COLORS.MILES_200,
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
  const imageSrc = useCardImage(faceDown ? 'CARD_BACK' : card)

  const handleDragStart = (e) => {
    if (dragIndex != null) {
      e.dataTransfer.setData('text/plain', String(dragIndex))
      e.dataTransfer.effectAllowed = 'move'
    }
  }

  // Image-based rendering (used when a matching PNG exists in /cards/)
  if (imageSrc) {
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
          boxShadow: selected ? '0 0 12px rgba(255,215,0,0.8)' : '0 2px 4px rgba(0,0,0,0.3)',
          cursor: draggable ? 'grab' : (onClick ? 'pointer' : 'default'),
          transition: 'transform 0.15s, box-shadow 0.15s',
          transform: selected ? 'translateY(-8px)' : 'none',
          overflow: 'hidden',
          outline: selected ? '3px solid #ffd700' : 'none',
          ...extraStyle
        }}
      >
        <img src={imageSrc} alt={card || 'card back'} draggable={false} style={{
          width: '100%', height: '100%', objectFit: 'fill', display: 'block',
        }} />
      </div>
    )
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
  const isDistance = card.startsWith('MILES_')

  const baseStyle = {
    width: small ? 100 : 160,
    height: small ? 140 : 220,
    borderRadius: 8,
    border: `3px solid ${selected ? '#ffd700' : colors.border}`,
    boxShadow: selected ? '0 0 12px rgba(255,215,0,0.8)' : '0 2px 4px rgba(0,0,0,0.3)',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    cursor: draggable ? 'grab' : (onClick ? 'pointer' : 'default'),
    transition: 'transform 0.15s, box-shadow 0.15s',
    transform: selected ? 'translateY(-8px)' : 'none',
    overflow: 'hidden',
    position: 'relative',
    ...extraStyle
  }

  if (isDistance) {
    const miles = card.replace('MILES_', '')
    const dc = colors
    return (
      <div
        onClick={onClick}
        draggable={!!draggable}
        onDragStart={handleDragStart}
        data-drag-index={draggable && dragIndex != null ? dragIndex : undefined}
        style={{
          ...baseStyle,
          background: dc.bg,
          padding: 0,
        }}
      >
        {/* Top-left and bottom-right corner numbers */}
        <span style={{
          position: 'absolute', top: small ? 4 : 6, left: small ? 6 : 8,
          fontSize: small ? 12 : 16, fontWeight: 'bold', color: dc.text, lineHeight: 1
        }}>{miles}</span>
        <span style={{
          position: 'absolute', bottom: small ? 4 : 6, right: small ? 6 : 8,
          fontSize: small ? 12 : 16, fontWeight: 'bold', color: dc.text, lineHeight: 1,
          transform: 'rotate(180deg)'
        }}>{miles}</span>

        {/* Colored stripe across the middle */}
        <div style={{
          position: 'absolute',
          top: '50%', left: 0, right: 0,
          transform: 'translateY(-50%)',
          height: small ? 50 : 80,
          background: `linear-gradient(135deg, ${dc.accent}, ${dc.border})`,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}>
          <span style={{
            fontSize: small ? 32 : 52,
            fontWeight: 900,
            color: '#fff',
            textShadow: '1px 2px 3px rgba(0,0,0,0.3)',
            letterSpacing: small ? 1 : 2,
            lineHeight: 1,
          }}>{miles}</span>
        </div>

        {/* "mi" label below stripe */}
        <span style={{
          position: 'absolute',
          bottom: small ? 16 : 24,
          fontSize: small ? 11 : 14,
          fontWeight: 'bold',
          color: dc.text,
          textTransform: 'uppercase',
          letterSpacing: small ? 2 : 4,
        }}>miles</span>
      </div>
    )
  }

  return (
    <div
      onClick={onClick}
      draggable={!!draggable}
      onDragStart={handleDragStart}
      data-drag-index={draggable && dragIndex != null ? dragIndex : undefined}
      style={{
        ...baseStyle,
        background: colors.bg,
        padding: small ? 4 : 8,
      }}
    >
      <span style={{
        fontSize: small ? 28 : 44,
        lineHeight: 1,
        color: colors.text,
        textShadow: '0 0 3px rgba(0,0,0,0.2)',
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
