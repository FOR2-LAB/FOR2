// Simple WS client to connect and log messages
const WebSocket = require('ws');
const ws = new WebSocket('ws://localhost:8080');
ws.on('open', () => console.log('connected to ws'));
ws.on('message', (msg) => {
  try {
    const obj = JSON.parse(msg.toString());
    console.log('ws message', JSON.stringify(obj, null, 2));
  } catch (e) {
    console.log('raw msg', msg.toString());
  }
});
