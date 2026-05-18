const puppeteer = require('puppeteer');
const fs = require('fs');

const delay = ms => new Promise(res => setTimeout(res, ms));

// Danh sách đầy đủ 63 tỉnh/thành với slug trên phongtro123.com
const ALL_PROVINCES = [
    { id: 'Hà Nội', url: 'https://phongtro123.com/tinh-thanh/ha-noi' },
    { id: 'Hồ Chí Minh', url: 'https://phongtro123.com/tinh-thanh/ho-chi-minh' },
    { id: 'Đà Nẵng', url: 'https://phongtro123.com/tinh-thanh/da-nang' },
    { id: 'Hải Phòng', url: 'https://phongtro123.com/tinh-thanh/hai-phong' },
    { id: 'Cần Thơ', url: 'https://phongtro123.com/tinh-thanh/can-tho' },
    { id: 'Bình Dương', url: 'https://phongtro123.com/tinh-thanh/binh-duong' },
    { id: 'Đồng Nai', url: 'https://phongtro123.com/tinh-thanh/dong-nai' },
    { id: 'Khánh Hòa', url: 'https://phongtro123.com/tinh-thanh/khanh-hoa' },
    { id: 'Bà Rịa - Vũng Tàu', url: 'https://phongtro123.com/tinh-thanh/ba-ria-vung-tau' },
    { id: 'Bình Thuận', url: 'https://phongtro123.com/tinh-thanh/binh-thuan' },
    { id: 'Lâm Đồng', url: 'https://phongtro123.com/tinh-thanh/lam-dong' },
    { id: 'Long An', url: 'https://phongtro123.com/tinh-thanh/long-an' },
    { id: 'Tiền Giang', url: 'https://phongtro123.com/tinh-thanh/tien-giang' },
    { id: 'Bến Tre', url: 'https://phongtro123.com/tinh-thanh/ben-tre' },
    { id: 'Vĩnh Long', url: 'https://phongtro123.com/tinh-thanh/vinh-long' },
    { id: 'An Giang', url: 'https://phongtro123.com/tinh-thanh/an-giang' },
    { id: 'Kiên Giang', url: 'https://phongtro123.com/tinh-thanh/kien-giang' },
    { id: 'Bạc Liêu', url: 'https://phongtro123.com/tinh-thanh/bac-lieu' },
    { id: 'Cà Mau', url: 'https://phongtro123.com/tinh-thanh/ca-mau' },
    { id: 'Sóc Trăng', url: 'https://phongtro123.com/tinh-thanh/soc-trang' },
    { id: 'Trà Vinh', url: 'https://phongtro123.com/tinh-thanh/tra-vinh' },
    { id: 'Hậu Giang', url: 'https://phongtro123.com/tinh-thanh/hau-giang' },
    { id: 'Đồng Tháp', url: 'https://phongtro123.com/tinh-thanh/dong-thap' },
    { id: 'Tây Ninh', url: 'https://phongtro123.com/tinh-thanh/tay-ninh' },
    { id: 'Bình Phước', url: 'https://phongtro123.com/tinh-thanh/binh-phuoc' },
    { id: 'Đắk Lắk', url: 'https://phongtro123.com/tinh-thanh/dak-lak' },
    { id: 'Đắk Nông', url: 'https://phongtro123.com/tinh-thanh/dak-nong' },
    { id: 'Gia Lai', url: 'https://phongtro123.com/tinh-thanh/gia-lai' },
    { id: 'Kon Tum', url: 'https://phongtro123.com/tinh-thanh/kon-tum' },
    { id: 'Ninh Thuận', url: 'https://phongtro123.com/tinh-thanh/ninh-thuan' },
    { id: 'Phú Yên', url: 'https://phongtro123.com/tinh-thanh/phu-yen' },
    { id: 'Bình Định', url: 'https://phongtro123.com/tinh-thanh/binh-dinh' },
    { id: 'Quảng Ngãi', url: 'https://phongtro123.com/tinh-thanh/quang-ngai' },
    { id: 'Quảng Nam', url: 'https://phongtro123.com/tinh-thanh/quang-nam' },
    { id: 'Thừa Thiên Huế', url: 'https://phongtro123.com/tinh-thanh/thua-thien-hue' },
    { id: 'Quảng Trị', url: 'https://phongtro123.com/tinh-thanh/quang-tri' },
    { id: 'Quảng Bình', url: 'https://phongtro123.com/tinh-thanh/quang-binh' },
    { id: 'Hà Tĩnh', url: 'https://phongtro123.com/tinh-thanh/ha-tinh' },
    { id: 'Nghệ An', url: 'https://phongtro123.com/tinh-thanh/nghe-an' },
    { id: 'Thanh Hóa', url: 'https://phongtro123.com/tinh-thanh/thanh-hoa' },
    { id: 'Ninh Bình', url: 'https://phongtro123.com/tinh-thanh/ninh-binh' },
    { id: 'Nam Định', url: 'https://phongtro123.com/tinh-thanh/nam-dinh' },
    { id: 'Thái Bình', url: 'https://phongtro123.com/tinh-thanh/thai-binh' },
    { id: 'Hà Nam', url: 'https://phongtro123.com/tinh-thanh/ha-nam' },
    { id: 'Hưng Yên', url: 'https://phongtro123.com/tinh-thanh/hung-yen' },
    { id: 'Hải Dương', url: 'https://phongtro123.com/tinh-thanh/hai-duong' },
    { id: 'Bắc Ninh', url: 'https://phongtro123.com/tinh-thanh/bac-ninh' },
    { id: 'Vĩnh Phúc', url: 'https://phongtro123.com/tinh-thanh/vinh-phuc' },
    { id: 'Quảng Ninh', url: 'https://phongtro123.com/tinh-thanh/quang-ninh' },
    { id: 'Bắc Giang', url: 'https://phongtro123.com/tinh-thanh/bac-giang' },
    { id: 'Thái Nguyên', url: 'https://phongtro123.com/tinh-thanh/thai-nguyen' },
    { id: 'Phú Thọ', url: 'https://phongtro123.com/tinh-thanh/phu-tho' },
    { id: 'Hòa Bình', url: 'https://phongtro123.com/tinh-thanh/hoa-binh' },
    { id: 'Sơn La', url: 'https://phongtro123.com/tinh-thanh/son-la' },
    { id: 'Điện Biên', url: 'https://phongtro123.com/tinh-thanh/dien-bien' },
    { id: 'Lai Châu', url: 'https://phongtro123.com/tinh-thanh/lai-chau' },
    { id: 'Yên Bái', url: 'https://phongtro123.com/tinh-thanh/yen-bai' },
    { id: 'Lào Cai', url: 'https://phongtro123.com/tinh-thanh/lao-cai' },
    { id: 'Tuyên Quang', url: 'https://phongtro123.com/tinh-thanh/tuyen-quang' },
    { id: 'Hà Giang', url: 'https://phongtro123.com/tinh-thanh/ha-giang' },
    { id: 'Cao Bằng', url: 'https://phongtro123.com/tinh-thanh/cao-bang' },
    { id: 'Bắc Kạn', url: 'https://phongtro123.com/tinh-thanh/bac-kan' },
    { id: 'Lạng Sơn', url: 'https://phongtro123.com/tinh-thanh/lang-son' },
];

async function scrapeAll() {
    console.log(`🤖 Bắt đầu cào dữ liệu ${ALL_PROVINCES.length} tỉnh thành...\n`);

    const browser = await puppeteer.launch({ headless: 'new', args: ['--no-sandbox'] });
    const page = await browser.newPage();
    await page.setUserAgent('Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36');
    await page.setExtraHTTPHeaders({ 'Accept-Language': 'vi-VN,vi;q=0.9' });

    const results = {};
    let successCount = 0;

    for (let i = 0; i < ALL_PROVINCES.length; i++) {
        const target = ALL_PROVINCES[i];
        process.stdout.write(`[${i + 1}/${ALL_PROVINCES.length}] ${target.id}... `);

        try {
            await page.goto(target.url, { waitUntil: 'domcontentloaded', timeout: 30000 });
            await delay(1000);

            const prices = await page.evaluate(() => {
                const found = [];
                document.querySelectorAll('span, div, p').forEach(el => {
                    if (el.children.length === 0) {
                        const text = el.innerText || '';
                        const match = text.trim().match(/^([\d,.]+)\s*triệu\/tháng$/);
                        if (match) {
                            const val = parseFloat(match[1].replace(',', '.')) * 1000000;
                            if (val >= 300000 && val <= 30000000) found.push(val);
                        }
                    }
                });
                return found;
            });

            if (prices.length > 0) {
                const avg = Math.round(prices.reduce((a, b) => a + b, 0) / prices.length);
                results[target.id] = {
                    avgPrice: avg,
                    count: prices.length,
                    min: Math.min(...prices),
                    max: Math.max(...prices),
                    trend: 'stable',
                    source: 'crawled'
                };
                console.log(`✅ ${prices.length} tin | TB: ${(avg / 1e6).toFixed(2)} triệu`);
                successCount++;
            } else {
                console.log(`⚠️  0 tin (trang ít dữ liệu)`);
            }
        } catch (err) {
            console.log(`❌ Lỗi: ${err.message.slice(0, 60)}`);
        }

        await delay(800); // tránh rate-limit
    }

    await browser.close();

    // Lưu JSON thô
    fs.writeFileSync('crawled_data_full.json', JSON.stringify(results, null, 2), 'utf8');
    console.log(`\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━`);
    console.log(`✅ Hoàn tất! ${successCount}/${ALL_PROVINCES.length} tỉnh có dữ liệu`);
    console.log(`📁 Đã lưu: crawled_data_full.json`);

    // Ghi thẳng vào file WebApp
    const js = `// Dữ liệu giá thuê trọ theo tỉnh thành Việt Nam
// Nguồn: phongtro123.com - Cào tự động bằng Puppeteer (${new Date().toLocaleDateString('vi-VN')})
export const rentalPriceData = ${JSON.stringify(results, null, 2)};

export function getPriceColor(price) {
  if (price >= 5000000) return '#7f1d1d';
  if (price >= 4000000) return '#ef4444';
  if (price >= 3000000) return '#f97316';
  if (price >= 2500000) return '#eab308';
  if (price >= 2000000) return '#84cc16';
  return '#22c55e';
}

export function formatPrice(price) {
  if (price >= 1000000) return (price / 1000000).toFixed(1) + ' triệu/tháng';
  return price.toLocaleString('vi-VN') + ' đ/tháng';
}
`;
    fs.writeFileSync('../web/lib/rentalPriceData.js', js, 'utf8');
    console.log(`🌐 Đã cập nhật thẳng vào WebApp!`);
}

scrapeAll().catch(console.error);
