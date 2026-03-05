import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGame } from '../context/GameContext'
import { API_BASE } from '../services/api'

const styles = {
  container: {
    maxWidth: 600,
    margin: '0 auto',
    padding: 40,
    textAlign: 'center'
  },
  title: {
    fontSize: 48,
    fontWeight: 'bold',
    marginBottom: 8,
    color: '#ffd700',
    textShadow: '2px 2px 4px rgba(0,0,0,0.5)'
  },
  subtitle: {
    fontSize: 18,
    color: '#aed581',
    marginBottom: 40
  },
  input: {
    width: '100%',
    padding: '12px 16px',
    fontSize: 16,
    borderRadius: 8,
    border: '2px solid #4caf50',
    background: '#1a3a1a',
    color: '#fff',
    marginBottom: 16,
    outline: 'none'
  },
  buttonRow: {
    display: 'flex',
    gap: 12,
    marginBottom: 24
  },
  button: {
    flex: 1,
    padding: '12px 24px',
    fontSize: 16,
    fontWeight: 'bold',
    borderRadius: 8,
    border: 'none',
    cursor: 'pointer',
    transition: 'transform 0.1s'
  },
  createBtn: {
    background: '#ffd700',
    color: '#1a472a'
  },
  joinBtn: {
    background: '#4caf50',
    color: '#fff'
  },
  disabledBtn: {
    background: '#555',
    color: '#999',
    cursor: 'not-allowed'
  },
  section: {
    background: 'rgba(0,0,0,0.2)',
    borderRadius: 12,
    padding: 24,
    marginBottom: 24
  },
  lobbyInfo: {
    textAlign: 'left',
    marginTop: 20
  },
  roomCode: {
    fontSize: 36,
    fontWeight: 'bold',
    color: '#ffd700',
    letterSpacing: 8,
    marginBottom: 16
  },
  playerList: {
    listStyle: 'none',
    padding: 0
  },
  playerItem: {
    padding: '8px 16px',
    background: 'rgba(255,255,255,0.1)',
    borderRadius: 6,
    marginBottom: 4,
    display: 'flex',
    justifyContent: 'space-between'
  },
  startBtn: {
    width: '100%',
    padding: '14px 24px',
    fontSize: 18,
    fontWeight: 'bold',
    borderRadius: 8,
    border: 'none',
    cursor: 'pointer',
    background: '#ff6f00',
    color: '#fff',
    marginTop: 16
  },
  error: {
    color: '#ff5252',
    marginBottom: 16,
    fontWeight: 'bold'
  }
}

export default function LobbyPage() {
  const navigate = useNavigate()
  const { playerName, setPlayerName, createRoom, joinRoom, spectateRoom, startGame, leaveRoom,
    roomCode, lobby, playerId, error, setError, choosePartner } = useGame()
  const [joinCode, setJoinCode] = useState('')
  const [localError, setLocalError] = useState('')
  const [gameInProgress, setGameInProgress] = useState(false)

  // Check if game is in progress when a full room code is entered
  useEffect(() => {
    if (joinCode.length !== 4) {
      setGameInProgress(false)
      return
    }
    let cancelled = false
    fetch(`${API_BASE}/api/lobby/spectate/${joinCode}`)
      .then(r => r.json())
      .then(data => {
        if (!cancelled) setGameInProgress(!!data.gameInProgress)
      })
      .catch(() => {
        if (!cancelled) setGameInProgress(false)
      })
    return () => { cancelled = true }
  }, [joinCode])

  const handleCreate = async () => {
    if (!playerName.trim()) {
      setLocalError('Enter your name first')
      return
    }
    try {
      const code = await createRoom(playerName.trim())
      navigate(`/game/${code}`)
    } catch (e) {
      setLocalError(e.message)
    }
  }

  const handleJoin = async () => {
    if (!playerName.trim()) {
      setLocalError('Enter your name first')
      return
    }
    if (!joinCode.trim()) {
      setLocalError('Enter a room code')
      return
    }
    try {
      const code = await joinRoom(joinCode.trim().toUpperCase(), playerName.trim())
      navigate(`/game/${code}`)
    } catch (e) {
      setLocalError(e.message)
    }
  }

  const handleWatch = async () => {
    if (!joinCode.trim()) {
      setLocalError('Enter a room code to watch')
      return
    }
    try {
      const code = await spectateRoom(joinCode.trim().toUpperCase(), playerName.trim() || undefined)
      navigate(`/game/${code}`)
    } catch (e) {
      setLocalError(e.message)
    }
  }

  const handleStart = async () => {
    try {
      await startGame()
    } catch (e) {
      setLocalError(e.message)
    }
  }

  const joinDisabled = gameInProgress

  return (
    <div style={styles.container}>
      <h1 style={styles.title}>Mille Bornes</h1>
      <p style={styles.subtitle}>The Classic French Card Game</p>

      {(localError || error) && (
        <p style={styles.error}>{localError || error}</p>
      )}

      {!roomCode ? (
        <div style={styles.section}>
          <input
            style={styles.input}
            placeholder="Your name"
            value={playerName}
            onChange={e => { setPlayerName(e.target.value); setLocalError('') }}
            onKeyDown={e => e.key === 'Enter' && handleCreate()}
          />
          <div style={styles.buttonRow}>
            <button style={{ ...styles.button, ...styles.createBtn }} onClick={handleCreate}>
              Create Room
            </button>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, flexWrap: 'wrap' }}>
            <input
              style={{ ...styles.input, marginBottom: 0, flex: '1 1 120px', minWidth: 120 }}
              placeholder="Room code"
              value={joinCode}
              onChange={e => { setJoinCode(e.target.value.toUpperCase()); setLocalError('') }}
              maxLength={4}
              onKeyDown={e => e.key === 'Enter' && !joinDisabled && handleJoin()}
            />
            <button
              style={{ ...styles.button, flex: 'none', width: 100,
                ...(joinDisabled ? styles.disabledBtn : styles.joinBtn) }}
              onClick={joinDisabled ? undefined : handleJoin}
              disabled={joinDisabled}
            >
              Join
            </button>
            <button style={{ ...styles.button, flex: 'none', width: 100, background: '#666', color: '#fff' }}
              onClick={handleWatch}>
              Watch
            </button>
          </div>
          {gameInProgress && (
            <p style={{ color: '#ff9800', marginTop: 8, fontSize: 14 }}>
              Game in progress — use Watch to spectate
            </p>
          )}
        </div>
      ) : (
        <div style={styles.section}>
          <p style={{ marginBottom: 8, color: '#aed581' }}>Room Code:</p>
          <p style={styles.roomCode}>{roomCode}</p>
          <p style={{ color: '#aed581', marginBottom: 16 }}>
            Share this code with other players
          </p>

          {lobby && (() => {
            const playerCount = lobby.players?.length || 0
            const showPartnerSelection = playerCount === 4 || playerCount === 6
            const partnerRequired = playerCount === 6
            const TEAM_COLORS = ['#4caf50', '#42a5f5', '#ff9800']
            const myPlayer = lobby.players?.find(p => p.id === playerId)
            const myTeamIndex = myPlayer?.teamIndex ?? -1
            const allPartnered = showPartnerSelection && lobby.players?.every(p => p.teamIndex !== -1)
            const sixPlayerBlocked = partnerRequired && !allPartnered
            const isHost = playerId === lobby.players?.[0]?.id
            const enoughPlayers = playerCount >= 2

            const canClickPlayer = (p) => {
              if (!showPartnerSelection) return false
              if (p.id === playerId) return false
              if (p.autoPartnered) return false
              if (myTeamIndex !== -1 && p.teamIndex === myTeamIndex) return true
              if (myTeamIndex === -1 && p.teamIndex === -1) return true
              return false
            }

            const handlePartnerClick = async (targetId) => {
              try {
                const target = lobby.players?.find(p => p.id === targetId)
                const isAlreadyPartner = myTeamIndex !== -1 && target?.teamIndex === myTeamIndex
                let teamName = null
                if (!isAlreadyPartner) {
                  teamName = window.prompt('Enter a name for your team:')
                  if (teamName === null) return
                }
                await choosePartner(targetId, teamName)
              } catch (e) { setLocalError(e.message) }
            }

            return (
              <div style={styles.lobbyInfo}>
                {showPartnerSelection && (
                  <p style={{ color: partnerRequired ? '#ff9800' : '#aed581', marginBottom: 12, fontSize: 14 }}>
                    {partnerRequired
                      ? 'You must choose a partner before the game can start. Click a player to partner with them.'
                      : 'You may choose a partner by clicking another player. Remaining players will be auto-paired when the game starts.'}
                  </p>
                )}

                <h3 style={{ marginBottom: 8 }}>Players ({playerCount}/{lobby.maxPlayers}):</h3>
                <ul style={styles.playerList}>
                  {lobby.players?.map(p => {
                    const clickable = canClickPlayer(p)
                    const isPartner = myTeamIndex !== -1 && p.teamIndex === myTeamIndex && p.id !== playerId
                    const teamColor = p.teamIndex !== -1 ? TEAM_COLORS[p.teamIndex % TEAM_COLORS.length] : null
                    return (
                      <li key={p.id}
                        onClick={clickable ? () => handlePartnerClick(p.id) : undefined}
                        style={{
                          ...styles.playerItem,
                          background: teamColor ? `${teamColor}22` : 'rgba(255,255,255,0.1)',
                          borderLeft: teamColor ? `4px solid ${teamColor}` : '4px solid transparent',
                          cursor: clickable ? 'pointer' : 'default',
                          opacity: (showPartnerSelection && !clickable && p.id !== playerId) ? 0.6 : 1
                        }}
                      >
                        <span>
                          {p.name}
                          {p.id === playerId ? ' (You)' : ''}
                          {isPartner ? ' (Partner)' : ''}
                          {p.autoPartnered ? ' (Auto)' : ''}
                          {p.teamIndex !== -1 && lobby?.teamNames?.[p.teamIndex] && (
                            <span style={{ color: TEAM_COLORS[p.teamIndex % TEAM_COLORS.length], marginLeft: 8, fontSize: 13 }}>
                              [{lobby.teamNames[p.teamIndex]}]
                            </span>
                          )}
                        </span>
                        {p.id === lobby.players[0]?.id && <span style={{ color: '#ffd700' }}>Host</span>}
                      </li>
                    )
                  })}
                </ul>

                {lobby.spectators?.length > 0 && (
                  <>
                    <h3 style={{ marginBottom: 8, marginTop: 16, color: '#888' }}>
                      Spectators ({lobby.spectators.length}):
                    </h3>
                    <ul style={styles.playerList}>
                      {lobby.spectators.map((name, i) => (
                        <li key={i} style={{ ...styles.playerItem, opacity: 0.7 }}>
                          <span>{name}</span>
                        </li>
                      ))}
                    </ul>
                  </>
                )}

                {playerCount === 5 && (
                  <p style={{ textAlign: 'center', marginTop: 16, color: '#ff9800' }}>
                    {isHost
                      ? 'Cannot start with 5 players. One player must leave, or one more must join.'
                      : 'Waiting — one more player must join, or one player must leave.'}
                  </p>
                )}

                {isHost && enoughPlayers && playerCount !== 5 && !sixPlayerBlocked && (
                  <button style={styles.startBtn} onClick={handleStart}>
                    Start Game
                  </button>
                )}
                {isHost && enoughPlayers && sixPlayerBlocked && (
                  <p style={{ textAlign: 'center', marginTop: 16, color: '#ff9800' }}>
                    All players must have partners before starting (6-player mode).
                  </p>
                )}

                {!isHost && playerCount !== 5 && (
                  <p style={{ textAlign: 'center', marginTop: 16, color: '#aed581' }}>
                    Waiting for host to start the game...
                  </p>
                )}

                {!isHost && (
                  <button
                    onClick={async () => { try { await leaveRoom(); navigate('/') } catch(e) { setLocalError(e.message) } }}
                    style={{
                      ...styles.startBtn,
                      background: 'rgba(255,255,255,0.1)',
                      color: '#ccc',
                      border: '1px solid #888'
                    }}
                  >
                    Leave Room
                  </button>
                )}
              </div>
            )
          })()}
        </div>
      )}
    </div>
  )
}
