for2-backend

Minimal Node.js backend to ingest vehicle status messages and broadcast them to WebSocket clients.

Usage

1. Install dependencies:
   npm install

2. Start server:
   npm start

Endpoints

POST /status
  Accepts JSON body with at least: vehicle_id, lat, lon, timestamp

GET /vehicles
  Returns array of last-known statuses

WebSocket
  Connect to ws://HOST:PORT/ and you'll receive a 'snapshot' then live 'status' messages.

Example POST payload

{
  "vehicle_id": "BUS123",
  "lat": 12.345678,
  "lon": 77.123456,
  "speed_kmh": 48.2,
  "bearing": 87.5,
  "segment_id": "osm_road_345678",
  "timestamp": "2025-11-07T15:22:02Z"
}

Extras

 - Example WS client: `node examples/ws_client.js`
 - JSON schema for status messages: `schema/status.schema.json`

Testing

 - Start server: `npm start`
 - In another shell run: `node examples/ws_client.js` to observe messages
 - Use the test script to POST a sample and fetch vehicles: `node test/post_status.js`
