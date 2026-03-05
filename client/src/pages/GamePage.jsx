import React, { useEffect, useState, useRef, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useGame } from '../context/GameContext'
import GameBoard from '../components/GameBoard'
import PlayerHand from '../components/PlayerHand'
import DrawPile from '../components/DrawPile'
import DiscardPile from '../components/DiscardPile'
import TurnIndicator from '../components/TurnIndicator'
import CoupFourrePrompt from '../components/CoupFourrePrompt'
import ScoreBoard from '../components/ScoreBoard'
import CardComponent from '../components/CardComponent'

const HAZARD_CARDS = ['ACCIDENT', 'OUT_OF_GAS', 'FLAT_TIRE', 'STOP', 'SPEED_LIMIT']

function SpectatorWatermark() {
  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
      pointerEvents: 'none', zIndex: 500, overflow: 'hidden'
    }}>
      <div style={{
        position: 'absolute', top: '-50%', left: '-50%',
        width: '200%', height: '200%',
        display: 'flex', flexWrap: 'wrap', alignContent: 'flex-start',
        transform: 'rotate(-30deg)', transformOrigin: 'center center',
        gap: '60px 80px', padding: 40
      }}>
        {Array.from({ length: 80 }, (_, i) => (
          <span key={i} style={{
            fontSize: 36, fontWeight: 'bold', color: 'rgba(255,255,255,0.06)',
            whiteSpace: 'nowrap', userSelect: 'none'
          }}>
            SPECTATOR
          </span>
        ))}
      </div>
    </div>
  )
}

function SpectatorList({ spectators }) {
  if (!spectators || spectators.length === 0) return null
  return (
    <span style={{ fontSize: 14, color: '#888', marginLeft: 16 }}>
      Watching: {spectators.join(', ')}
    </span>
  )
}

// Inject keyframes for flash animation once
const FLASH_STYLE_ID = 'card-flash-keyframes'
if (typeof document !== 'undefined' && !document.getElementById(FLASH_STYLE_ID)) {
  const style = document.createElement('style')
  style.id = FLASH_STYLE_ID
  style.textContent = `
    @keyframes cardFlash {
      0%   { opacity: 1; }
      12%  { opacity: 0.2; }
      25%  { opacity: 1; }
      37%  { opacity: 0.2; }
      50%  { opacity: 1; }
      62%  { opacity: 0.2; }
      75%  { opacity: 1; }
      87%  { opacity: 0.2; }
      100% { opacity: 1; }
    }
  `
  document.head.appendChild(style)
}

function CardPlayAnimation({ card, fromRect, toRect, swoop, swoopStart, onComplete }) {
  const startX = swoop && swoopStart ? swoopStart.x - 50 : fromRect.x + fromRect.width / 2 - 50
  const startY = swoop && swoopStart ? swoopStart.y - 70 : fromRect.y + fromRect.height / 2 - 70
  const endX = toRect.x + toRect.width / 2 - 50
  const endY = toRect.y + toRect.height / 2 - 70

  const [pos, setPos] = useState({ x: startX, y: startY })

  useEffect(() => {
    const DURATION = 600
    const p0x = startX, p0y = startY
    const p2x = endX, p2y = endY

    let cpx, cpy
    if (swoop) {
      // Control point: down and to the left of the start
      cpx = p0x - 180
      cpy = p0y + 200
    } else {
      // Gentle arc: control point offset perpendicular to the flight path
      const midX = (p0x + p2x) / 2
      const midY = (p0y + p2y) / 2
      const dx = p2x - p0x
      const dy = p2y - p0y
      // Perpendicular offset (to the left of travel direction)
      const len = Math.sqrt(dx * dx + dy * dy) || 1
      const arcSize = Math.min(80, len * 0.3)
      cpx = midX - (dy / len) * arcSize
      cpy = midY + (dx / len) * arcSize
    }

    const startTime = performance.now()
    let rafId

    const animate = (now) => {
      const elapsed = now - startTime
      const t = Math.min(elapsed / DURATION, 1)
      // Ease in-out
      const ease = t < 0.5 ? 2 * t * t : 1 - Math.pow(-2 * t + 2, 2) / 2
      // Quadratic bezier: B(t) = (1-t)²P0 + 2(1-t)tCP + t²P2
      const u = 1 - ease
      setPos({
        x: u * u * p0x + 2 * u * ease * cpx + ease * ease * p2x,
        y: u * u * p0y + 2 * u * ease * cpy + ease * ease * p2y
      })
      if (t < 1) {
        rafId = requestAnimationFrame(animate)
      } else {
        onComplete()
      }
    }
    rafId = requestAnimationFrame(animate)
    return () => cancelAnimationFrame(rafId)
  }, [toRect, swoop, onComplete])

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, width: '100%', height: '100%',
      pointerEvents: 'none', zIndex: 9999
    }}>
      <div style={{
        position: 'absolute',
        left: pos.x,
        top: pos.y,
      }}>
        <CardComponent card={card} small />
      </div>
    </div>
  )
}

export default function GamePage() {
  const { roomCode: routeCode } = useParams()
  const navigate = useNavigate()
  const {
    gameState, hand, playerId, error, scores, lobby, spectating,
    drawCard, playCard, discardCard, startGame, resetRoom, declareExtension, choosePartner, leaveRoom
  } = useGame()

  const [vw, setVw] = useState(window.innerWidth)
  const [vh, setVh] = useState(window.innerHeight)

  useEffect(() => {
    const onResize = () => { setVw(window.innerWidth); setVh(window.innerHeight) }
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [])

  const DESIGN_WIDTH = 1500
  const scale = Math.min(vw / DESIGN_WIDTH, 1)

  useEffect(() => {
    if (!playerId && !spectating) navigate('/')
  }, [playerId, spectating, navigate])

  // Buffer game state during animations so the card doesn't appear at destination early
  const [displayedGameState, setDisplayedGameState] = useState(gameState)
  const latestGameStateRef = useRef(gameState)
  latestGameStateRef.current = gameState

  const isMyTurn = gameState?.currentPlayerId === playerId
  const turnPhase = gameState?.turnPhase
  const canDrag = isMyTurn && turnPhase === 'PLAY'

  // --- Touch drag-and-drop for mobile ---
  const [gameContainerEl, setGameContainerEl] = useState(null)
  const touchRef = useRef({ dragging: false, dragIndex: null, clone: null, lastHighlight: null, originalCard: null, visualW: 0, visualH: 0 })
  const callbacksRef = useRef({})

  useEffect(() => {
    if (!gameContainerEl) return

    const state = touchRef.current

    const cleanup = () => {
      if (state.clone) { state.clone.remove(); state.clone = null }
      if (state.lastHighlight) { state.lastHighlight.style.outline = ''; state.lastHighlight = null }
      if (state.originalCard) { state.originalCard.style.opacity = ''; state.originalCard = null }
      state.dragging = false
      state.dragIndex = null
    }

    const onTouchStart = (e) => {
      if (!callbacksRef.current.canDrag) return
      const card = e.target.closest('[data-drag-index]')
      if (!card) return

      e.preventDefault()
      const idx = parseInt(card.dataset.dragIndex, 10)
      if (isNaN(idx)) return

      state.dragging = true
      state.dragIndex = idx

      // Clone the card as a floating drag image
      const s = callbacksRef.current.scale
      const clone = card.cloneNode(true)
      clone.style.position = 'fixed'
      clone.style.pointerEvents = 'none'
      clone.style.opacity = '0.85'
      clone.style.zIndex = '10000'
      clone.style.transition = 'none'
      clone.style.transform = `scale(${s})`
      clone.style.transformOrigin = 'top left'

      state.visualW = card.offsetWidth * s
      state.visualH = card.offsetHeight * s

      const touch = e.touches[0]
      clone.style.left = (touch.clientX - state.visualW / 2) + 'px'
      clone.style.top = (touch.clientY - state.visualH / 2) + 'px'

      document.body.appendChild(clone)
      state.clone = clone

      // Dim the original card
      card.style.opacity = '0.3'
      state.originalCard = card
    }

    const onTouchMove = (e) => {
      if (!state.dragging) return
      e.preventDefault()

      const touch = e.touches[0]
      if (state.clone) {
        state.clone.style.left = (touch.clientX - state.visualW / 2) + 'px'
        state.clone.style.top = (touch.clientY - state.visualH / 2) + 'px'
      }

      // Highlight drop target under finger
      const el = document.elementFromPoint(touch.clientX, touch.clientY)
      const dropTarget = el?.closest('[data-drop-target]')

      if (state.lastHighlight && state.lastHighlight !== dropTarget) {
        state.lastHighlight.style.outline = ''
      }
      if (dropTarget) {
        dropTarget.style.outline = '3px solid #ff6f00'
      }
      state.lastHighlight = dropTarget || null
    }

    const onTouchEnd = (e) => {
      if (!state.dragging) return
      e.preventDefault()

      const { handleDropOnPlayer, handleDropOnDiscard, playerId: pid } = callbacksRef.current
      const idx = state.dragIndex

      const touch = e.changedTouches[0]
      const el = document.elementFromPoint(touch.clientX, touch.clientY)
      const dropTarget = el?.closest('[data-drop-target]')

      if (dropTarget && idx != null) {
        const type = dropTarget.dataset.dropTarget
        if (type === 'discard') {
          handleDropOnDiscard(idx)
        } else if (type === 'player') {
          const targetId = dropTarget.dataset.playerId
          handleDropOnPlayer(idx, targetId === pid ? null : targetId)
        }
      }

      cleanup()
    }

    gameContainerEl.addEventListener('touchstart', onTouchStart, { passive: false })
    gameContainerEl.addEventListener('touchmove', onTouchMove, { passive: false })
    gameContainerEl.addEventListener('touchend', onTouchEnd, { passive: false })

    return () => {
      gameContainerEl.removeEventListener('touchstart', onTouchStart)
      gameContainerEl.removeEventListener('touchmove', onTouchMove)
      gameContainerEl.removeEventListener('touchend', onTouchEnd)
      cleanup()
    }
  }, [gameContainerEl])

  const handleDrawClick = () => {
    if (!isMyTurn || turnPhase !== 'DRAW') return
    drawCard()
  }

  const handleDropOnPlayer = (cardIndex, targetPlayerId) => {
    if (!canDrag) return
    if (cardIndex < 0 || cardIndex >= hand.length) return

    const card = hand[cardIndex]

    if (targetPlayerId) {
      // Dropped on an opponent - play as hazard
      playCard(cardIndex, targetPlayerId)
    } else {
      // Dropped on own area - play distance/remedy/safety
      if (HAZARD_CARDS.includes(card)) return // can't play hazard on yourself
      playCard(cardIndex, null)
    }
  }

  const handleDropOnDiscard = (cardIndex) => {
    if (!canDrag) return
    if (cardIndex < 0 || cardIndex >= hand.length) return
    discardCard(cardIndex)
  }

  // Keep callbacks ref in sync every render (must be after handler definitions)
  callbacksRef.current = { handleDropOnPlayer, handleDropOnDiscard, canDrag, playerId, scale }

  // --- Card play animation ---
  const [animState, setAnimState] = useState(null) // { card, fromRect, toRect }
  const lastAnimKeyRef = useRef(null)

  useEffect(() => {
    if (!gameState?.lastPlayedCard || !gameState?.lastActionType) {
      // No animation — update displayed state immediately
      setDisplayedGameState(gameState)
      return
    }

    // Build a unique key for this action to avoid re-triggering on reconnect
    const key = `${gameState.lastPlayedByPlayerId}-${gameState.lastPlayedCard}-${gameState.lastActionType}-${gameState.currentPlayerId}`
    if (key === lastAnimKeyRef.current) {
      setDisplayedGameState(gameState)
      return
    }
    lastAnimKeyRef.current = key

    const sourceId = gameState.lastPlayedByPlayerId
    // Helper: find element by player ID, falling back to team-members attribute
    const findPlayerEl = (id) => {
      return document.querySelector(`[data-player-id="${id}"]`)
        || document.querySelector(`[data-team-members*="${id}"]`)
    }
    // Local player's card flies from their hand; other players' cards fly from their board area
    let sourceEl
    if (sourceId === playerId) {
      sourceEl = document.querySelector('[data-hand-area]') || findPlayerEl(sourceId)
    } else {
      sourceEl = findPlayerEl(sourceId)
    }
    if (!sourceEl) { setDisplayedGameState(gameState); return }

    let destEl
    if (gameState.lastActionType === 'DISCARD') {
      destEl = document.querySelector('[data-drop-target="discard"]')
    } else {
      const targetId = gameState.lastPlayedTargetPlayerId || sourceId
      destEl = findPlayerEl(targetId)
    }
    if (!destEl) { setDisplayedGameState(gameState); return }

    const fromRect = sourceEl.getBoundingClientRect()
    const toRect = destEl.getBoundingClientRect()

    // Self-play viewed by another player: source and dest are the same player area
    const isSelfPlay = sourceEl === destEl
    let swoopStart = null
    if (isSelfPlay) {
      swoopStart = {
        x: fromRect.x + 60,
        y: fromRect.y + 40
      }
    }
    // Don't update displayedGameState yet — wait for animation to finish
    setAnimState({ card: gameState.lastPlayedCard, fromRect, toRect, swoop: isSelfPlay, swoopStart })
  }, [gameState?.lastPlayedCard, gameState?.lastActionType, gameState?.lastPlayedByPlayerId, gameState?.currentPlayerId, gameState])

  const handleAnimComplete = useCallback(() => {
    setAnimState(null)
    setDisplayedGameState(latestGameStateRef.current)
  }, [])

  // Reconnecting state — have session but no data yet
  if (!gameState && !lobby && (playerId || spectating)) {
    return (
      <div style={{ width: '100vw', height: '100vh', overflow: 'hidden' }}>
        <div style={{
          width: DESIGN_WIDTH,
          height: vh / scale,
          transform: `scale(${scale})`,
          transformOrigin: 'top left',
        }}>
          <div style={{ maxWidth: 500, margin: '0 auto', padding: 40, textAlign: 'center' }}>
            <p style={{ color: '#ffd700', fontSize: 24 }}>Reconnecting...</p>
          </div>
        </div>
      </div>
    )
  }

  // Waiting state - show lobby
  if (!gameState || gameState.phase === 'WAITING') {
    const isHost = lobby?.players?.[0]?.id === playerId
    const enoughPlayers = (lobby?.players?.length || 0) >= 2
    const playerCount = lobby?.players?.length || 0
    const showPartnerSelection = playerCount === 4 || playerCount === 6
    const partnerRequired = playerCount === 6

    const TEAM_COLORS = ['#4caf50', '#42a5f5', '#ff9800']
    const myPlayer = lobby?.players?.find(p => p.id === playerId)
    const myTeamIndex = myPlayer?.teamIndex ?? -1

    const allPartnered = showPartnerSelection && lobby?.players?.every(p => p.teamIndex !== -1)
    const sixPlayerBlocked = partnerRequired && !allPartnered

    const canClickPlayer = (p) => {
      if (!showPartnerSelection) return false
      if (p.id === playerId) return false
      if (p.autoPartnered) return false
      // Can click your current partner to unpair
      if (myTeamIndex !== -1 && p.teamIndex === myTeamIndex) return true
      // Can click unpaired players when you're unpaired
      if (myTeamIndex === -1 && p.teamIndex === -1) return true
      return false
    }

    const handlePartnerClick = async (targetId) => {
      try {
        const target = lobby?.players?.find(p => p.id === targetId)
        const isAlreadyPartner = myTeamIndex !== -1 && target?.teamIndex === myTeamIndex
        let teamName = null
        if (!isAlreadyPartner) {
          // Pairing — prompt for team name
          teamName = window.prompt('Enter a name for your team:')
          if (teamName === null) return // cancelled
        }
        await choosePartner(targetId, teamName)
      } catch (e) {
        // error handled by context
      }
    }

    return (
      <div style={{ width: '100vw', height: '100vh', overflow: 'hidden' }}>
        <div style={{
          width: DESIGN_WIDTH,
          height: vh / scale,
          transform: `scale(${scale})`,
          transformOrigin: 'top left',
        }}>
          <div style={{ maxWidth: 500, margin: '0 auto', padding: 40, textAlign: 'center' }}>
            <h1 style={{ fontSize: 36, color: '#ffd700', marginBottom: 8 }}>Mille Bornes</h1>
            <p style={{ color: '#aed581', marginBottom: 24 }}>Room Code:</p>
            <p style={{ fontSize: 48, fontWeight: 'bold', color: '#ffd700', letterSpacing: 8, marginBottom: 24 }}>
              {routeCode}
            </p>
            <p style={{ color: '#aed581', marginBottom: 24 }}>Share this code with other players</p>

            {showPartnerSelection && (
              <p style={{ color: partnerRequired ? '#ff9800' : '#aed581', marginBottom: 16, fontSize: 14 }}>
                {partnerRequired
                  ? 'You must choose a partner before the game can start. Click a player to partner with them.'
                  : 'You may choose a partner by clicking another player. Remaining players will be auto-paired when the game starts.'}
              </p>
            )}

            <div style={{
              background: 'rgba(0,0,0,0.2)', borderRadius: 12, padding: 20, marginBottom: 24, textAlign: 'left'
            }}>
              <h3 style={{ marginBottom: 12 }}>
                Players ({playerCount}/{lobby?.maxPlayers || 6}):
              </h3>
              <ul style={{ listStyle: 'none', padding: 0 }}>
                {lobby?.players?.map(p => {
                  const clickable = canClickPlayer(p)
                  const isPartner = myTeamIndex !== -1 && p.teamIndex === myTeamIndex && p.id !== playerId
                  const teamColor = p.teamIndex !== -1 ? TEAM_COLORS[p.teamIndex % TEAM_COLORS.length] : null
                  return (
                    <li key={p.id}
                      onClick={clickable ? () => handlePartnerClick(p.id) : undefined}
                      style={{
                        padding: '8px 16px',
                        background: teamColor ? `${teamColor}22` : 'rgba(255,255,255,0.1)',
                        borderRadius: 6, marginBottom: 4, display: 'flex', justifyContent: 'space-between',
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

              {lobby?.spectators?.length > 0 && (
                <>
                  <h3 style={{ marginBottom: 8, marginTop: 16, color: '#888' }}>
                    Spectators ({lobby.spectators.length}):
                  </h3>
                  <ul style={{ listStyle: 'none', padding: 0 }}>
                    {lobby.spectators.map((name, i) => (
                      <li key={i} style={{
                        padding: '8px 16px', background: 'rgba(255,255,255,0.05)',
                        borderRadius: 6, marginBottom: 4, opacity: 0.7
                      }}>
                        {name}
                      </li>
                    ))}
                  </ul>
                </>
              )}
            </div>

            {playerCount === 5 && (
              <p style={{ color: '#ff9800', marginBottom: 12 }}>
                {isHost
                  ? 'Cannot start with 5 players. One player must leave, or one more must join.'
                  : 'Waiting — one more player must join, or one player must leave.'}
              </p>
            )}

            {isHost && enoughPlayers && playerCount !== 5 && !sixPlayerBlocked && (
              <button onClick={startGame} style={{
                width: '100%', padding: '14px 24px', fontSize: 18, fontWeight: 'bold',
                borderRadius: 8, border: 'none', cursor: 'pointer', background: '#ff6f00', color: '#fff'
              }}>
                Start Game
              </button>
            )}
            {isHost && enoughPlayers && sixPlayerBlocked && (
              <p style={{ color: '#ff9800' }}>All players must have partners before starting (6-player mode).</p>
            )}
            {isHost && !enoughPlayers && (
              <p style={{ color: '#aed581' }}>Waiting for more players to join...</p>
            )}
            {!isHost && playerCount !== 5 && (
              <p style={{ color: '#aed581' }}>Waiting for host to start the game...</p>
            )}

            {!isHost && (
              <button onClick={async () => { try { await leaveRoom(); navigate('/') } catch(e) {} }} style={{
                marginTop: 12, padding: '10px 24px', fontSize: 16, fontWeight: 'bold',
                borderRadius: 8, border: '1px solid #888', cursor: 'pointer',
                background: 'rgba(255,255,255,0.1)', color: '#ccc', width: '100%'
              }}>
                Leave Room
              </button>
            )}
          </div>
        </div>
      </div>
    )
  }

  return (
    <>
      {/* Card play animation overlay */}
      {animState && (
        <CardPlayAnimation
          card={animState.card}
          fromRect={animState.fromRect}
          toRect={animState.toRect}
          swoop={animState.swoop}
          swoopStart={animState.swoopStart}
          onComplete={handleAnimComplete}
        />
      )}

      {/* Spectator watermark at native resolution */}
      {spectating && <SpectatorWatermark />}

      {/* Modals at native resolution — outside scaled container */}
      {!spectating && <CoupFourrePrompt />}

      {/* Extension prompt */}
      {gameState.phase === 'EXTENSION_PROMPT' && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          background: 'rgba(0,0,0,0.7)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          zIndex: 1000
        }}>
          <div style={{
            background: '#1a472a', border: '3px solid #ffd700',
            borderRadius: 16, padding: 32, maxWidth: 450,
            width: '90%', textAlign: 'center'
          }}>
            {gameState.extensionPromptPlayerId === playerId ? (
              <>
                <h2 style={{ color: '#ffd700', marginBottom: 12, fontSize: 28 }}>
                  700 Miles Reached!
                </h2>
                <p style={{ color: '#aed581', marginBottom: 24, fontSize: 18 }}>
                  Extend the trip to 1,000 miles?
                </p>
                <div style={{ display: 'flex', gap: 16, justifyContent: 'center' }}>
                  <button
                    onClick={() => declareExtension(true)}
                    style={{
                      padding: '12px 28px', fontSize: 20, fontWeight: 'bold',
                      borderRadius: 8, border: 'none', cursor: 'pointer',
                      background: '#ffd700', color: '#1a472a'
                    }}
                  >
                    Extend to 1,000
                  </button>
                  <button
                    onClick={() => declareExtension(false)}
                    style={{
                      padding: '12px 28px', fontSize: 20, fontWeight: 'bold',
                      borderRadius: 8, border: 'none', cursor: 'pointer',
                      background: '#666', color: '#fff'
                    }}
                  >
                    End Hand
                  </button>
                </div>
              </>
            ) : (
              <>
                <h2 style={{ color: '#ffd700', marginBottom: 12, fontSize: 28 }}>
                  700 Miles Reached!
                </h2>
                <p style={{ color: '#aed581', fontSize: 18 }}>
                  {gameState.players?.find(p => p.id === gameState.extensionPromptPlayerId)?.name || 'A player'} is deciding whether to extend...
                </p>
              </>
            )}
          </div>
        </div>
      )}

      {(gameState.phase === 'HAND_OVER' || gameState.phase === 'GAME_OVER') && scores && (
        <ScoreBoard />
      )}

      {error && (
        <div style={{
          position: 'fixed', top: 20, left: '50%', transform: 'translateX(-50%)',
          background: '#f44336', padding: '10px 24px', borderRadius: 8,
          zIndex: 999, fontWeight: 'bold'
        }}>
          {error}
        </div>
      )}

      {/* Outer clip wrapper */}
      <div ref={setGameContainerEl} style={{ width: '100vw', height: '100vh', overflow: 'hidden' }}>
        {/* Inner scaled container */}
        <div style={{
          width: DESIGN_WIDTH,
          height: vh / scale,
          transform: `scale(${scale})`,
          transformOrigin: 'top left',
          display: 'flex',
          flexDirection: 'column',
          padding: 12,
          gap: 8
        }}>
          {/* Room code + spectator info */}
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <span style={{ fontSize: 20, color: '#888' }}>
                Room: <span style={{ color: '#ffd700', fontWeight: 'bold', letterSpacing: 4 }}>{routeCode}</span>
              </span>
              <SpectatorList spectators={lobby?.spectators} />
              {lobby?.players?.[0]?.id === playerId &&
                ['PLAYING', 'EXTENSION_PROMPT', 'HAND_OVER', 'GAME_OVER'].includes(gameState?.phase) && (
                <button
                  onClick={resetRoom}
                  style={{
                    marginLeft: 16, padding: '4px 12px', fontSize: 14,
                    borderRadius: 6, border: '1px solid #888', cursor: 'pointer',
                    background: 'rgba(255,255,255,0.1)', color: '#ccc'
                  }}
                >
                  Reset Room
                </button>
              )}
            </div>
            {spectating && (
              <span style={{
                fontSize: 20, color: '#fff', background: '#666',
                padding: '4px 16px', borderRadius: 6, fontWeight: 'bold'
              }}>
                Spectating
              </span>
            )}
          </div>

          <TurnIndicator gameState={displayedGameState || gameState} playerId={playerId} />

          {/* Main game area */}
          <div style={{ flex: 1, overflow: 'auto' }}>
            <GameBoard
              gameState={displayedGameState || gameState}
              playerId={playerId}
              onDropOnPlayer={spectating ? () => {} : handleDropOnPlayer}
            />
          </div>

          {/* Center: draw pile + discard */}
          <div style={{
            display: 'flex',
            justifyContent: 'center',
            gap: 24,
            alignItems: 'center',
            padding: '8px 0'
          }}>
            <DrawPile deckSize={(displayedGameState || gameState).deckSize} onClick={spectating ? undefined : handleDrawClick} />
            <DiscardPile topCard={(displayedGameState || gameState).discardTop} onDrop={spectating ? undefined : handleDropOnDiscard} />
          </div>

          {/* Your hand (hidden for spectators) */}
          {!spectating && (
            <div data-hand-area style={{
              background: 'rgba(0,0,0,0.3)',
              borderRadius: 12,
              padding: '8px 16px'
            }}>
              <PlayerHand hand={hand} canDrag={canDrag} />
            </div>
          )}
        </div>
      </div>
    </>
  )
}
