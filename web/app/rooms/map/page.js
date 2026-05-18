'use client';
import { useState, useEffect, useRef } from 'react';
import { db } from '../../../lib/firebase';
import { collectionGroup, getDocs, query, doc, getDoc } from 'firebase/firestore';

const MOCK_HOSTELS = [
    { uid: 'demo', hostelName: 'Trọ Cao Cấp Quận 7', landlordName: 'Ngô Lê Gia Cát', landlordPhone: '0901234567', hostelAddress: 'Nguyễn Thị Thập, P. Tân Phong, Quận 7', lat: 10.7290, lng: 106.7140, rooms: [{ roomName: 'Phòng 101', price: 3500000, status: 'TRONG' }, { roomName: 'Phòng 102', price: 3200000, status: 'DANG_THUE' }] },
    { uid: 'demo2', hostelName: 'Tâm Đôn Apart', landlordName: 'Trần Văn Tâm', landlordPhone: '0912345678', hostelAddress: 'Hẻm 12 Thích Quảng Đức, Quận Phú Nhuận', lat: 10.7990, lng: 106.6820, rooms: [{ roomName: 'Studio Mini', price: 4200000, status: 'TRONG' }] },
    { uid: 'demo3', hostelName: 'Trọ Làng Đại Học', landlordName: 'Cô Nụ', landlordPhone: '0978901234', hostelAddress: 'Khu phố 6, Linh Trung, Thủ Đức', lat: 10.8700, lng: 106.7750, rooms: [{ roomName: 'Phòng Sinh Viên A', price: 2000000, status: 'TRONG' }, { roomName: 'Phòng Sinh Viên B', price: 2100000, status: 'TRONG' }] },
    { uid: 'demo4', hostelName: 'Trọ Bình Thạnh Xanh', landlordName: 'Anh Minh', landlordPhone: '0934567890', hostelAddress: '25 Đinh Bộ Lĩnh, Phường 26, Bình Thạnh', lat: 10.8120, lng: 106.7170, rooms: [{ roomName: 'Phòng Đơn', price: 2800000, status: 'TRONG' }] },
];

export default function MapPage() {
    const mapRef = useRef(null);
    const leafletMapRef = useRef(null);
    const [hostels, setHostels] = useState([]);
    const [selected, setSelected] = useState(null);
    const [loading, setLoading] = useState(true);
    const selectedRef = useRef(null);

    const nf = new Intl.NumberFormat('vi-VN');

    useEffect(() => {
        async function fetchHostels() {
            try {
                const q = query(collectionGroup(db, 'rooms'));
                const snapshot = await getDocs(q);
                const rawRooms = snapshot.docs
                    .map(d => { const uid = d.ref.path.split('/')[1]; return { id: d.id, uid, ...d.data() }; });

                const uniqueUids = [...new Set(rawRooms.map(r => r.uid))];
                const hostelMap = {};
                await Promise.all(uniqueUids.map(async uid => {
                    const sDoc = await getDoc(doc(db, `users/${uid}/settings/config`));
                    if (sDoc.exists()) {
                        const s = sDoc.data();
                        hostelMap[uid] = { uid, ...s, rooms: rawRooms.filter(r => r.uid === uid) };
                    }
                }));

                const list = Object.values(hostelMap).filter(h => h.hostelAddress);
                setHostels(list.length > 0 ? list : MOCK_HOSTELS);
            } catch {
                setHostels(MOCK_HOSTELS);
            } finally {
                setLoading(false);
            }
        }
        fetchHostels();
    }, []);

    async function geocode(address) {
        try {
            const url = `https://nominatim.openstreetmap.org/search?q=${encodeURIComponent(address + ', Viet Nam')}&format=json&limit=1`;
            const res = await fetch(url, { headers: { 'Accept-Language': 'vi' } });
            const data = await res.json();
            if (data?.[0]) return { lat: parseFloat(data[0].lat), lng: parseFloat(data[0].lon) };
        } catch { }
        return null;
    }

    useEffect(() => {
        if (loading || !mapRef.current || typeof window === 'undefined') return;

        let destroyed = false;
        async function initMap() {
            const L = (await import('leaflet')).default;
            if (destroyed) return;

            // Inject leaflet CSS once
            if (!document.querySelector('#leaflet-css')) {
                const link = document.createElement('link');
                link.id = 'leaflet-css';
                link.rel = 'stylesheet';
                link.href = 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css';
                document.head.appendChild(link);
            }

            delete L.Icon.Default.prototype._getIconUrl;
            L.Icon.Default.mergeOptions({
                iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
                iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
                shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
            });

            if (leafletMapRef.current) { leafletMapRef.current.remove(); leafletMapRef.current = null; }
            const map = L.map(mapRef.current).setView([10.78, 106.695], 12);
            leafletMapRef.current = map;

            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© <a href="https://www.openstreetmap.org">OpenStreetMap</a>',
                maxZoom: 19,
            }).addTo(map);

            const makeIcon = (count) => L.divIcon({
                className: '',
                html: `<div style="background:linear-gradient(135deg,#4f46e5,#7c3aed);color:#fff;width:44px;height:44px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-weight:900;font-size:15px;border:3px solid white;box-shadow:0 4px 16px rgba(79,70,229,0.5);cursor:pointer;">${count}</div>`,
                iconSize: [44, 44], iconAnchor: [22, 22],
            });

            const bounds = [];
            for (const hostel of hostels) {
                if (destroyed) return;
                let lat = hostel.lat, lng = hostel.lng;
                if (!lat || !lng) {
                    const coords = await geocode(hostel.hostelAddress);
                    if (coords) { lat = coords.lat; lng = coords.lng; }
                    else continue;
                }
                bounds.push([lat, lng]);
                const count = (hostel.rooms || []).filter(r => r.status === 'TRONG').length;
                const marker = L.marker([lat, lng], { icon: makeIcon(count > 0 ? count : '?') }).addTo(map);
                marker.bindTooltip(`<b>${hostel.hostelName}</b><br/>${count} phòng trống`, {
                    className: 'leaflet-rental-tooltip', direction: 'top', offset: [0, -24],
                });
                marker.on('click', () => {
                    setSelected({ ...hostel, lat, lng });
                    map.panTo([lat, lng]);
                });
            }
            // Fit to actual markers, capped at zoom 14 so it stays local
            if (bounds.length > 0) {
                map.fitBounds(bounds, { maxZoom: 14, padding: [40, 40] });
            }
        }
        initMap();
        return () => { destroyed = true; if (leafletMapRef.current) { leafletMapRef.current.remove(); leafletMapRef.current = null; } };
    }, [loading, hostels]);

    const vacantCount = hostels.reduce((s, h) => s + (h.rooms || []).filter(r => r.status === 'TRONG').length, 0);

    return (
        <div className="min-h-screen bg-slate-50 font-sans flex flex-col">
            {/* Header */}
            <div className="bg-white border-b border-slate-200 px-6 py-5 shadow-sm">
                <div className="max-w-7xl mx-auto flex flex-col sm:flex-row sm:items-center gap-2">
                    <div className="flex-1">
                        <h1 className="text-2xl md:text-3xl font-extrabold text-slate-900 tracking-tight">🗺️ Bản Đồ Phòng Trọ</h1>
                        <p className="text-slate-500 text-sm mt-0.5">Tìm phòng trống gần bạn qua bản đồ trực quan</p>
                    </div>
                    <div className="flex gap-3 text-sm font-semibold">
                        <span className="bg-indigo-50 text-indigo-700 px-3 py-1.5 rounded-full border border-indigo-100">{hostels.length} khu trọ</span>
                        <span className="bg-emerald-50 text-emerald-700 px-3 py-1.5 rounded-full border border-emerald-100">{vacantCount} phòng trống</span>
                    </div>
                </div>
            </div>

            <div className="flex flex-col lg:flex-row flex-1" style={{ minHeight: 0 }}>
                {/* Map area */}
                <div className="flex-1 relative" style={{ minHeight: '60vh' }}>
                    {loading && (
                        <div className="absolute inset-0 flex items-center justify-center bg-slate-100 z-20">
                            <div className="text-center">
                                <div className="w-10 h-10 border-4 border-indigo-500/30 border-t-indigo-600 rounded-full animate-spin mx-auto mb-3" />
                                <p className="text-slate-500 text-sm">Đang tải bản đồ...</p>
                            </div>
                        </div>
                    )}
                    <div ref={mapRef} className="w-full h-full" style={{ minHeight: '60vh' }} />
                    <style>{`
                        .leaflet-rental-tooltip { background:#1e293b!important;color:#fff!important;border:none!important;border-radius:10px!important;font-size:12px!important;padding:7px 12px!important;box-shadow:0 4px 12px rgba(0,0,0,0.2)!important;}
                        .leaflet-rental-tooltip.leaflet-tooltip-top::before{border-top-color:#1e293b!important;}
                    `}</style>
                </div>

                {/* Sidebar */}
                <div className="w-full lg:w-[380px] bg-white border-t lg:border-t-0 lg:border-l border-slate-200 overflow-y-auto" style={{ maxHeight: '100vh' }}>
                    {selected ? (
                        <div className="p-5">
                            <button onClick={() => setSelected(null)} className="text-xs text-slate-400 hover:text-indigo-600 mb-4 flex items-center gap-1 transition-colors">
                                ← Danh sách khu trọ
                            </button>
                            <div className="bg-gradient-to-br from-indigo-50 to-white border border-indigo-100 rounded-2xl p-5 mb-4">
                                <h2 className="text-lg font-bold text-slate-800 mb-2">{selected.hostelName}</h2>
                                {selected.landlordName && <div className="text-sm text-slate-600 flex items-center gap-1.5 mb-1">👤 Quản lý: <span className="font-semibold">{selected.landlordName}</span></div>}
                                {selected.landlordPhone && <a href={`tel:${selected.landlordPhone}`} className="text-sm text-indigo-600 font-bold hover:underline flex items-center gap-1.5 mb-1">📞 {selected.landlordPhone}</a>}
                                {selected.hostelAddress && <div className="text-xs text-slate-400 flex items-start gap-1">📍 {selected.hostelAddress}</div>}
                            </div>
                            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">Phòng trống</h3>
                            <div className="space-y-2">
                                {(selected.rooms || []).filter(r => r.status === 'TRONG').map((room, i) => (
                                    <div key={i} className="bg-white border border-slate-200 rounded-xl p-4 flex justify-between items-center hover:border-indigo-300 transition-colors">
                                        <div>
                                            <div className="font-semibold text-slate-800 text-sm">{room.roomName}</div>
                                            <div className="text-xs text-emerald-600 font-bold mt-0.5">🟢 Đang trống</div>
                                        </div>
                                        <div className="text-right">
                                            <div className="text-indigo-700 font-black font-mono text-sm">{nf.format(room.price)}đ</div>
                                            <div className="text-slate-400 text-xs">/tháng</div>
                                        </div>
                                    </div>
                                ))}
                                {(selected.rooms || []).filter(r => r.status === 'TRONG').length === 0 && (
                                    <div className="text-center py-8 text-slate-400 text-sm">Khu trọ này không còn phòng trống</div>
                                )}
                            </div>
                            <a href="/rooms" className="mt-4 w-full py-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-colors">
                                Xem tất cả phòng trống →
                            </a>
                        </div>
                    ) : (
                        <div className="p-5">
                            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">Khu trọ đang có phòng trống</h3>
                            {loading ? (
                                <div className="space-y-3">
                                    {[1, 2, 3].map(i => <div key={i} className="h-20 bg-slate-100 rounded-xl animate-pulse" />)}
                                </div>
                            ) : (
                                <div className="space-y-2">
                                    {hostels.map((h, i) => {
                                        const count = (h.rooms || []).filter(r => r.status === 'TRONG').length;
                                        return (
                                            <button key={h.uid || i} onClick={() => setSelected(h)}
                                                className="w-full text-left bg-white border border-slate-200 hover:border-indigo-400 hover:shadow-md rounded-xl p-4 flex gap-3 items-center transition-all group"
                                            >
                                                <div className="w-11 h-11 bg-gradient-to-br from-indigo-600 to-violet-600 text-white rounded-full flex items-center justify-center font-black text-base shrink-0">
                                                    {count}
                                                </div>
                                                <div className="min-w-0 flex-1">
                                                    <div className="font-bold text-slate-800 text-sm group-hover:text-indigo-600 transition-colors line-clamp-1">{h.hostelName}</div>
                                                    {h.hostelAddress && <div className="text-xs text-slate-400 line-clamp-1 mt-0.5">📍 {h.hostelAddress}</div>}
                                                    {h.landlordPhone && <div className="text-xs text-indigo-600 mt-0.5">📞 {h.landlordPhone}</div>}
                                                </div>
                                                <svg className="w-4 h-4 text-slate-300 group-hover:text-indigo-500 transition-colors shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
