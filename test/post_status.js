// Simple test script to POST a sample status and GET /vehicles
const http = require('http');

const data = JSON.stringify({
  vehicle_id: 'BUS123',
  lat: 12.971599,
  lon: 77.594566,
  speed_kmh: 45.3,
  bearing: 87.5,
  segment_id: 'osm_road_1',
  timestamp: new Date().toISOString()
});

const postOptions = {
  hostname: 'localhost',
  port: 8080,
  path: '/status',
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(data)
  }
};

const req = http.request(postOptions, (res) => {
  let body = '';
  res.on('data', d => body += d);
  res.on('end', () => {
    console.log('/status response:', body);

    // now GET /vehicles
    http.get('http://localhost:8080/vehicles', (r) => {
      let out = '';
      r.on('data', d => out += d);
      r.on('end', () => {
        console.log('/vehicles response:', out);
      });
    }).on('error', (e) => console.error('GET error', e));

  });
});

req.on('error', (e) => console.error('POST error', e));
req.write(data);
req.end();
