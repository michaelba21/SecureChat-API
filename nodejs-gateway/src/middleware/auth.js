const jwt = require('jsonwebtoken')
// Helper: Extract Bearer token from Authorization header
function extractBearer(header) {
  if (!header) return null
  const parts = header.split(' ')
  if (parts.length !== 2 || parts[0] !== 'Bearer') return null
  return parts[1]
}
// Helper: Verify JWT token safely with try-catch
function verifyToken(token, secret) {
  try {
    return jwt.verify(token, secret)
  } catch (err) {
    return null// Token invalid or expired
  }
}

// Verifies access tokens on all /api routes except /api/auth/**
function authenticateAccessToken(req, res, next) {
  if (req.path.startsWith('/auth/')) return next()

  const accessToken =
    extractBearer(req.headers.authorization) || req.cookies?.access_token

  if (!accessToken) {
    return res.status(401).json({ error: 'Access token required' })
  }

  const decoded = verifyToken(accessToken, process.env.JWT_SECRET)
  if (!decoded) {
    return res.status(403).json({ error: 'Invalid or expired access token' })
  }

  req.user = {
    id: decoded.id || decoded.sub,
    roles: decoded.roles || [],
  }
  return next()
}

// Verifies refresh tokens specifically for /api/auth/refresh
 // Use refresh-specific secret, fallback to main secret
function authenticateRefreshToken(req, res, next) {
  const refreshToken =
    extractBearer(req.headers.authorization) || req.cookies?.refresh_token

  if (!refreshToken) {
    return res.status(401).json({ error: 'Refresh token required' })
  }

  const decoded = verifyToken(
    refreshToken,
    process.env.JWT_REFRESH_SECRET || process.env.JWT_SECRET
  )
  if (!decoded) {
    return res.status(403).json({ error: 'Invalid or expired refresh token' })
  }
  // Attach user info to request
  req.user = {
    id: decoded.id || decoded.sub,
    roles: decoded.roles || [],
  }
  return next()
}

module.exports = { authenticateAccessToken, authenticateRefreshToken }


