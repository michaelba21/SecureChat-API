const express = require('express');
const axios = require('axios');
const router = express.Router();
const backend = require('../config/backend');

// Helper to set cookies from backend response (expects tokens in JSON)
function setTokensCookies(res, tokens) {
  if (!tokens) return;
  const cookieOptions = {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'lax',
    // maxAge can be set per token type
  };
  if (tokens.accessToken) {
    res.cookie('access_token', tokens.accessToken, { ...cookieOptions, maxAge: 15 * 60 * 1000 });
  }
  if (tokens.refreshToken) {
    res.cookie('refresh_token', tokens.refreshToken, { ...cookieOptions, maxAge: 7 * 24 * 60 * 60 * 1000 });
  }
}

// POST /auth/login  -> proxy to backend login, store tokens in cookies
router.post('/login', async (req, res) => {
  try {
    const resp = await axios.post(`${backend.backendUrl}/auth/login`, req.body, { withCredentials: true });
    // backend expected to return { accessToken, refreshToken, user }
    setTokensCookies(res, resp.data);
    res.json({ user: resp.data.user });
  } catch (err) {
    const status = err.response ? err.response.status : 500;
    res.status(status).json({ error: err.response ? err.response.data : 'gateway_error' });
  }
});

// POST /auth/refresh -> token rotation: send current refresh token to backend, replace cookies with rotated tokens
router.post('/refresh', async (req, res) => {
  try {
    const currentRefresh = req.cookies['refresh_token'] || req.body.refreshToken;
    if (!currentRefresh) return res.status(400).json({ error: 'no_refresh_token' });

    const resp = await axios.post(`${backend.backendUrl}/auth/refresh`, { refreshToken: currentRefresh }, { withCredentials: true });
    // backend should return new { accessToken, refreshToken }
    setTokensCookies(res, resp.data);
    res.json({ ok: true });
  } catch (err) {
    // In case of invalid token, clear cookies
    res.clearCookie('access_token');
    res.clearCookie('refresh_token');
    const status = err.response ? err.response.status : 500;
    res.status(status).json({ error: err.response ? err.response.data : 'gateway_error' });
  }
});

// POST /auth/logout -> proxy to backend to invalidate refresh token and clear cookies
router.post('/logout', async (req, res) => {
  try {
    const refresh = req.cookies['refresh_token'] || req.body.refreshToken;
    await axios.post(`${backend.backendUrl}/auth/logout`, { refreshToken: refresh }, { withCredentials: true });
  } catch (err) {
    // ignore backend logout errors, still clear cookies locally
  } finally {
    res.clearCookie('access_token');
    res.clearCookie('refresh_token');
    res.json({ ok: true });
  }
});

module.exports = router;
