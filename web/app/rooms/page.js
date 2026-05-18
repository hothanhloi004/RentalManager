'use client';
import { useState, useEffect, useMemo } from 'react';
import { db } from '../../lib/firebase';
import { collectionGroup, getDocs, query, doc, getDoc, collection, addDoc, serverTimestamp } from 'firebase/firestore';
import Link from 'next/link';

// ─── Helpers ─────────────────────────────────────────────────────────────────
function removeAccents(str) {
    return (str || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').replace(/đ/g, 'd').replace(/Đ/g, 'D').toLowerCase();
}

// ─── Contact Modal ────────────────────────────────────────────────────────────
function ContactModal({ room, onClose }) {
    const [form, setForm] = useState({ name: '', phone: '', note: '' });
    const [sending, setSending] = useState(false);
    const [sent, setSent] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!form.name || !form.phone) return;
        setSending(true);
        try {
            await addDoc(collection(db, `inquiries/${room.uid}/requests`), {
                roomId: room.id,
                roomName: room.roomName || room.name,
                name: form.name,
                phone: form.phone,
                note: form.note,
                createdAt: serverTimestamp(),
                status: 'NEW',
            });
            // Gọi API gửi Push Notification cho chủ nhà (vẫn sẽ gửi thành công kể cả chủ nhà chưa cung cấp key, api trả về catch)
            try {
                await fetch('/api/notify', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        uid: room.uid,
                        title: 'Khách hỏi xem phòng mới!',
                        message: `Khách ${form.name} (${form.phone}) muốn xem ${room.roomName || room.name}.`
                    })
                });
            } catch (e) {
                console.error('Lỗi khi gọi API Push Notification:', e);
            }
            setSent(true);
        } catch (err) {
            alert('Gửi thất bại: ' + err.message);
        } finally {
            setSending(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm" onClick={onClose}>
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md p-7 relative" onClick={e => e.stopPropagation()}>
                <button onClick={onClose} className="absolute top-4 right-4 text-slate-400 hover:text-slate-600">
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                </button>
                {sent ? (
                    <div className="text-center py-6">
                        <div className="w-16 h-16 bg-emerald-50 text-emerald-500 rounded-full flex items-center justify-center mx-auto mb-4">
                            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
                        </div>
                        <h3 className="text-xl font-bold text-slate-800 mb-2">Đã gửi yêu cầu!</h3>
                        <p className="text-slate-500 text-sm">Chủ nhà sẽ liên hệ lại với bạn qua số điện thoại đã để sớm nhất có thể.</p>
                        <button onClick={onClose} className="mt-6 px-6 py-2.5 bg-indigo-600 text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors">Đóng</button>
                    </div>
                ) : (
                    <>
                        <h3 className="text-xl font-bold text-slate-800 mb-1">Liên hệ xem phòng</h3>
                        {(room.hostelName || room.landlordName) && (
                            <div className="bg-indigo-50 border border-indigo-100 rounded-lg p-2.5 mb-3 flex flex-col gap-1">
                                {room.hostelName && <div className="text-sm font-bold text-indigo-700 flex items-center gap-1.5"><svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg> {room.hostelName}</div>}
                                {room.landlordName && <div className="text-xs font-semibold text-indigo-600 flex items-center gap-1.5"><svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg> Quản lý: {room.landlordName}</div>}
                            </div>
                        )}
                        <p className="text-slate-500 text-sm mb-5">Phòng: <span className="font-semibold text-slate-800">{room.roomName || room.name}</span></p>
                        <form onSubmit={handleSubmit} className="space-y-4">
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 mb-1.5 uppercase tracking-wide">Họ và Tên *</label>
                                <input type="text" required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} placeholder="Nguyễn Văn A" className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 mb-1.5 uppercase tracking-wide">Số điện thoại *</label>
                                <input type="tel" required value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })} placeholder="09xxxxxxxx" className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500" />
                            </div>
                            <div>
                                <label className="block text-xs font-semibold text-slate-600 mb-1.5 uppercase tracking-wide">Lời nhắn (tuỳ chọn)</label>
                                <textarea value={form.note} onChange={e => setForm({ ...form, note: e.target.value })} placeholder="Ví dụ: Tôi muốn thuê từ tháng 5..." rows={3} className="w-full px-4 py-2.5 border border-slate-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500 resize-none" />
                            </div>
                            <button type="submit" disabled={sending} className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white rounded-xl font-bold transition-colors">
                                {sending ? 'Đang gửi...' : 'Gửi yêu cầu xem phòng'}
                            </button>
                        </form>
                    </>
                )}
            </div>
        </div>
    );
}

// ─── Skeleton Card ────────────────────────────────────────────────────────────
function SkeletonCard() {
    return (
        <div className="bg-white rounded-2xl overflow-hidden border border-slate-200 shadow-sm animate-pulse">
            <div className="h-52 bg-slate-200" />
            <div className="p-5 space-y-3">
                <div className="h-5 bg-slate-200 rounded-lg w-3/4" />
                <div className="h-7 bg-slate-200 rounded-lg w-1/2" />
                <div className="h-4 bg-slate-200 rounded-lg w-full" />
                <div className="h-4 bg-slate-200 rounded-lg w-5/6" />
                <div className="grid grid-cols-2 gap-2 pt-2">
                    <div className="h-14 bg-slate-200 rounded-lg" />
                    <div className="h-14 bg-slate-200 rounded-lg" />
                </div>
                <div className="h-11 bg-slate-200 rounded-xl mt-2" />
            </div>
        </div>
    );
}

// ─── Room Card ────────────────────────────────────────────────────────────────
const PLACEHOLDER_IMG = 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&q=80';

function RoomCard({ room, nf, onContact }) {
    return (
        <div className="group bg-white rounded-2xl overflow-hidden border border-slate-200 hover:border-slate-300 hover:shadow-xl transition-all duration-300 flex flex-col shadow-sm">
            <Link href={`/rooms/${room.uid}/${room.id}`} className="block w-full h-52 overflow-hidden relative bg-slate-100 text-left">
                <span className="absolute top-3 left-3 z-10 bg-white/90 backdrop-blur-sm border border-emerald-200 text-emerald-600 text-xs font-bold px-3 py-1.5 rounded-full shadow-sm">Đang Trống</span>
                <img
                    src={room.imageUrl || room.imgUrl || (typeof room.images === 'string' ? room.images : (Array.isArray(room.images) && room.images.length > 0 ? room.images[0] : null)) || room.image || PLACEHOLDER_IMG}
                    alt={room.roomName || room.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700"
                    loading="lazy"
                />
            </Link>
            <div className="p-5 flex-1 flex flex-col">
                <h2 className="text-lg font-bold text-slate-800 hover:text-indigo-600 transition-colors line-clamp-1 mb-1.5 cursor-pointer" onClick={() => onContact(room)}>{room.roomName || room.name}</h2>
                <div className="text-2xl font-black text-slate-900 mb-3 font-mono tracking-tight">
                    {nf.format(room.price)}<span className="text-sm font-medium text-slate-400 font-sans tracking-normal ml-1">đ/tháng</span>
                </div>
                <p className="text-slate-500 text-sm mb-5 line-clamp-2 flex-1">{room.note || room.description || 'Chưa có mô tả từ chủ nhà.'}</p>

                <div className="grid grid-cols-2 gap-2 mb-5">
                    <div className="bg-slate-50 border border-slate-100 rounded-xl p-3 flex items-center gap-2.5">
                        <div className="text-amber-500 bg-amber-50 rounded-lg p-1.5">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
                        </div>
                        <div>
                            <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Tiền Điện</div>
                            <div className="text-sm font-bold text-slate-700">{room.electricPrice ? nf.format(room.electricPrice) : '3.500'}đ</div>
                        </div>
                    </div>
                    <div className="bg-slate-50 border border-slate-100 rounded-xl p-3 flex items-center gap-2.5">
                        <div className="text-sky-500 bg-sky-50 rounded-lg p-1.5">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 18v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2m8-11a4 4 0 100-8 4 4 0 000 8zM19 12a10.001 10.001 0 011 20H4a10 10 0 011-20" /></svg>
                        </div>
                        <div>
                            <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Tiền Nước</div>
                            <div className="text-sm font-bold text-slate-700">{room.waterPrice ? nf.format(room.waterPrice) : '70.000'}đ</div>
                        </div>
                    </div>
                    {(room.serviceFee > 0 || room.trashFee > 0) && (
                        <div className="bg-slate-50 border border-slate-100 rounded-xl p-3 flex items-center gap-2.5 col-span-2">
                            <div className="text-emerald-500 bg-emerald-50 rounded-lg p-1.5">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
                            </div>
                            <div className="flex-1 flex justify-between items-center">
                                <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Dịch vụ &amp; Rác</div>
                                <div className="text-sm font-bold text-slate-700">+{nf.format((room.serviceFee || 0) + (room.trashFee || 0))}đ</div>
                            </div>
                        </div>
                    )}
                    {room.wifiPrice > 0 && (
                        <div className="bg-slate-50 border border-slate-100 rounded-xl p-3 flex items-center gap-2.5 col-span-2">
                            <div className="text-violet-500 bg-violet-50 rounded-lg p-1.5">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0" /></svg>
                            </div>
                            <div className="flex-1 flex justify-between items-center">
                                <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">Wi-Fi</div>
                                <div className="text-sm font-bold text-slate-700">+{nf.format(room.wifiPrice)}đ</div>
                            </div>
                        </div>
                    )}
                </div>

                {(room.hostelName || room.landlordName || room.hostelAddress || room.landlordPhone) && (
                    <div className="bg-gradient-to-r from-slate-50 to-white border border-slate-200 rounded-xl p-3 mb-4 text-xs text-slate-600 space-y-1">
                        {room.hostelName && <div className="font-bold text-slate-800 flex items-center gap-1.5"><svg className="w-3.5 h-3.5 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4" /></svg> {room.hostelName}</div>}
                        {room.landlordName && <div className="flex items-center gap-1.5"><svg className="w-3.5 h-3.5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg> Quản lý: <span className="font-semibold text-slate-700">{room.landlordName}</span></div>}
                        {room.landlordPhone && <a href={`tel:${room.landlordPhone}`} className="flex items-center gap-1.5 text-indigo-600 font-semibold hover:underline"><svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg> {room.landlordPhone}</a>}
                        {room.hostelAddress && <div className="flex items-start gap-1.5"><svg className="w-3.5 h-3.5 text-slate-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg> <span className="line-clamp-2">{room.hostelAddress}</span></div>}
                    </div>
                )}

                <div className="grid grid-cols-2 gap-2">
                    <Link href={`/rooms/${room.uid}/${room.id}`} className="py-3 border border-slate-200 hover:border-slate-300 text-slate-700 hover:text-indigo-600 rounded-xl font-semibold transition-colors text-sm flex items-center justify-center gap-1.5">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" /></svg>
                        Xem chi tiết
                    </Link>
                    <button onClick={() => onContact(room)} className="py-3 bg-slate-900 hover:bg-slate-700 text-white rounded-xl font-bold transition-colors text-sm shadow-sm flex items-center justify-center gap-2">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
                        Liên hệ
                    </button>
                </div>
            </div>
        </div>
    );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function PublicRoomsPage() {
    const [rooms, setRooms] = useState([]);
    const [loading, setLoading] = useState(true);
    const [search, setSearch] = useState('');
    const [addressSearch, setAddressSearch] = useState('');
    const [selectedDistrict, setSelectedDistrict] = useState('');
    const [priceRange, setPriceRange] = useState('');
    const [sortBy, setSortBy] = useState('default');
    const [contactRoom, setContactRoom] = useState(null);

    useEffect(() => {
        async function fetchRooms() {
            try {
                const q = query(collectionGroup(db, 'rooms'));
                const snapshot = await getDocs(q);

                const allRooms = snapshot.docs.map(d => {
                    const uid = d.ref.path.split('/')[1];
                    return { id: d.id, uid, ...d.data() };
                }).filter(room => room.status === 'TRONG');

                // Dedup: nếu cùng uid+id thì chỉ giữ 1 (lấy bản có imageUrl trước)
                const seenKeys = new Set();
                const rawRooms = [];
                for (const room of allRooms) {
                    const key = `${room.uid}_${room.id}`;
                    if (!seenKeys.has(key)) {
                        seenKeys.add(key);
                        rawRooms.push(room);
                    }
                }

                const uniqueUids = [...new Set(rawRooms.map(r => r.uid))];
                const settingsMap = {};
                await Promise.all(uniqueUids.map(async (uid) => {
                    const sDoc = await getDoc(doc(db, `users/${uid}/settings/config`));
                    if (sDoc.exists()) settingsMap[uid] = sDoc.data();
                }));

                const roomList = await Promise.all(rawRooms.map(async r => {
                    const s = settingsMap[r.uid] || {};
                    let finalImageUrl = r.imageUrl || r.imgUrl || r.images || r.image || null;

                    return {
                        ...r,
                        images: finalImageUrl,
                        electricPrice: s.electricPrice || null,
                        waterPrice: s.waterPrice || null,
                        wifiPrice: s.wifiPrice || null,
                        serviceFee: s.serviceFee || null,
                        trashFee: s.trashFee || null,
                        hostelName: s.hostelName || null,
                        landlordName: s.landlordName || null,
                        landlordPhone: s.landlordPhone || null,
                        hostelAddress: s.hostelAddress || null
                    };
                }));

                // Dedup lần 2: xử lý trường hợp cùng phòng nhưng khác UID
                // (xảy ra khi user backup dữ liệu dưới 2 tài khoản Firebase khác nhau)
                const seenRoomKeys = new Set();
                const dedupedList = [];
                for (const room of roomList) {
                    const key = `${(room.hostelAddress || '').trim().toLowerCase()}__${(room.roomName || room.name || '').trim().toLowerCase()}`;
                    if (!seenRoomKeys.has(key)) {
                        seenRoomKeys.add(key);
                        dedupedList.push(room);
                    } else {
                        const idx = dedupedList.findIndex(r => {
                            const k = `${(r.hostelAddress || '').trim().toLowerCase()}__${(r.roomName || r.name || '').trim().toLowerCase()}`;
                            return k === key;
                        });
                        if (idx !== -1) {
                            const existing = dedupedList[idx];
                            // Ưu tiên bản có SĐT hoặc bản có Ảnh thực tế
                            if (room.landlordPhone && !existing.landlordPhone) {
                                dedupedList[idx] = room;
                            } else if (room.images && !existing.images) {
                                dedupedList[idx] = room;
                            }
                        }
                    }
                }

                if (dedupedList.length === 0) {
                    setRooms([
                        { id: 'mock1', uid: 'demo', roomName: 'Phòng 101 - Cao cấp', price: 3500000, images: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&q=80&w=800', note: 'Có cửa sổ thoáng mát, full nội thất, giường nệm cao cấp.', electricPrice: 3500, waterPrice: 80000, wifiPrice: 80000, hostelName: 'Trọ Cao Cấp Quận 7', landlordName: 'Ngô Lê Gia Cát', landlordPhone: '0901234567', hostelAddress: 'Nguyễn Thị Thập, P. Tân Phong, Quận 7' },
                        { id: 'mock2', uid: 'demo2', roomName: 'Studio Mini Tâm Đôn', price: 4200000, images: 'https://images.unsplash.com/photo-1502672260266-1c1de2d9d000?auto=format&fit=crop&q=80&w=800', note: 'Cửa sổ lớn, ban công, máy giặt riêng, giờ giấc tự do.', electricPrice: 4000, waterPrice: 100000, wifiPrice: 0, hostelName: 'Tâm Đôn Apart', landlordName: 'Trần Văn Tâm', landlordPhone: '0912345678', hostelAddress: 'Hẻm 12 Thích Quảng Đức, Quận Phú Nhuận' },
                        { id: 'mock3', uid: 'demo3', roomName: 'Phòng Trọ Sinh Viên', price: 2000000, images: 'https://images.unsplash.com/photo-1555854877-bab0e564b8d5?auto=format&fit=crop&q=80&w=800', note: 'Khu an ninh, gần siêu thị CoopMart, phù hợp sinh viên.', electricPrice: 3500, waterPrice: 70000, wifiPrice: 60000, serviceFee: 30000, hostelName: 'Trọ Làng Đại Học', landlordName: 'Cô Nụ', landlordPhone: '0978901234', hostelAddress: 'Khu phố 6, Linh Trung, Thủ Đức' },
                        { id: 'mock4', uid: 'demo4', roomName: 'Phòng Đơn Bình Thạnh', price: 2800000, images: 'https://images.unsplash.com/photo-1631049307264-da0ec9d70304?auto=format&fit=crop&q=80&w=800', note: 'Gần chợ, yên tĩnh, có máy lạnh và tủ lạnh.', electricPrice: 3500, waterPrice: 60000, wifiPrice: 50000, hostelName: 'Trọ Bình Thạnh Xanh', landlordName: 'Anh Minh', landlordPhone: '0934567890', hostelAddress: '25 Đinh Bộ Lĩnh, Phường 26, Bình Thạnh' },
                    ]);
                } else {
                    setRooms(dedupedList);
                }
            } catch (e) {
                console.error('Lỗi xem phòng', e);
            } finally {
                setLoading(false);
            }
        }
        fetchRooms();
    }, []);

    // Build district list dynamically from loaded rooms
    const districts = useMemo(() => {
        const set = new Set();
        rooms.forEach(r => {
            if (r.hostelAddress) {
                // Extract last 2 comma-separated parts as district hint
                const parts = r.hostelAddress.split(',');
                const district = parts[parts.length - 1]?.trim();
                if (district) set.add(district);
            }
        });
        return [...set].sort();
    }, [rooms]);

    const nf = new Intl.NumberFormat('vi-VN');

    const filtered = useMemo(() => {
        let result = rooms.filter(r => {
            const name = removeAccents(r.roomName || r.name || '');
            const addr = removeAccents(r.hostelAddress || '');
            const hostel = removeAccents(r.hostelName || '');
            if (search && !name.includes(removeAccents(search))) return false;
            if (addressSearch && !addr.includes(removeAccents(addressSearch)) && !hostel.includes(removeAccents(addressSearch))) return false;
            if (selectedDistrict && !(r.hostelAddress || '').includes(selectedDistrict)) return false;
            const p = r.price || 0;
            if (priceRange === 'lt2') return p < 2_000_000;
            if (priceRange === '2to4') return p >= 2_000_000 && p < 4_000_000;
            if (priceRange === '4to6') return p >= 4_000_000 && p < 6_000_000;
            if (priceRange === 'gt6') return p >= 6_000_000;
            return true;
        });
        if (sortBy === 'price-asc') result.sort((a, b) => (a.price || 0) - (b.price || 0));
        else if (sortBy === 'price-desc') result.sort((a, b) => (b.price || 0) - (a.price || 0));
        return result;
    }, [rooms, search, addressSearch, selectedDistrict, priceRange, sortBy]);

    const hasFilter = search || addressSearch || selectedDistrict || priceRange || sortBy !== 'default';

    return (
        <div className="min-h-screen bg-slate-50 text-slate-900 font-sans">
            {contactRoom && <ContactModal room={contactRoom} onClose={() => setContactRoom(null)} />}

            {/* Hero Header */}
            <div className="bg-white border-b border-slate-200 px-6 py-12 text-center shadow-sm">
                <h1 className="text-4xl md:text-5xl font-extrabold mb-3 text-slate-900 tracking-tight">
                    Phòng Trống Cho Thuê
                </h1>
                <p className="text-slate-500 max-w-xl mx-auto mb-8">
                    Danh sách phòng trọ, căn hộ, studio đang trống từ mạng lưới chủ nhà sử dụng Rental Manager.
                </p>

                {/* Search Bar Row */}
                <div className="max-w-3xl mx-auto flex flex-col sm:flex-row gap-3 mb-4">
                    {/* Room name search */}
                    <div className="relative flex-1">
                        <svg className="absolute left-4 top-3.5 w-5 h-5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                        <input
                            type="text"
                            placeholder="Tìm tên phòng..."
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                            className="w-full pl-12 pr-4 py-3 rounded-xl border border-slate-200 bg-white shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>
                    {/* Address keyword search */}
                    <div className="relative flex-1">
                        <svg className="absolute left-4 top-3.5 w-5 h-5 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                        <input
                            type="text"
                            placeholder="Tìm theo địa chỉ, khu vực..."
                            value={addressSearch}
                            onChange={e => setAddressSearch(e.target.value)}
                            className="w-full pl-12 pr-4 py-3 rounded-xl border border-slate-200 bg-white shadow-sm text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500"
                        />
                    </div>
                </div>

                {/* Filter Row */}
                <div className="max-w-3xl mx-auto flex flex-wrap gap-2 justify-center">
                    {/* District filter */}
                    {districts.length > 0 && (
                        <select
                            value={selectedDistrict}
                            onChange={e => setSelectedDistrict(e.target.value)}
                            className="px-4 py-2 rounded-xl border border-slate-200 bg-white text-sm font-medium text-slate-700 shadow-sm focus:outline-none focus:ring-2 focus:ring-slate-500 cursor-pointer"
                        >
                            <option value="">Tất cả khu vực</option>
                            {districts.map(d => <option key={d} value={d}>{d}</option>)}
                        </select>
                    )}

                    {/* Price filter chips */}
                    {[
                        { label: 'Tất cả mức giá', value: '' },
                        { label: 'Dưới 2 triệu', value: 'lt2' },
                        { label: '2 – 4 triệu', value: '2to4' },
                        { label: '4 – 6 triệu', value: '4to6' },
                        { label: 'Trên 6 triệu', value: 'gt6' },
                    ].map(opt => (
                        <button
                            key={opt.value}
                            onClick={() => setPriceRange(opt.value)}
                            className={`px-4 py-2 rounded-xl border text-sm font-medium transition-colors shadow-sm ${priceRange === opt.value
                                ? 'bg-slate-900 text-white border-slate-900'
                                : 'bg-white text-slate-600 border-slate-200 hover:border-slate-300 hover:text-slate-900'}`}
                        >
                            {opt.label}
                        </button>
                    ))}

                    {/* Sort chips */}
                    <div className="flex gap-1.5 items-center">
                        <span className="text-xs text-slate-400 font-medium px-1">Sắp xếp:</span>
                        {[
                            { label: 'Thấp nhất', value: 'price-asc', icon: <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4h13M3 8h9m-9 4h6m4 0l4-4m0 0l4 4m-4-4v12" /></svg> },
                            { label: 'Cao nhất', value: 'price-desc', icon: <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4h13M3 8h9m-9 4h9m5-4v12m0 0l-4-4m4 4l4-4" /></svg> },
                        ].map(opt => (
                            <button
                                key={opt.value}
                                onClick={() => setSortBy(sortBy === opt.value ? 'default' : opt.value)}
                                className={`px-3 py-2 rounded-xl border text-sm font-medium transition-colors shadow-sm flex items-center gap-1 ${sortBy === opt.value
                                    ? 'bg-slate-900 text-white border-slate-900'
                                    : 'bg-white text-slate-600 border-slate-200 hover:border-slate-300'}`}
                            >
                                {opt.icon}
                                {opt.label}
                            </button>
                        ))}
                    </div>

                    {/* Clear filters */}
                    {hasFilter && (
                        <button
                            onClick={() => { setSearch(''); setAddressSearch(''); setSelectedDistrict(''); setPriceRange(''); setSortBy('default'); }}
                            className="px-4 py-2 rounded-xl border border-rose-200 bg-rose-50 text-rose-600 text-sm font-medium hover:bg-rose-100 transition-colors shadow-sm flex items-center gap-1.5"
                        >
                            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                            Xoá bộ lọc
                        </button>
                    )}
                </div>
            </div>

            {/* Results */}
            <div className="max-w-6xl mx-auto px-4 py-10">
                {loading ? (
                    <>
                        <div className="h-5 bg-slate-200 rounded w-40 mb-6 animate-pulse" />
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {[1, 2, 3].map(i => <SkeletonCard key={i} />)}
                        </div>
                    </>
                ) : (
                    <>
                        <p className="text-slate-500 text-sm mb-6">
                            {filtered.length > 0
                                ? <><span className="font-semibold text-slate-700">{filtered.length}</span> phòng đang trống{hasFilter ? ' (đã lọc)' : ''}</>
                                : <span className="text-rose-500">Không tìm thấy phòng phù hợp với bộ lọc hiện tại.</span>
                            }
                        </p>
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            {filtered.map(room => (
                                <RoomCard key={`${room.uid}_${room.id}`} room={room} nf={nf} onContact={setContactRoom} />
                            ))}
                        </div>

                        {filtered.length === 0 && (
                            <div className="text-center py-20">
                                <div className="text-6xl mb-4">🔍</div>
                                <h3 className="text-xl font-bold text-slate-700 mb-2">Không có kết quả</h3>
                                <p className="text-slate-500 text-sm">Thử thay đổi từ khoá hoặc điều chỉnh bộ lọc để tìm phòng phù hợp.</p>
                                <button onClick={() => { setSearch(''); setAddressSearch(''); setSelectedDistrict(''); setPriceRange(''); }} className="mt-6 px-6 py-2.5 bg-indigo-600 text-white rounded-xl font-semibold text-sm hover:bg-indigo-700 transition-colors">
                                    Xoá bộ lọc
                                </button>
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}
