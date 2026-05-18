const https = require('https');
const fs = require('fs');

const url = 'https://raw.githubusercontent.com/VinhNgT/vietnam-provinces-geojson/refs/heads/main/geojson/vietnam-provinces.json';
console.log('Downloading Vietnam GeoJSON...');
https.get(url, (res) => {
    let data = '';
    res.on('data', (chunk) => data += chunk);
    res.on('end', () => {
        fs.writeFileSync('vietnam-provinces.json', data);
        console.log('Done! Size:', data.length, 'bytes');
    });
}).on('error', (e) => {
    console.log('Error:', e.message);
});
