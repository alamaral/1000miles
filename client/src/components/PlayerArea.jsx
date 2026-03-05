import React, { useState } from 'react'
import CardComponent from './CardComponent'

const CARD_VALUES = {
  MILES_25: 25, MILES_50: 50, MILES_75: 75, MILES_100: 100, MILES_200: 200
}

/**
 * Group small distance cards (25/50/75) into bundles of 100.
 * Returns an array of bundles, where each bundle is { cards: [...], total: number }.
 * 100 and 200 mile cards get their own single-card bundle.
 */
function groupDistanceCards(distancePile) {
  if (!distancePile || distancePile.length === 0) return []

  const big = []      // 100 and 200 cards as solo bundles
  const smalls = []   // 25, 50, 75 values to pack

  for (const card of distancePile) {
    const v = CARD_VALUES[card] || 0
    if (v >= 100) {
      big.push({ cards: [card], total: v })
    } else {
      smalls.push(card)
    }
  }

  // Sort smalls descending so greedy packing works well
  smalls.sort((a, b) => (CARD_VALUES[b] || 0) - (CARD_VALUES[a] || 0))

  const bundles = [] // each: { cards: [], total: number }

  for (const card of smalls) {
    const v = CARD_VALUES[card] || 0
    // Try to fit into an existing open bundle
    let placed = false
    for (const b of bundles) {
      if (b.total + v <= 100) {
        b.cards.push(card)
        b.total += v
        placed = true
        break
      }
    }
    if (!placed) {
      bundles.push({ cards: [card], total: v })
    }
  }

  return [...big, ...bundles]
}

function DistanceBundle({ bundle }) {
  const isFull = bundle.total >= 100
  const cardCount = bundle.cards.length

  if (cardCount === 1) {
    // Single card — just render it
    return (
      <div style={{ position: 'relative', width: 100, height: 140 }}>
        <CardComponent card={bundle.cards[0]} small />
      </div>
    )
  }

  // Stack multiple cards with a slight offset
  return (
    <div style={{
      position: 'relative',
      width: 100 + (cardCount - 1) * 8,
      height: 140 + (cardCount - 1) * 6,
    }}>
      {bundle.cards.map((card, i) => (
        <div key={i} style={{
          position: 'absolute',
          top: i * 6,
          left: i * 8,
          zIndex: i,
        }}>
          <CardComponent card={card} small />
        </div>
      ))}
      {/* Bundle total badge */}
      <div style={{
        position: 'absolute',
        bottom: -4,
        right: -4,
        background: isFull ? '#4caf50' : '#ff9800',
        color: '#fff',
        fontSize: 24,
        fontWeight: 'bold',
        borderRadius: 10,
        padding: '2px 8px',
        zIndex: cardCount + 1,
        boxShadow: '0 1px 3px rgba(0,0,0,0.4)',
      }}>
        {bundle.total}
      </div>
    </div>
  )
}

export default function PlayerArea({ player, isCurrentTurn, isYou, mileTarget, onDrop, displayName, teammateHandSizes, teamMemberIds }) {
  const [dragOver, setDragOver] = useState(false)

  const borderColor = dragOver ? '#ff6f00'
    : isCurrentTurn ? '#ffd700'
    : isYou ? '#4caf50'
    : 'rgba(255,255,255,0.2)'

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

  const bundles = groupDistanceCards(player.distancePile)

  return (
    <div
      data-drop-target="player"
      data-player-id={player.id}
      {...(teamMemberIds ? { 'data-team-members': teamMemberIds } : {})}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
      style={{
        background: dragOver ? 'rgba(255,111,0,0.2)'
          : isYou ? 'rgba(76,175,80,0.15)'
          : 'rgba(0,0,0,0.2)',
        border: `3px solid ${borderColor}`,
        borderRadius: 12,
        padding: 12,
        transition: 'border-color 0.15s, background 0.15s',
        minWidth: 200
      }}
    >
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 8
      }}>
        <span style={{
          fontWeight: 'bold',
          color: isCurrentTurn ? '#ffd700' : '#fff',
          fontSize: 28
        }}>
          {displayName || player.name} {isYou ? '(You)' : ''}
        </span>
        <span style={{
          fontSize: 40,
          fontWeight: 'bold',
          color: '#4caf50'
        }}>
          {player.totalMiles} mi
        </span>
      </div>

      {/* Mile progress bar */}
      <div style={{
        height: 6,
        background: 'rgba(255,255,255,0.1)',
        borderRadius: 3,
        marginBottom: 8,
        overflow: 'hidden'
      }}>
        <div style={{
          height: '100%',
          width: `${Math.min(100, (player.totalMiles / (mileTarget || 700)) * 100)}%`,
          background: 'linear-gradient(90deg, #4caf50, #8bc34a)',
          borderRadius: 3,
          transition: 'width 0.5s'
        }} />
      </div>

      <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
        {/* Battle pile top */}
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 18, color: '#aaa', marginBottom: 2 }}>Battle</div>
          {player.battleTop ? (
            <CardComponent card={player.battleTop} small />
          ) : (
            <div style={{
              width: 100, height: 140, borderRadius: 6,
              border: '1px dashed rgba(255,255,255,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 18, color: '#666'
            }}>Empty</div>
          )}
        </div>

        {/* Speed pile top */}
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 18, color: '#aaa', marginBottom: 2 }}>Speed</div>
          {player.speedTop ? (
            <CardComponent card={player.speedTop} small />
          ) : (
            <div style={{
              width: 100, height: 140, borderRadius: 6,
              border: '1px dashed rgba(255,255,255,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 18, color: '#666'
            }}>---</div>
          )}
        </div>

        {/* Safeties — CF safeties displayed landscape (rotated 90°) */}
        {player.safetyArea?.length > 0 && (
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 18, color: '#aaa', marginBottom: 2 }}>Safeties</div>
            <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
              {player.safetyArea.map((card, i) => {
                const isCF = player.coupFourreSafeties?.includes(card)
                return isCF ? (
                  <div key={i} style={{
                    width: 140,
                    height: 100,
                    position: 'relative',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                  }}>
                    <div style={{ transform: 'rotate(90deg)' }}>
                      <CardComponent card={card} small />
                    </div>
                  </div>
                ) : (
                  <CardComponent key={i} card={card} small />
                )
              })}
            </div>
          </div>
        )}

        {/* Distance cards grouped into bundles */}
        {bundles.length > 0 && (
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 18, color: '#aaa', marginBottom: 2 }}>Distance</div>
            <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'flex-end' }}>
              {bundles.map((bundle, i) => (
                <DistanceBundle key={i} bundle={bundle} />
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Cards in hand count */}
      <div style={{ fontSize: 22, color: '#888', marginTop: 6 }}>
        {teammateHandSizes
          ? teammateHandSizes.map((m, i) => (
              <span key={i}>
                {i > 0 ? ' / ' : ''}
                {m.name}: {m.handSize}
              </span>
            ))
          : `Cards: ${player.handSize}`
        }
      </div>
    </div>
  )
}
