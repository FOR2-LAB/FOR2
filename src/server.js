const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const http = require('http');
const WebSocket = require('ws');

const app = express();
app.use(cors());
app.use(bodyParser.json());

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// In-memory store of last status per vehicle
const vehicles = new Map();

// helpers: haversine distance (meters) and angle normalization
function toRad(deg) { return deg * Math.PI / 180; }
function haversineMeters(lat1, lon1, lat2, lon2) {
  const R = 6371000;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) + Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon/2) * Math.sin(dLon/2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
  return R * c;
}

function normalizeAngle(a) {
  const d = Math.abs(((a % 360) + 540) % 360 - 180);
  return d; // 0..180
}

function broadcast(type, payload) {
  const msg = JSON.stringify({ type, payload });
  for (const ws of wss.clients) {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(msg);
    }
  }
}

// Basic validation
function validateStatus(obj) {
  const required = ['vehicle_id', 'lat', 'lon', 'timestamp'];
  for (const k of required) {
    if (!(k in obj)) return false;
  }
  return true;
}

// HTTP endpoint to receive status updates
app.post('/status', (req, res) => {
  const body = req.body;
  if (!validateStatus(body)) return res.status(400).json({ error: 'invalid payload' });

  // store
  vehicles.set(body.vehicle_id, body);

  console.log('received status for', body.vehicle_id, 'lat', body.lat, 'lon', body.lon);

  // compute simple neighbor relations (same segment)
  const neighbors = [];
  for (const [vid, v] of vehicles.entries()) {
    if (vid === body.vehicle_id) continue;
    if (v.segment_id && body.segment_id && v.segment_id === body.segment_id) {
      // compute straight-line distance and bearing diff
      const dist = haversineMeters(body.lat, body.lon, v.lat, v.lon);
      const bearingDiff = Math.abs(normalizeAngle(body.bearing - (v.bearing || body.bearing)));
      let relation = 'other';
      if (bearingDiff <= 30) relation = 'same_direction';
      else if (bearingDiff >= 150) relation = 'opposite_direction';
      neighbors.push({ vehicle_id: vid, distance_m: Math.round(dist), bearing_diff_deg: Math.round(bearingDiff), relation, speed_kmh: v.speed_kmh });
    }
  }

  // include neighbors in broadcast
  const payload = Object.assign({}, body, { neighbors });

  // broadcast to WS clients
  broadcast('status', payload);

  res.json({ ok: true, neighbors_count: neighbors.length });
});

// Simple endpoint to list known vehicles
app.get('/vehicles', (req, res) => {
  const arr = Array.from(vehicles.values());
  res.json(arr);
});

wss.on('connection', (ws, req) => {
  console.log('ws connected');
  // send current vehicles
  ws.send(JSON.stringify({ type: 'snapshot', payload: Array.from(vehicles.values()) }));

  ws.on('message', (msg) => {
    // basic echo for now
    console.log('ws message', msg.toString());
  });

  ws.on('close', () => console.log('ws closed'));
});

const PORT = process.env.PORT || 8080;
server.listen(PORT, () => console.log(`Server listening on ${PORT}`));
