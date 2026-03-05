// Base URL for API requests. In production, VITE_API_URL points to the server.
// In development, the Vite proxy handles /api and /ws routing.
export const API_BASE = import.meta.env.VITE_API_URL || ''
