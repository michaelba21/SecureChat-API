const express = require('express');
const jwt = require('jsonwebtoken');
const rateLimit = require('express-rate-limit');
const { createProxyMiddleware } = require('http-proxy-middleware');

const app = express();
const PORT = 3000;
const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key';

// Rate limiting configurations
const generalLimiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 100, // 100 requests per minute
    message: 'Too many requests from this IP, please try again later.'
});

const authLimiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 5, // 5 requests per minute for auth endpoints
    message: 'Too many login attempts, please try again later.'
});

// Apply rate limiting
app.use('/api/auth', authLimiter);
app.use('/api', generalLimiter);

// JWT verification middleware
const verifyToken = (req, res, next) => {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];
    
    if (!token) {
        return res.status(401).json({ error: 'Access token required' });
    }
    
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        
        // Add user info to headers for Java backend
        req.headers['x-user-id'] = decoded.userId;
        req.headers['x-user-roles'] = decoded.roles.join(',');
        
        next();
    } catch (error) {
        return res.status(403).json({ error: 'Invalid or expired token' });
    }
};

// Apply JWT verification to protected routes
app.use('/api/chatrooms/*/messages', verifyToken);
app.use('/api/files/upload', verifyToken);
app.use('/api/messages/poll', verifyToken);

// Proxy configuration for Java backend
const javaProxy = createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true,
    pathRewrite: {
        '^/api': '/api'
    },
    onProxyReq: (proxyReq, req, res) => {
        // Forward custom headers to Java backend
        if (req.headers['x-user-id']) {
            proxyReq.setHeader('X-User-Id', req.headers['x-user-id']);
        }
        if (req.headers['x-user-roles']) {
            proxyReq.setHeader('X-User-Roles', req.headers['x-user-roles']);
        }
    }
});

// Route all API requests to Java backend
app.use('/api', javaProxy);

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Polling endpoint simulation
app.get('/api/messages/poll', verifyToken, (req, res) => {
    // This endpoint demonstrates the polling flow
    const { since, chatRoomId } = req.query;
    
    // In real implementation, this would forward to Java backend
    // For demonstration, we simulate the polling behavior
    res.json({
        messages: [],
        timestamp: new Date().toISOString(),
        nextPollIn: 5000 // 5 seconds
    });
});

app.listen(PORT, () => {
    console.log(`Gateway running on port ${PORT}`);
    console.log(`Proxying to Java backend on port 8080`);
});
