require('dotenv').config();
const express = require('express');
const morgan = require('morgan');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const { createProxyMiddleware } = require('http-proxy-middleware');
const rateLimit = require('express-rate-limit');
const {
  authenticateAccessToken,
  authenticateRefreshToken,
} = require('./src/middleware/auth');

const app = express();
const PORT = process.env.PORT || 3000;

// Rate limiter configuration
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per windowMs
  message: 'Too many requests from this IP, please try again later.'
});

// Middleware
app.use(morgan('combined'));
app.use(cors({ 
  origin: true, 
  credentials: true 
}));
app.use(express.json());
app.use(cookieParser());
app.use(limiter); // Apply rate limiting

// Health check endpoint
app.get('/health', (req, res) => {
  res.status(200).json({ 
    status: 'OK', 
    service: 'Node.js Gateway',
    timestamp: new Date().toISOString()
  });
});

// Proxy configuration for Java backend
const javaBackendProxy = createProxyMiddleware({
  target: 'http://localhost:8081', // Your Java Spring Boot backend
  changeOrigin: true,
  pathRewrite: {
    '^/api': '/api', // Rewrite if needed
  },
  onProxyReq: (proxyReq, req, res) => {
    console.log(`Proxying request: ${req.method} ${req.url}`);

    // Only forward internal identity headers to the Java backend
    if (req.user?.id) {
      proxyReq.setHeader('X-User-Id', req.user.id);
    }
    if (req.user?.roles?.length) {
      proxyReq.setHeader('X-User-Roles', req.user.roles.join(','));
    }

    // Do not forward original Authorization header to backend
    proxyReq.removeHeader('authorization');
  },
  onError: (err, req, res) => {
    console.error('Proxy Error:', err);
    res.status(500).json({ 
      error: 'Gateway service unavailable',
      message: 'Cannot connect to backend service'
    });
  }
});

// Refresh token verification
app.use('/api/auth/refresh', authenticateRefreshToken);

// Proxy all /api requests to Java backend with access token verification
app.use('/api', authenticateAccessToken, javaBackendProxy);

// 404 handler for undefined routes
app.use('*', (req, res) => {
  res.status(404).json({ 
    error: 'Route not found',
    path: req.originalUrl 
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Gateway Error:', err);
  res.status(500).json({ 
    error: 'Internal server error',
    message: err.message 
  });
});

app.listen(PORT, () => {
  console.log(` Node.js Gateway running on port ${PORT}`);
  console.log(` Proxying to Java backend: http://localhost:8081`);
  console.log(`  Health check: http://localhost:${PORT}/health`);
});
