import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react'
import wsService from '../services/WebSocketService'
import { API_BASE } from '../services/api'

const GameContext = createContext(null)

const SESSION_KEY = 'mb_session'

function saveSession(playerId, playerName, roomCode, spectating) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify({ playerId, playerName, roomCode, spectating }))
}

function loadSession() {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? JSON.parse(raw) : null
  } catch { return null }
}

export function GameProvider({ children }) {
  const saved = useRef(loadSession()).current

  const [playerId, setPlayerId] = useState(saved?.playerId || null)
  const [playerName, setPlayerName] = useState(saved?.playerName || '')
  const [roomCode, setRoomCode] = useState(saved?.roomCode || null)
  const [spectating, setSpectating] = useState(saved?.spectating || false)
  const [lobby, setLobby] = useState(null)
  const [gameState, setGameState] = useState(null)
  const [hand, setHand] = useState([])
  const [error, setError] = useState(null)
  const [scores, setScores] = useState(null)

  const connectAndSubscribe = useCallback((code, pId, isSpectator = false, spectatorName = null) => {
    wsService.connect(() => {
      // Subscribe to public topics first (before register, so we receive the state push)
      wsService.subscribe(`/topic/game/${code}`, (state) => {
        setGameState(state)
      })

      wsService.subscribe(`/topic/lobby/${code}`, (lobbyData) => {
        setLobby(lobbyData)
      })

      wsService.subscribe(`/topic/game/${code}/score`, (data) => {
        setScores(data)
      })

      wsService.subscribe(`/topic/game/${code}/reset`, () => {
        setGameState(null)
        setHand([])
        setScores(null)
      })

      if (!isSpectator && pId) {
        // Player-specific subscriptions
        wsService.subscribe(`/topic/game/${code}/hand/${pId}`, (data) => {
          setHand(data.hand || [])
        })

        wsService.subscribe(`/topic/game/${code}/error/${pId}`, (data) => {
          setError(data.message)
          setTimeout(() => setError(null), 3000)
        })
      }

      // Register (triggers server to push current state)
      const registerPayload = { playerId: pId || null }
      if (isSpectator && spectatorName) {
        registerPayload.spectatorName = spectatorName
      }
      wsService.send(`/app/game/${code}/register`, registerPayload)
    })
  }, [])

  // Reconnect on mount if session exists — validate room first
  useEffect(() => {
    if (!saved?.roomCode) return

    fetch(`${API_BASE}/api/lobby/spectate/${saved.roomCode}`)
      .then(r => r.json())
      .then(data => {
        if (data.error) {
          // Room no longer exists — clear stale session
          sessionStorage.removeItem(SESSION_KEY)
          setPlayerId(null)
          setPlayerName('')
          setRoomCode(null)
          setSpectating(false)
        } else {
          connectAndSubscribe(saved.roomCode, saved.playerId, saved.spectating || false, saved.playerName || null)
        }
      })
      .catch(() => {
        // Server unreachable — clear session
        sessionStorage.removeItem(SESSION_KEY)
        setPlayerId(null)
        setPlayerName('')
        setRoomCode(null)
        setSpectating(false)
      })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const createRoom = useCallback(async (name) => {
    const res = await fetch(`${API_BASE}/api/lobby/create`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerName: name })
    })
    const data = await res.json()
    if (data.error) throw new Error(data.error)

    setPlayerId(data.playerId)
    setPlayerName(name)
    setRoomCode(data.roomCode)
    setLobby(data.lobby)
    setSpectating(false)
    saveSession(data.playerId, name, data.roomCode, false)
    connectAndSubscribe(data.roomCode, data.playerId)
    return data.roomCode
  }, [connectAndSubscribe])

  const joinRoom = useCallback(async (code, name) => {
    const res = await fetch(`${API_BASE}/api/lobby/join/${code}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerName: name })
    })
    const data = await res.json()
    if (data.error) throw new Error(data.error)

    setPlayerId(data.playerId)
    setPlayerName(name)
    setRoomCode(data.roomCode)
    setLobby(data.lobby)
    setSpectating(false)
    saveSession(data.playerId, name, data.roomCode, false)
    connectAndSubscribe(data.roomCode, data.playerId)
    return data.roomCode
  }, [connectAndSubscribe])

  const spectateRoom = useCallback(async (code, name) => {
    const res = await fetch(`${API_BASE}/api/lobby/spectate/${code}`)
    const data = await res.json()
    if (data.error) throw new Error(data.error)

    const displayName = name?.trim() || 'Spectator'
    setPlayerId(null)
    setPlayerName(displayName)
    setRoomCode(data.roomCode)
    setLobby(data.lobby)
    setSpectating(true)
    saveSession(null, displayName, data.roomCode, true)
    connectAndSubscribe(data.roomCode, null, true, displayName)
    return data.roomCode
  }, [connectAndSubscribe])

  const startGame = useCallback(async () => {
    const res = await fetch(`${API_BASE}/api/lobby/${roomCode}/start`, { method: 'POST' })
    const data = await res.json()
    if (data.error) throw new Error(data.error)
  }, [roomCode])

  const drawCard = useCallback(() => {
    wsService.send(`/app/game/${roomCode}/draw`, { playerId })
  }, [roomCode, playerId])

  const playCard = useCallback((cardIndex, targetPlayerId) => {
    wsService.send(`/app/game/${roomCode}/play`, { playerId, cardIndex, targetPlayerId })
  }, [roomCode, playerId])

  const discardCard = useCallback((cardIndex) => {
    wsService.send(`/app/game/${roomCode}/discard`, { playerId, cardIndex })
  }, [roomCode, playerId])

  const declareCoupFourre = useCallback((cardIndex) => {
    wsService.send(`/app/game/${roomCode}/coup-fourre`, { playerId, cardIndex })
  }, [roomCode, playerId])

  const passCoupFourre = useCallback(() => {
    wsService.send(`/app/game/${roomCode}/coup-fourre-pass`, { playerId })
  }, [roomCode, playerId])

  const declareExtension = useCallback((declare) => {
    wsService.send(`/app/game/${roomCode}/extension`, { playerId, declare })
  }, [roomCode, playerId])

  const startNewHand = useCallback(async () => {
    await fetch(`${API_BASE}/api/lobby/${roomCode}/new-hand`, { method: 'POST' })
  }, [roomCode])

  const resetRoom = useCallback(async () => {
    await fetch(`${API_BASE}/api/lobby/${roomCode}/reset`, { method: 'POST' })
  }, [roomCode])

  const leaveRoom = useCallback(async () => {
    const res = await fetch(`${API_BASE}/api/lobby/${roomCode}/leave`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId })
    })
    const data = await res.json()
    if (data.error) throw new Error(data.error)
    // Clear all state
    wsService.disconnect()
    setPlayerId(null)
    setPlayerName('')
    setRoomCode(null)
    setLobby(null)
    setGameState(null)
    setHand([])
    setScores(null)
    setSpectating(false)
    sessionStorage.removeItem(SESSION_KEY)
  }, [roomCode, playerId])

  const choosePartner = useCallback(async (targetPlayerId, teamName) => {
    const res = await fetch(`${API_BASE}/api/lobby/${roomCode}/partner`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ playerId, targetPlayerId, teamName: teamName || null })
    })
    const data = await res.json()
    if (data.error) throw new Error(data.error)
    setLobby(data)
  }, [roomCode, playerId])

  const value = {
    playerId, playerName, roomCode, lobby, gameState, hand, error, scores, spectating,
    setPlayerName, setError,
    createRoom, joinRoom, spectateRoom, startGame, leaveRoom,
    drawCard, playCard, discardCard,
    declareCoupFourre, passCoupFourre,
    declareExtension, startNewHand, resetRoom, choosePartner
  }

  return <GameContext.Provider value={value}>{children}</GameContext.Provider>
}

export function useGame() {
  const ctx = useContext(GameContext)
  if (!ctx) throw new Error('useGame must be used within GameProvider')
  return ctx
}
