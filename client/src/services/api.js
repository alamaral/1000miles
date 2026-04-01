// Copyright © 2026 Alan Amaral
// All rights reserved.
//
// Unauthorized copying, modification, distribution, or use of this software,
// via any medium, is strictly prohibited without prior written permission.
//
// Description:
// Exports the base URL for API requests, sourced from environment or defaulting to same-origin.

// Base URL for API requests. In production, VITE_API_URL points to the server.
// In development, the Vite proxy handles /api and /ws routing.
export const API_BASE = import.meta.env.VITE_API_URL || ''
