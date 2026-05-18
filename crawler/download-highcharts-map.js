const https = require('https');
const fs = require('fs');

const url = 'https://code.highcharts.com/mapdata/countries/vn/vn-all.topo.json';

https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        fs.writeFileSync('../web/public/vietnam.topo.json', data);
        console.log('✅ Downloaded TopoJSON from Highcharts: ' + data.length + ' bytes');
    });
}).on('error', e => console.error(e));
