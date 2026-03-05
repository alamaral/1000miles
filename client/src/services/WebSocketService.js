import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client/dist/sockjs'
import { API_BASE } from './api'

class WebSocketService {
  constructor() {
    this.client = null
    this.subscriptions = {}
    this.connected = false
    this.onConnectCallbacks = []
  }

  connect(onConnect) {
    if (this.connected) {
      if (onConnect) onConnect()
      return
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE}/ws`),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        this.connected = true
        console.log('WebSocket connected')
        if (onConnect) onConnect()
        this.onConnectCallbacks.forEach(cb => cb())
        this.onConnectCallbacks = []
      },
      onDisconnect: () => {
        this.connected = false
        console.log('WebSocket disconnected')
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'])
      }
    })

    this.client.activate()
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate()
      this.connected = false
      this.subscriptions = {}
    }
  }

  subscribe(destination, callback) {
    if (!this.client || !this.connected) {
      this.onConnectCallbacks.push(() => this.subscribe(destination, callback))
      return
    }

    if (this.subscriptions[destination]) {
      this.subscriptions[destination].unsubscribe()
    }

    this.subscriptions[destination] = this.client.subscribe(destination, (message) => {
      const body = JSON.parse(message.body)
      callback(body)
    })
  }

  subscribeUser(destination, callback) {
    this.subscribe('/user' + destination, callback)
  }

  send(destination, body = {}) {
    if (!this.client || !this.connected) {
      console.warn('Not connected, cannot send')
      return
    }
    this.client.publish({
      destination,
      body: JSON.stringify(body)
    })
  }

  unsubscribe(destination) {
    if (this.subscriptions[destination]) {
      this.subscriptions[destination].unsubscribe()
      delete this.subscriptions[destination]
    }
  }

  unsubscribeAll() {
    Object.keys(this.subscriptions).forEach(dest => {
      this.subscriptions[dest].unsubscribe()
    })
    this.subscriptions = {}
  }
}

const wsService = new WebSocketService()
export default wsService
