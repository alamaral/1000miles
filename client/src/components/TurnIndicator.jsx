import React from 'react'

export default function TurnIndicator({ gameState, playerId }) {
  if (!gameState) return null

  const isYourTurn = gameState.currentPlayerId === playerId
  const currentPlayer = gameState.players?.find(p => p.id === gameState.currentPlayerId)
  const phase = gameState.turnPhase

  let message = ''
  if (gameState.phase === 'HAND_OVER') {
    message = 'Hand Over - Calculating scores...'
  } else if (gameState.phase === 'GAME_OVER') {
    message = 'Game Over!'
  } else if (phase === 'COUP_FOURRE_WINDOW') {
    message = 'Coup Fourr\u00e9 window open!'
  } else if (isYourTurn) {
    message = phase === 'DRAW' ? 'Your turn - Draw a card!' : 'Your turn - Play or discard a card'
  } else {
    let turnName = currentPlayer?.name || '...'
    if (gameState.useTeams && gameState.teamNames && currentPlayer?.teamIndex >= 0) {
      const teamName = gameState.teamNames[currentPlayer.teamIndex]
      if (teamName) turnName = `${currentPlayer.name} (${teamName})`
    }
    message = `${turnName}'s turn${phase === 'DRAW' ? ' (drawing)' : ' (playing)'}`
  }

  return (
    <div style={{
      textAlign: 'center',
      padding: '10px 20px',
      background: isYourTurn ? 'rgba(255,215,0,0.2)' : 'rgba(0,0,0,0.3)',
      borderRadius: 8,
      border: `1px solid ${isYourTurn ? '#ffd700' : 'rgba(255,255,255,0.1)'}`,
      marginBottom: 12
    }}>
      <span style={{
        fontWeight: 'bold',
        fontSize: 32,
        color: isYourTurn ? '#ffd700' : '#fff'
      }}>
        {message}
      </span>
      <span style={{
        display: 'block',
        fontSize: 24,
        color: '#aed581',
        marginTop: 4
      }}>
        Hand #{gameState.handNumber} | Target: {gameState.mileTarget} mi
      </span>
    </div>
  )
}
