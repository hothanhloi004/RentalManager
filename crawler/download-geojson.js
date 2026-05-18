const puppeteer = require('puppeteer');
const fs = require('fs');

async function downloadGeoJSON() {
    console.log('🗺️  Downloading Vietnam GeoJSON via Puppeteer...');
    const browser = await puppeteer.launch({ headless: 'new', args: ['--no-sandbox'] });
    const page = await browser.newPage();

    // Try multiple sources
    const sources = [
        'https://raw.githubusercontent.com/VinhNgT/vietnam-provinces-geojson/refs/heads/main/geojson/vietnam-provinces.json',
        'https://raw.githubusercontent.com/duongvm/vietnam-geojson/master/vietnamprovinces.geojson',
        'https://cdn.jsdelivr.net/gh/VinhNgT/vietnam-provinces-geojson@main/geojson/vietnam-provinces.json',
    ];

    for (const url of sources) {
        try {
            console.log('Trying:', url);

            // Use page.evaluate to fetch via browser's fetch API
            const result = await page.evaluate(async (u) => {
                try {
                    const r = await fetch(u);
                    if (!r.ok) return { ok: false, status: r.status };
                    const text = await r.text();
                    return { ok: true, text };
                } catch (e) {
                    return { ok: false, error: e.message };
                }
            }, url);

            if (result.ok && result.text && result.text.length > 10000) {
                fs.writeFileSync('../web/public/vietnam-provinces.json', result.text);
                console.log(`✅ Success! Size: ${result.text.length} bytes`);
                await browser.close();
                return true;
            } else {
                console.log(`   Failed: ${JSON.stringify(result).slice(0, 80)}`);
            }
        } catch (e) {
            console.log(`   Error: ${e.message}`);
        }
    }

    await browser.close();
    return false;
}

downloadGeoJSON().then(ok => {
    if (!ok) console.log('❌ All sources failed. Check firewall/proxy settings.');
});
