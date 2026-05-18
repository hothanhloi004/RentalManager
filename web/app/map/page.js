'use client';
import { useState, useEffect, useRef, useMemo } from 'react';
import crawledData from '../../data/crawled_data_full.json';

const formatMoney = (val) => val ? new Intl.NumberFormat('vi-VN').format(Math.round(val)) + 'đ' : '—';

// Toạ độ trung tâm 63 tỉnh thành Việt Nam
const PROVINCE_COORDS = {
    "Hà Nội": [21.0285, 105.8542],
    "Hồ Chí Minh": [10.8231, 106.6297],
    "Đà Nẵng": [16.0544, 108.2022],
    "Hải Phòng": [20.8449, 106.6881],
    "Cần Thơ": [10.0452, 105.7469],
    "Bình Dương": [11.0264, 106.6531],
    "Đồng Nai": [10.9453, 106.8243],
    "Khánh Hòa": [12.2388, 109.1967],
    "Bà Rịa - Vũng Tàu": [10.3465, 107.0843],
    "Bình Thuận": [10.9278, 108.1009],
    "Lâm Đồng": [11.9465, 108.4422],
    "Long An": [10.5360, 106.4113],
    "Tiền Giang": [10.3600, 106.3635],
    "Bến Tre": [10.2434, 106.3756],
    "Vĩnh Long": [10.2397, 105.9572],
    "An Giang": [10.5216, 105.1259],
    "Kiên Giang": [10.0125, 105.0809],
    "Bạc Liêu": [9.2940, 105.7216],
    "Cà Mau": [9.1527, 105.1961],
    "Sóc Trăng": [9.6025, 105.9739],
    "Trà Vinh": [9.8126, 106.2993],
    "Hậu Giang": [9.7579, 105.6413],
    "Đồng Tháp": [10.4938, 105.6882],
    "Tây Ninh": [11.3352, 106.0987],
    "Bình Phước": [11.7512, 106.7235],
    "Đắk Lắk": [12.7100, 108.2378],
    "Đắk Nông": [12.0036, 107.6876],
    "Gia Lai": [13.9832, 108.0159],
    "Kon Tum": [14.3498, 108.0005],
    "Ninh Thuận": [11.5649, 108.9881],
    "Phú Yên": [13.0882, 109.0929],
    "Bình Định": [13.7765, 109.2236],
    "Quảng Ngãi": [15.1214, 108.8044],
    "Quảng Nam": [15.5394, 108.0191],
    "Thừa Thiên Huế": [16.4674, 107.5905],
    "Quảng Trị": [16.7505, 107.1856],
    "Quảng Bình": [17.4690, 106.6222],
    "Hà Tĩnh": [18.3559, 105.8877],
    "Nghệ An": [18.6793, 105.6813],
    "Thanh Hóa": [19.8067, 105.7852],
    "Ninh Bình": [20.2507, 105.9745],
    "Nam Định": [20.4389, 106.1621],
    "Thái Bình": [20.4463, 106.3365],
    "Hà Nam": [20.5835, 105.9230],
    "Hưng Yên": [20.6464, 106.0511],
    "Hải Dương": [20.9373, 106.3146],
    "Bắc Ninh": [21.1861, 106.0763],
    "Vĩnh Phúc": [21.3089, 105.6047],
    "Quảng Ninh": [21.0064, 107.2925],
    "Bắc Giang": [21.2720, 106.1946],
    "Thái Nguyên": [21.5942, 105.8482],
    "Phú Thọ": [21.4225, 105.2295],
    "Hòa Bình": [20.8133, 105.3384],
    "Sơn La": [21.3270, 103.9144],
    "Điện Biên": [21.3862, 103.0230],
    "Lai Châu": [22.3862, 103.4706],
    "Yên Bái": [21.7168, 104.8985],
    "Lào Cai": [22.4856, 103.9707],
    "Tuyên Quang": [21.7768, 105.2280],
    "Hà Giang": [22.8233, 104.9837],
    "Cao Bằng": [22.6359, 106.2523],
    "Bắc Kạn": [22.1443, 105.8348],
    "Lạng Sơn": [21.8462, 106.7572],
};

function getRadius(count) {
    if (count >= 25) return 10;
    if (count >= 15) return 8;
    return 6;
}

function getMarkerColor(price) {
    if (!price) return '#94a3b8';
    if (price >= 4_500_000) return '#ef4444'; // đỏ
    if (price >= 3_500_000) return '#f97316'; // cam
    if (price >= 2_500_000) return '#eab308'; // vàng
    return '#22c55e'; // xanh
}

export default function DarkLeafletMapPage() {
    const mapRef = useRef(null);
    const leafletMapRef = useRef(null);
    const [mounted, setMounted] = useState(false);
    const [loading, setLoading] = useState(true);
    const [selectedProv, setSelectedProv] = useState(null);
    const [hoveredProv, setHoveredProv] = useState(null);
    const [search, setSearch] = useState('');
    const [sortBy, setSortBy] = useState('price-desc');
    const [filter, setFilter] = useState('all');

    const provinces = useMemo(() => {
        return Object.keys(crawledData).map(name => ({
            name,
            coords: PROVINCE_COORDS[name],
            ...crawledData[name],
        })).filter(p => p.coords);
    }, []);

    const filteredProvinces = useMemo(() => {
        let list = [...provinces];
        // Tìm kiếm không dấu
        if (search) {
            const t = removeAccents(search).toLowerCase();
            list = list.filter(p => removeAccents(p.name).toLowerCase().includes(t) || p.name.toLowerCase().includes(search.toLowerCase()));
        }
        // Lọc theo mức giá
        if (filter === 'high') list = list.filter(p => p.avgPrice >= 3_500_000);
        else if (filter === 'mid') list = list.filter(p => p.avgPrice >= 1_500_000 && p.avgPrice < 3_500_000);
        else if (filter === 'low') list = list.filter(p => p.avgPrice < 1_500_000);
        // Sắp xếp
        switch (sortBy) {
            case 'price-asc': list.sort((a, b) => (a.avgPrice || 0) - (b.avgPrice || 0)); break;
            case 'name': list.sort((a, b) => a.name.localeCompare(b.name, 'vi')); break;
            case 'count': list.sort((a, b) => (b.count || 0) - (a.count || 0)); break;
            default: list.sort((a, b) => (b.avgPrice || 0) - (a.avgPrice || 0));
        }
        return list;
    }, [provinces, search, filter, sortBy]);

    useEffect(() => setMounted(true), []);

    useEffect(() => {
        if (!mounted || !mapRef.current || typeof window === 'undefined') return;

        let destroyed = false;

        async function initMap() {
            const L = (await import('leaflet')).default;
            if (destroyed) return;

            // CSS Leaflet
            if (!document.querySelector('#leaflet-css')) {
                const link = document.createElement('link');
                link.id = 'leaflet-css';
                link.rel = 'stylesheet';
                link.href = 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css';
                document.head.appendChild(link);
            }

            if (leafletMapRef.current) { leafletMapRef.current.remove(); leafletMapRef.current = null; }

            // Giới hạn bản đồ chỉ hiển thị Việt Nam
            const vnBounds = [[7.5, 101.5], [24, 110.5]];
            const map = L.map(mapRef.current, {
                center: [16, 106],
                zoom: 6,
                minZoom: 5,
                maxZoom: 13,
                zoomControl: false,
                attributionControl: false,
                maxBounds: vnBounds,
                maxBoundsViscosity: 1.0,
            });
            leafletMapRef.current = map;

            // Light tile layer — CartoDB Positron (không nhãn)
            L.tileLayer('https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png', {
                subdomains: 'abcd',
                maxZoom: 19,
            }).addTo(map);

            // Zoom control góc phải
            L.control.zoom({ position: 'bottomright' }).addTo(map);

            // Tạo mask che toàn bộ thế giới, chỉ để lộ Việt Nam
            try {
                const topoRes = await fetch('/vietnam.topo.json');
                const topoData = await topoRes.json();
                // Lấy tất cả tọa độ tỉnh từ TopoJSON
                const { feature } = await import('topojson-client');
                const geoFeatures = feature(topoData, topoData.objects[Object.keys(topoData.objects)[0]]);

                // Hình chữ nhật bao toàn thế giới (ngược chiều kim đồng hồ)
                const worldOuter = [[-90, -180], [-90, 180], [90, 180], [90, -180], [-90, -180]];

                // Gộp tất cả tỉnh thành 1 polygon lớn (chiều kim đồng hồ = lỗ cắt)
                const vnHoles = geoFeatures.features.flatMap(f => {
                    const geom = f.geometry;
                    if (geom.type === 'Polygon') return geom.coordinates;
                    if (geom.type === 'MultiPolygon') return geom.coordinates.flat();
                    return [];
                });

                // Chuyển [lng,lat] → [lat,lng] cho Leaflet
                const outerRing = worldOuter.map(([lat, lng]) => [lat, lng]);
                const holes = vnHoles.map(ring => ring.map(([lng, lat]) => [lat, lng]));

                L.polygon([outerRing, ...holes], {
                    fillColor: '#f8fafc',
                    fillOpacity: 1,
                    stroke: false,
                    interactive: false,
                }).addTo(map);
            } catch (e) {
                console.warn('Mask load failed:', e);
            }

            // Thêm marker cho từng tỉnh
            provinces.forEach(prov => {
                const color = getMarkerColor(prov.avgPrice);
                const r = getRadius(prov.count);

                const circle = L.circleMarker(prov.coords, {
                    radius: r,
                    fillColor: color,
                    fillOpacity: 0.9,
                    color: '#ffffff',
                    weight: 2,
                    className: 'province-marker',
                }).addTo(map);

                // Tooltip luôn hiện tên tỉnh
                circle.bindTooltip(prov.name, {
                    permanent: true,
                    direction: 'top',
                    offset: [0, -r - 4],
                    className: 'province-label',
                });

                // Popup khi click
                circle.bindPopup(`
          <div style="font-family: system-ui; min-width: 180px;">
            <div style="font-weight: 800; font-size: 14px; margin-bottom: 8px; color: #f1f5f9;">${prov.name}</div>
            <div style="display:flex; justify-content:space-between; margin-bottom: 4px;">
              <span style="color: #94a3b8; font-size: 12px;">Giá trung bình</span>
              <span style="color: ${color}; font-weight: 700; font-size: 13px;">${formatMoney(prov.avgPrice)}</span>
            </div>
            <div style="display:flex; justify-content:space-between; margin-bottom: 4px;">
              <span style="color: #94a3b8; font-size: 12px;">Dao động</span>
              <span style="color: #e2e8f0; font-weight: 600; font-size: 12px;">${(prov.min / 1e6).toFixed(1)} – ${(prov.max / 1e6).toFixed(1)} triệu</span>
            </div>
            <div style="display:flex; justify-content:space-between;">
              <span style="color: #94a3b8; font-size: 12px;">Số tin đăng</span>
              <span style="color: #e2e8f0; font-weight: 600; font-size: 12px;">${prov.count} tin</span>
            </div>
          </div>
        `, {
                    className: 'dark-popup',
                    closeButton: false,
                });

                circle.on('click', () => {
                    setSelectedProv(prov.name);
                });
                circle.on('mouseover', () => {
                    setHoveredProv(prov);
                    circle.setStyle({ fillOpacity: 1, weight: 3 });
                });
                circle.on('mouseout', () => {
                    setHoveredProv(null);
                    circle.setStyle({ fillOpacity: 0.9, weight: 2 });
                });
            });

            setLoading(false);
        }

        initMap();
        return () => {
            destroyed = true;
            if (leafletMapRef.current) { leafletMapRef.current.remove(); leafletMapRef.current = null; }
        };
    }, [mounted, provinces]);

    // Pan đến tỉnh khi click từ sidebar
    const panToProvince = (prov) => {
        setSelectedProv(prov.name);
        if (leafletMapRef.current && prov.coords) {
            leafletMapRef.current.setView(prov.coords, 10, { animate: true });
        }
    };

    // Helper: strip Vietnamese diacritics so jsPDF Helvetica renders correctly
    const vi2ascii = (str) => (str || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/\u0111/g, 'd').replace(/\u0110/g, 'D')
        .replace(/\u01b0/g, 'u').replace(/\u01af/g, 'U')
        .replace(/\u01a1/g, 'o').replace(/\u01a0/g, 'O')
        .replace(/\u1ea1/g, 'a').replace(/\u1eb9/g, 'e')
        .replace(/[^\x00-\x7F]/g, '?');

    const exportProvincesToPDF = async () => {
        const { default: jsPDF } = await import('jspdf');
        const { default: autoTable } = await import('jspdf-autotable');

        const sorted = [...provinces].sort((a, b) => (b.avgPrice || 0) - (a.avgPrice || 0));
        const nf = new Intl.NumberFormat('vi-VN');
        const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

        // Header
        doc.setFillColor(15, 23, 42);
        doc.rect(0, 0, 210, 24, 'F');
        doc.setTextColor(255, 255, 255);
        doc.setFontSize(13);
        doc.setFont('helvetica', 'bold');
        doc.text('BAO CAO GIA THUE PHONG TRO - VIET NAM', 14, 11);
        doc.setFontSize(8);
        doc.setFont('helvetica', 'normal');
        doc.text(`Du lieu tu ${sorted.length} tinh thanh  |  Xuat ngay: ${new Date().toLocaleDateString('vi-VN')}`, 14, 19);

        const avgAll = sorted.reduce((s, p) => s + (p.avgPrice || 0), 0) / (sorted.filter(p => p.avgPrice).length || 1);
        doc.setTextColor(30, 30, 30);
        doc.setFontSize(9);
        doc.setFont('helvetica', 'bold');
        doc.text(`Gia TB ca nuoc: ${nf.format(Math.round(avgAll))}d  |  Tong tin dang: ${sorted.reduce((s, p) => s + (p.count || 0), 0)}`, 14, 31);

        const rows = sorted.map((p, i) => {
            const tier = p.avgPrice >= 4_500_000 ? 'Cao' : p.avgPrice >= 3_500_000 ? 'Kha cao' : p.avgPrice >= 2_500_000 ? 'Trung binh' : 'Thap';
            const vsAvg = avgAll ? (((p.avgPrice || 0) - avgAll) / avgAll * 100).toFixed(0) + '%' : '—';
            return [
                i + 1,
                vi2ascii(p.name),
                p.avgPrice ? nf.format(p.avgPrice) + 'd' : '—',
                p.min ? (p.min / 1e6).toFixed(1) + 'M' : '—',
                p.max ? (p.max / 1e6).toFixed(1) + 'M' : '—',
                p.count || 0,
                vsAvg,
                tier,
            ];
        });

        const tierColors = { 'Cao': [220, 38, 38], 'Kha cao': [234, 88, 12], 'Trung binh': [202, 138, 4], 'Thap': [22, 163, 74] };

        autoTable(doc, {
            startY: 36,
            head: [['#', 'Tinh thanh', 'Gia TB', 'Thap nhat', 'Cao nhat', 'So tin', 'So sanh', 'Muc gia']],
            body: rows,
            theme: 'grid',
            headStyles: { fillColor: [15, 23, 42], textColor: 255, fontStyle: 'bold', fontSize: 8 },
            bodyStyles: { fontSize: 8, textColor: [30, 30, 30] },
            alternateRowStyles: { fillColor: [241, 245, 249] },
            columnStyles: {
                0: { cellWidth: 8, halign: 'center' },
                1: { cellWidth: 42 },
                2: { cellWidth: 30, halign: 'right', fontStyle: 'bold' },
                3: { cellWidth: 22, halign: 'right' },
                4: { cellWidth: 22, halign: 'right' },
                5: { cellWidth: 16, halign: 'center' },
                6: { cellWidth: 22, halign: 'center' },
                7: { cellWidth: 22, halign: 'center', fontStyle: 'bold' },
            },
            didParseCell: (data) => {
                if (data.column.index === 7 && data.section === 'body') {
                    data.cell.styles.textColor = tierColors[String(data.cell.raw)] || [30, 30, 30];
                }
                if (data.column.index === 6 && data.section === 'body') {
                    const raw = String(data.cell.raw || '');
                    if (raw.startsWith('-')) data.cell.styles.textColor = [22, 163, 74];
                    else if (raw !== '—') data.cell.styles.textColor = [220, 38, 38];
                }
            },
        });

        // Footer pages
        const pageCount = doc.internal.getNumberOfPages();
        for (let i = 1; i <= pageCount; i++) {
            doc.setPage(i);
            doc.setFontSize(7);
            doc.setTextColor(150);
            doc.text(`Trang ${i}/${pageCount}  |  Rental Manager - Ban do gia thue`, 14, doc.internal.pageSize.height - 5);
        }

        doc.save(`gia-thue-tinh-thanh-${new Date().toISOString().slice(0, 10)}.pdf`);
    };

    if (!mounted) return null;

    return (
        <div className="flex flex-col h-screen bg-white overflow-hidden" style={{ paddingTop: '68px' }}>
            <div className="flex-1 flex overflow-hidden">

                {/* Sidebar */}
                <aside className="w-80 lg:w-96 bg-white border-r border-slate-200 flex flex-col flex-shrink-0 z-40">
                    <div className="p-4 space-y-4 border-b border-slate-100">
                        <div className="flex items-center justify-between mt-1">
                            <h2 className="text-base font-extrabold text-slate-800">Giá thuê theo tỉnh</h2>
                            <button onClick={exportProvincesToPDF}
                                className="flex items-center gap-1.5 px-3 py-1.5 bg-rose-50 border border-rose-200 text-rose-600 rounded-lg text-xs font-bold hover:bg-rose-100 transition-all">
                                <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
                                Xuất PDF
                            </button>
                        </div>
                        <input type="text" value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Tìm tỉnh (gõ không dấu được)..."
                            className="w-full px-3.5 py-2.5 text-sm bg-slate-50 border border-slate-200 rounded-xl text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-slate-400 focus:bg-white transition-colors font-medium" />

                        {/* Lọc theo giá */}
                        <div className="flex gap-1.5 flex-wrap">
                            {[{ id: 'all', label: 'Tất cả' }, { id: 'high', label: '≥ 3.5M' }, { id: 'mid', label: '1.5–3.5M' }, { id: 'low', label: '< 1.5M' }].map(({ id, label }) => (
                                <button key={id} onClick={() => setFilter(id)}
                                    className={`text-xs font-bold px-3 py-1.5 rounded-lg border transition-all ${filter === id ? 'bg-slate-800 text-white border-slate-800' : 'text-slate-500 border-slate-200 hover:border-slate-300 hover:text-slate-800'}`}>
                                    {label}
                                </button>
                            ))}
                        </div>

                        {/* Sắp xếp */}
                        <div className="flex gap-1.5 flex-wrap">
                            {[{ id: 'price-desc', label: 'Giá cao nhất' }, { id: 'price-asc', label: 'Giá thấp nhất' }, { id: 'name', label: 'A–Z' }, { id: 'count', label: 'Sôi động nhất' }].map(({ id, label }) => (
                                <button key={id} onClick={() => setSortBy(id)}
                                    className={`text-xs font-bold px-3 py-1.5 rounded-lg border transition-all ${sortBy === id ? 'bg-slate-100 text-slate-800 border-slate-200 shadow-sm' : 'text-slate-500 border-transparent hover:border-slate-200 hover:bg-slate-50'}`}>
                                    {label}
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="flex-1 overflow-y-auto custom-scrollbar">
                        {filteredProvinces.length === 0 ? (
                            <div className="text-center py-10 text-slate-400 text-sm font-medium">Không tìm thấy tỉnh nào</div>
                        ) : filteredProvinces.map(prov => (
                            <button key={prov.name} onClick={() => panToProvince(prov)}
                                className={`w-full text-left px-5 py-3 border-b border-slate-100 transition-colors ${selectedProv === prov.name ? 'bg-slate-50' : 'hover:bg-slate-50'}`}>
                                <div className="flex items-center gap-2 mb-1">
                                    <div className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ background: getMarkerColor(prov.avgPrice) }} />
                                    <span className="text-sm font-bold text-slate-800">{prov.name}</span>
                                    <span className="text-xs font-medium text-slate-400 ml-auto">{prov.count} tin</span>
                                </div>
                                <div className="text-sm font-black ml-4" style={{ color: getMarkerColor(prov.avgPrice) }}>
                                    {formatMoney(prov.avgPrice)}
                                </div>
                            </button>
                        ))}
                    </div>
                </aside>

                {/* Map */}
                <main className="flex-1 relative">
                    {/* Hover panel góc phải */}
                    {hoveredProv && (
                        <div className="absolute top-4 right-4 z-[1000] w-56 bg-white/95 backdrop-blur-md border border-slate-200 rounded-2xl shadow-xl overflow-hidden pointer-events-none">
                            <div className="px-4 py-3 border-b border-slate-100 flex items-center gap-2">
                                <div className="w-2.5 h-2.5 rounded-full" style={{ background: getMarkerColor(hoveredProv.avgPrice) }} />
                                <span className="font-bold text-slate-800 text-sm">{hoveredProv.name}</span>
                            </div>
                            <div className="px-4 py-3 text-sm space-y-2 text-slate-600">
                                <div className="flex justify-between items-center">
                                    <span className="text-slate-500 font-medium">Giá trung bình</span>
                                    <span className="font-black" style={{ color: getMarkerColor(hoveredProv.avgPrice) }}>{formatMoney(hoveredProv.avgPrice)}</span>
                                </div>
                                <div className="flex justify-between items-center">
                                    <span className="text-slate-500 font-medium">Dao động</span>
                                    <span className="font-semibold text-slate-700">{(hoveredProv.min / 1e6).toFixed(1)} – {(hoveredProv.max / 1e6).toFixed(1)} triệu</span>
                                </div>
                                <div className="flex justify-between items-center border-t border-slate-100 pt-2 mt-2">
                                    <span className="text-slate-500 font-medium">Số tin</span>
                                    <span className="font-semibold text-slate-700">{hoveredProv.count} tin đăng</span>
                                </div>
                            </div>
                        </div>
                    )}

                    {loading && (
                        <div className="absolute inset-0 flex items-center justify-center bg-slate-50/80 backdrop-blur-sm z-20">
                            <div className="text-center bg-white p-6 rounded-2xl shadow-xl flex flex-col items-center">
                                <div className="w-8 h-8 border-4 border-slate-200 border-t-indigo-600 rounded-full animate-spin mb-3" />
                                <h3 className="text-slate-800 font-bold mb-1">Đang tải bản đồ</h3>
                                <p className="text-slate-500 text-xs">Vui lòng chờ giây lát...</p>
                            </div>
                        </div>
                    )}
                    <div ref={mapRef} className="w-full h-full" />

                    {/* Chú thích */}
                    <div className="absolute top-4 left-4 z-[1000] bg-white/95 backdrop-blur border border-slate-200 shadow-sm rounded-xl px-4 py-3 space-y-2">
                        <div className="text-[10px] text-slate-500 font-bold uppercase tracking-wider mb-2">MỨC GIÁ TRUNG BÌNH</div>
                        {[
                            { color: '#22c55e', label: 'Dưới 2.5 triệu' },
                            { color: '#eab308', label: '2.5 – 3.5 triệu' },
                            { color: '#f97316', label: '3.5 – 4.5 triệu' },
                            { color: '#ef4444', label: 'Trên 4.5 triệu' },
                        ].map(item => (
                            <div key={item.label} className="flex items-center gap-2">
                                <div className="w-3 h-3 rounded-full shadow-inner" style={{ background: item.color }} />
                                <span className="text-xs font-medium text-slate-700">{item.label}</span>
                            </div>
                        ))}
                    </div>
                </main>
            </div>

            <style jsx global>{`
        .province-label {
          background: transparent !important;
          border: none !important;
          box-shadow: none !important;
          color: #1e293b !important;
          font-size: 10px !important;
          font-weight: 700 !important;
          text-shadow: 0 0 4px rgba(255,255,255,1), 0 0 8px rgba(255,255,255,0.8), 0 0 12px rgba(255,255,255,0.8) !important;
          padding: 0 !important;
          white-space: nowrap !important;
        }
        .province-label::before {
          display: none !important;
        }
        .dark-popup .leaflet-popup-content-wrapper {
          background: #ffffff !important;
          border-radius: 12px !important;
          box-shadow: 0 10px 25px rgba(0,0,0,0.1) !important;
          border: 1px solid #e2e8f0 !important;
        }
        .dark-popup .leaflet-popup-tip {
          background: #ffffff !important;
          border: 1px solid #e2e8f0 !important;
        }
        .dark-popup .leaflet-popup-content {
          margin: 12px 14px !important;
          color: #1e293b !important;
        }
        .province-marker {
          transition: all 0.15s ease;
        }
        .province-marker:hover {
          fill-opacity: 1 !important;
          stroke-width: 3 !important;
        }
        .custom-scrollbar::-webkit-scrollbar { width: 4px; }
        .custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
        .custom-scrollbar::-webkit-scrollbar-thumb { background: #cbd5e1; border-radius: 4px; }
        .custom-scrollbar:hover::-webkit-scrollbar-thumb { background: #94a3b8; }
        .leaflet-control-zoom a {
          background: #ffffff !important;
          color: #64748b !important;
          border-color: #cbd5e1 !important;
        }
        .leaflet-control-zoom a:hover {
          background: #f8fafc !important;
          color: #0f172a !important;
        }
      `}</style>
        </div>
    );
}
