import React, { useState, useEffect } from 'react'
import { useGame } from '../context/GameContext'
import { CARD_LABELS } from './CardComponent'

export default function CoupFourrePrompt() {
  const { gameState, playerId, hand, declareCoupFourre } = useGame()

  const [dismissed, setDismissed] = useState(false)

  const hazardType = gameState?.coupFourreHazardType
  const sourcePlayerId = gameState?.coupFourreSourcePlayerId
  const targetPlayerId = gameState?.coupFourreTargetPlayerId

  // Reset dismissed state whenever the CF opportunity changes
  // (cleared by a draw, or a new hazard sets different metadata)
  useEffect(() => {
    setDismissed(false)
  }, [hazardType, sourcePlayerId, targetPlayerId])

  // No CF opportunity active, or I'm the one who played the hazard
  if (!hazardType || playerId === sourcePlayerId) return null

  // Already dismissed this CF opportunity
  if (dismissed) return null

  // Find the safety card in my hand
  const safetyMap = {
    ACCIDENT: 'DRIVING_ACE',
    OUT_OF_GAS: 'EXTRA_TANK',
    FLAT_TIRE: 'PUNCTURE_PROOF',
    STOP: 'RIGHT_OF_WAY',
    SPEED_LIMIT: 'RIGHT_OF_WAY'
  }
  const neededSafety = safetyMap[hazardType]
  const safetyIndex = hand.findIndex(c => c === neededSafety)

  // I don't have the matching safety — don't show the prompt
  if (safetyIndex < 0) return null

  // Find the victim's name for context
  const victim = gameState?.players?.find(p => p.id === targetPlayerId)
  const isVictim = targetPlayerId === playerId

  return (
    <div style={{
      position: 'fixed',
      top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(0,0,0,0.7)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000
    }}>
      <div style={{
        background: '#1a472a',
        border: '3px solid #ffd700',
        borderRadius: 16,
        padding: 32,
        textAlign: 'center',
        maxWidth: 400
      }}>
        <h2 style={{ color: '#ffd700', marginBottom: 12 }}>Coup Fourr&eacute;!</h2>
        <p style={{ marginBottom: 8 }}>
          {isVictim
            ? <>You were hit with a <strong style={{ color: '#f44336' }}>
                {hazardType?.replace('_', ' ')}
              </strong></>
            : <><strong>{victim?.name || 'A player'}</strong> was hit with a{' '}
              <strong style={{ color: '#f44336' }}>
                {hazardType?.replace('_', ' ')}
              </strong></>
          }
        </p>
        <p style={{ marginBottom: 16 }}>
          You have <strong style={{ color: '#ffd700' }}>
            {CARD_LABELS[neededSafety]}
          </strong>! Declare Coup Fourr&eacute;?
        </p>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
          <button
            onClick={() => declareCoupFourre(safetyIndex)}
            style={{
              padding: '12px 32px',
              fontSize: 16,
              fontWeight: 'bold',
              borderRadius: 8,
              border: 'none',
              background: '#ffd700',
              color: '#1a472a',
              cursor: 'pointer'
            }}
          >
            Coup Fourr&eacute;!
          </button>
          <button
            onClick={() => setDismissed(true)}
            style={{
              padding: '12px 24px',
              fontSize: 14,
              borderRadius: 8,
              border: '1px solid #666',
              background: 'transparent',
              color: '#999',
              cursor: 'pointer'
            }}
          >
            Pass
          </button>
        </div>
      </div>
    </div>
  )
}
