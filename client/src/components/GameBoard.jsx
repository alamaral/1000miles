import React from 'react'
import PlayerArea from './PlayerArea'

export default function GameBoard({ gameState, playerId, onDropOnPlayer }) {
  if (!gameState?.players) return null

  // Team mode: group players by teamIndex and render one PlayerArea per team
  if (gameState.useTeams && gameState.teamNames && Object.keys(gameState.teamNames).length > 0) {
    // Group players by teamIndex
    const teamMap = {}
    for (const p of gameState.players) {
      const ti = p.teamIndex
      if (ti == null || ti < 0) continue
      if (!teamMap[ti]) teamMap[ti] = []
      teamMap[ti].push(p)
    }

    const teamIndices = Object.keys(teamMap).map(Number).sort((a, b) => a - b)

    // Determine which team the current player is on
    const myPlayer = gameState.players.find(p => p.id === playerId)
    const myTeamIndex = myPlayer?.teamIndex ?? -1

    const myTeam = myTeamIndex >= 0 ? teamMap[myTeamIndex] : null
    const opponentTeams = teamIndices.filter(ti => ti !== myTeamIndex)

    // Render a team area
    const renderTeamArea = (teamIdx, isYou) => {
      const members = teamMap[teamIdx]
      if (!members || members.length === 0) return null
      const rep = members[0]
      const teamName = gameState.teamNames[teamIdx] || `Team ${teamIdx + 1}`
      const isCurrentTurn = members.some(m => m.id === gameState.currentPlayerId)
      const teamMemberIds = members.map(m => m.id).join(',')

      // For teammate hand sizes: show both
      const teammateHandSizes = members.length > 1
        ? members.map(m => ({ name: m.name, handSize: m.handSize }))
        : null

      return (
        <PlayerArea
          key={`team-${teamIdx}`}
          player={rep}
          displayName={teamName}
          teammateHandSizes={teammateHandSizes}
          teamMemberIds={teamMemberIds}
          isCurrentTurn={isCurrentTurn}
          isYou={isYou}
          mileTarget={gameState.mileTarget}
          onDrop={(cardIndex) => onDropOnPlayer(cardIndex, isYou ? null : rep.id)}
        />
      )
    }

    return (
      <div>
        {/* Opponent teams across the top */}
        <div style={{
          display: 'flex',
          gap: 12,
          justifyContent: 'center',
          flexWrap: 'wrap',
          marginBottom: 16
        }}>
          {opponentTeams.map(ti => renderTeamArea(ti, false))}
        </div>

        {/* Your team at bottom */}
        {myTeam && renderTeamArea(myTeamIndex, true)}
      </div>
    )
  }

  // Non-team mode: original behavior
  const self = gameState.players.find(p => p.id === playerId)
  const opponents = gameState.players.filter(p => p.id !== playerId)

  return (
    <div>
      {/* Opponents across the top */}
      <div style={{
        display: 'flex',
        gap: 12,
        justifyContent: 'center',
        flexWrap: 'wrap',
        marginBottom: 16
      }}>
        {opponents.map(p => (
          <PlayerArea
            key={p.id}
            player={p}
            isCurrentTurn={p.id === gameState.currentPlayerId}
            isYou={false}
            mileTarget={gameState.mileTarget}
            onDrop={(cardIndex) => onDropOnPlayer(cardIndex, p.id)}
          />
        ))}
      </div>

      {/* Your area at bottom */}
      {self && (
        <PlayerArea
          player={self}
          isCurrentTurn={self.id === gameState.currentPlayerId}
          isYou={true}
          mileTarget={gameState.mileTarget}
          onDrop={(cardIndex) => onDropOnPlayer(cardIndex, null)}
        />
      )}
    </div>
  )
}
