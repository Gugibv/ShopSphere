// src/setupProxy.js
const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'https://localhost:8080', // 你的 Spring Boot 服务地址
      changeOrigin: true,
    })
  );
};
