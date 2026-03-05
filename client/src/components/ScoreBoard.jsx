import React from 'react'
import { useGame } from '../context/GameContext'

// Official Mille Bornes scoring categories in order
const SCORE_ROWS = [
  { key: 'distancePoints',    label: 'Distance',        desc: '1 pt per mile' },
  { key: 'safetyPoints',      label: 'Safeties',        desc: '100 pts each' },
  { key: 'allSafetiesBonus',  label: 'All 4 Safeties',  desc: '300 pts' },
  { key: 'coupFourrePoints',  label: 'Coup Fourr\u00e9', desc: '300 pts each' },
  { key: 'tripCompletedBonus', label: 'Trip Completed', desc: '400 pts' },
  { key: 'delayedActionBonus', label: 'Delayed Action', desc: '300 pts' },
  { key: 'safeTripBonus',     label: 'Safe Trip',       desc: '300 pts' },
  { key: 'shutoutBonus',      label: 'Shutout',         desc: '500 pts each' },
]

export default function ScoreBoard() {
  const { scores, gameState, startNewHand, resetRoom, playerId, lobby } = useGame()

  if (!scores) return null

  const isGameOver = scores.gameComplete
  const isHost = lobby?.players?.[0]?.id === playerId
  const playerScores = scores.scores || []
  const playerCount = playerScores.length

  const useTeams = gameState?.useTeams

  // Find winner for game over
  const getWinnerName = () => {
    if (useTeams && playerScores.length > 0) {
      // Find the score sheet with highest cumulative
      let bestSheet = playerScores[0]
      let bestCum = scores.cumulativeScores?.[bestSheet.playerId] || 0
      for (const s of playerScores) {
        const cum = scores.cumulativeScores?.[s.playerId] || 0
        if (cum > bestCum) { bestSheet = s; bestCum = cum }
      }
      return bestSheet.teamName || gameState?.players?.find(p => p.id === bestSheet.playerId)?.name || '?'
    }
    const entries = Object.entries(scores.cumulativeScores || {})
    if (entries.length === 0) return '?'
    entries.sort((a, b) => b[1] - a[1])
    const winnerId = entries[0][0]
    return gameState?.players?.find(p => p.id === winnerId)?.name || winnerId
  }

  // Column width for player columns
  const colWidth = playerCount <= 3 ? 100 : 80

  return (
    <div style={{
      position: 'fixed',
      top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(0,0,0,0.85)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000
    }}>
      <div style={{
        background: '#f5f0e1',
        border: '3px solid #8b7355',
        borderRadius: 4,
        padding: 0,
        maxWidth: 700,
        width: '95%',
        maxHeight: '90vh',
        overflow: 'auto',
        boxShadow: '0 8px 32px rgba(0,0,0,0.5)',
        fontFamily: '"Courier New", Courier, monospace'
      }}>
        {/* Header */}
        <div style={{
          background: '#1a472a',
          padding: '16px 24px',
          textAlign: 'center',
          borderBottom: '3px solid #8b7355'
        }}>
          <h2 style={{
            margin: 0, color: '#ffd700', fontSize: 28,
            fontFamily: 'Georgia, serif', letterSpacing: 3,
            textTransform: 'uppercase'
          }}>
            Mille Bornes
          </h2>
          <div style={{ color: '#aed581', fontSize: 14, marginTop: 4, letterSpacing: 2 }}>
            {isGameOver
              ? 'Final Score'
              : `Hand #${(gameState?.handNumber ?? 0) + 1} Score`}
          </div>
        </div>

        {/* Score table */}
        <table style={{
          width: '100%', borderCollapse: 'collapse',
          fontSize: 15, color: '#2c1810'
        }}>
          <thead>
            <tr style={{ background: '#e8dcc8' }}>
              <th style={headerCellStyle}>Category</th>
              {playerScores.map(s => {
                const player = gameState?.players?.find(p => p.id === s.playerId)
                const label = (useTeams && s.teamName) ? s.teamName : (player?.name || '?')
                return (
                  <th key={s.playerId} style={{
                    ...headerCellStyle,
                    width: colWidth, minWidth: colWidth,
                    maxWidth: colWidth + 20,
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }}>
                    {label}
                  </th>
                )
              })}
            </tr>
          </thead>
          <tbody>
            {SCORE_ROWS.map((row, i) => {
              const allZero = playerScores.every(s => (s[row.key] || 0) === 0)
              return (
                <tr key={row.key} style={{
                  background: i % 2 === 0 ? '#f5f0e1' : '#ede7d5'
                }}>
                  <td style={labelCellStyle}>
                    <span>{row.label}</span>
                    <span style={{ fontSize: 11, color: '#8b7355', marginLeft: 6 }}>
                      {row.desc}
                    </span>
                  </td>
                  {playerScores.map(s => {
                    const val = s[row.key] || 0
                    return (
                      <td key={s.playerId} style={{
                        ...valueCellStyle,
                        color: val > 0 ? '#2c1810' : '#bbb',
                        fontWeight: val > 0 ? 'bold' : 'normal'
                      }}>
                        {val > 0 ? val.toLocaleString() : (allZero ? '\u2014' : '0')}
                      </td>
                    )
                  })}
                </tr>
              )
            })}

            {/* Divider */}
            <tr><td colSpan={playerCount + 1} style={{ height: 2, background: '#8b7355', padding: 0 }} /></tr>

            {/* Hand Total */}
            <tr style={{ background: '#d4c9a8' }}>
              <td style={{ ...labelCellStyle, fontWeight: 'bold', fontSize: 16 }}>
                Hand Total
              </td>
              {playerScores.map(s => (
                <td key={s.playerId} style={{
                  ...valueCellStyle,
                  fontWeight: 'bold', fontSize: 18, color: '#1a472a'
                }}>
                  {(s.totalHandScore || 0).toLocaleString()}
                </td>
              ))}
            </tr>

            {/* Previous Total (cumulative minus this hand) */}
            <tr style={{ background: '#e8dcc8' }}>
              <td style={{ ...labelCellStyle, color: '#8b7355' }}>
                Previous Total
              </td>
              {playerScores.map(s => {
                const cumulative = scores.cumulativeScores?.[s.playerId] || 0
                const prev = cumulative - (s.totalHandScore || 0)
                return (
                  <td key={s.playerId} style={{
                    ...valueCellStyle, color: '#8b7355'
                  }}>
                    {prev > 0 ? prev.toLocaleString() : '\u2014'}
                  </td>
                )
              })}
            </tr>

            {/* Divider */}
            <tr><td colSpan={playerCount + 1} style={{ height: 3, background: '#1a472a', padding: 0 }} /></tr>

            {/* Grand Total */}
            <tr style={{ background: '#1a472a' }}>
              <td style={{
                ...labelCellStyle,
                color: '#ffd700', fontWeight: 'bold', fontSize: 18,
                borderBottom: 'none'
              }}>
                Grand Total
              </td>
              {playerScores.map(s => {
                const cumulative = scores.cumulativeScores?.[s.playerId] || 0
                return (
                  <td key={s.playerId} style={{
                    ...valueCellStyle,
                    color: '#ffd700', fontWeight: 'bold', fontSize: 22,
                    borderBottom: 'none'
                  }}>
                    {cumulative.toLocaleString()}
                  </td>
                )
              })}
            </tr>
          </tbody>
        </table>

        {/* Footer with buttons / winner announcement */}
        <div style={{
          padding: '16px 24px',
          background: '#1a472a',
          borderTop: '2px solid #8b7355',
          textAlign: 'center'
        }}>
          {isGameOver && (
            <p style={{
              fontSize: 28, color: '#ffd700', fontWeight: 'bold',
              margin: '0 0 12px 0', fontFamily: 'Georgia, serif'
            }}>
              Winner: {getWinnerName()}
            </p>
          )}

          {!isGameOver && (
            <button onClick={startNewHand} style={buttonStyle}>
              Next Hand
            </button>
          )}

          {isGameOver && isHost && (
            <button onClick={resetRoom} style={{
              ...buttonStyle, background: '#ff6f00'
            }}>
              Return to Lobby
            </button>
          )}

          {isGameOver && !isHost && (
            <p style={{ color: '#aed581', fontSize: 16, margin: 0 }}>
              Waiting for host...
            </p>
          )}
        </div>
      </div>
    </div>
  )
}

const headerCellStyle = {
  padding: '10px 8px',
  borderBottom: '2px solid #8b7355',
  textAlign: 'center',
  fontSize: 15,
  fontWeight: 'bold',
  color: '#2c1810',
  fontFamily: 'Georgia, serif'
}

const labelCellStyle = {
  padding: '7px 12px',
  borderBottom: '1px solid rgba(139,115,85,0.2)',
  textAlign: 'left',
  fontSize: 14
}

const valueCellStyle = {
  padding: '7px 8px',
  borderBottom: '1px solid rgba(139,115,85,0.2)',
  textAlign: 'center',
  fontSize: 16,
  fontFamily: '"Courier New", Courier, monospace'
}

const buttonStyle = {
  padding: '10px 28px',
  fontSize: 20,
  fontWeight: 'bold',
  borderRadius: 8,
  border: 'none',
  background: '#ffd700',
  color: '#1a472a',
  cursor: 'pointer',
  fontFamily: 'Georgia, serif',
  letterSpacing: 1
}
