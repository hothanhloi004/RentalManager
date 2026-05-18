'use client';
import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { doc, getDoc, addDoc, collection, serverTimestamp } from 'firebase/firestore';
import { db } from '../../../../lib/firebase';
import Link from 'next/link';

const formatMoney = (n) => new Intl.NumberFormat('vi-VN').format(n || 0);

export default function RoomDetailPage() {
    const params = useParams();
    const router = useRouter();
    const { uid, id } = params;

    const [room, setRoom] = useState(null);
    const [settings, setSettings] = useState(null);
    const [images, setImages] = useState([]);
    const [currentImage, setCurrentImage] = useState(0);
    const [loading, setLoading] = useState(true);
    const [showContact, setShowContact] = useState(false);
    const [form, setForm] = useState({ name: '', phone: '', note: '' });
    const [sending, setSending] = useState(false);
    const [sent, setSent] = useState(false);

    useEffect(() => {
        async function fetchDetail() {
            try {
                // Fetch room
                const roomDoc = await getDoc(doc(db, `users/${uid}/rooms/${id}`));
                if (!roomDoc.exists()) {
                    setLoading(false);
                    return;
                }
                setRoom({ id: roomDoc.id, ...roomDoc.data() });

                // Fetch settings
                const settingsDoc = await getDoc(doc(db, `users/${uid}/settings/config`));
                if (settingsDoc.exists()) {
                    setSettings(settingsDoc.data());
                } else {
                    setSettings({});
                }

                // Set images from Firestore (ImgBB URLs)
                const data = roomDoc.data();
                const roomImages = data.imageUrl || data.imgUrl || data.images || data.image;
                if (roomImages) {
                    setImages(Array.isArray(roomImages) ? roomImages : [roomImages]);
                } else {
                    setImages(['https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&q=80&w=1200']);
                }

                // Done loading core data
                setLoading(false);

            } catch (err) {
                console.error("Lỗi xem chi tiết", err);
                setLoading(false);
            }
        }

        if (uid && id) {
            fetchDetail();
        }
    }, [uid, id]);

    if (loading) {
        return (
            <div className="min-h-screen bg-slate-50 flex items-center justify-center pt-16">
                <div className="w-8 h-8 border-4 border-slate-200 border-t-indigo-600 rounded-full animate-spin" />
            </div>
        );
    }

    if (!room) {
        return (
            <div className="min-h-screen bg-slate-50 flex items-center justify-center pt-16">
                <div className="text-center">
                    <h2 className="text-xl font-bold text-slate-800">Không tìm thấy phòng</h2>
                    <p className="text-slate-500 mt-2">Phòng này có thể đã bị xóa hoặc không tồn tại.</p>
                    <button onClick={() => router.push('/rooms')} className="mt-4 px-6 py-2 bg-indigo-600 text-white rounded-lg font-semibold hover:bg-indigo-700">Go back</button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50 pt-20 pb-16">
            <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
                {/* Breadcrumb */}
                <div className="mb-6 flex items-center text-sm font-medium text-slate-500">
                    <Link href="/rooms" className="hover:text-indigo-600 flex items-center gap-1">
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" /></svg>
                        Tất cả phòng
                    </Link>
                    <span className="mx-2">/</span>
                    <span className="text-slate-800 truncate">{room.roomName || 'Chi tiết phòng'}</span>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    {/* Left: Images & Info */}
                    <div className="lg:col-span-2 space-y-6">
                        {/* Image Gallery */}
                        <div className="bg-white p-2 rounded-3xl border border-slate-200 shadow-sm relative group overflow-hidden">
                            <span className="absolute top-6 left-6 z-10 bg-white/90 backdrop-blur-sm border border-emerald-200 text-emerald-600 text-xs font-bold px-3 py-1.5 rounded-full shadow-sm">
                                Đang Trống
                            </span>
                            <div className="aspect-[4/3] sm:aspect-[16/9] w-full rounded-2xl overflow-hidden relative bg-slate-100">
                                <img src={images[currentImage]} alt="Room Image" className="w-full h-full object-cover" />
                            </div>

                            {images.length > 1 && (
                                <div className="flex gap-2 p-2 overflow-x-auto mt-2 custom-scrollbar">
                                    {images.map((img, idx) => (
                                        <button
                                            key={idx}
                                            onClick={() => setCurrentImage(idx)}
                                            className={`h-20 w-32 flex-shrink-0 rounded-xl overflow-hidden border-2 transition-all ${idx === currentImage ? 'border-indigo-600 opacity-100' : 'border-transparent opacity-60 hover:opacity-100'}`}
                                        >
                                            <img src={img} className="w-full h-full object-cover" />
                                        </button>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Room Info */}
                        <div className="bg-white p-6 sm:p-8 rounded-3xl border border-slate-200 shadow-sm">
                            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-800 mb-2">{room.roomName || 'Phòng Trống'}</h1>
                            <div className="text-2xl font-black text-indigo-600 mb-6">{formatMoney(room.price)}<span className="text-base text-slate-500 font-semibold">đ / tháng</span></div>

                            <div className="space-y-6">
                                <div>
                                    <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider mb-3">Thông tin chi tiết</h3>
                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                                        <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100">
                                            <div className="p-2 bg-white rounded-lg text-amber-500 shadow-sm"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" /></svg></div>
                                            <div>
                                                <div className="text-[11px] font-bold text-slate-500 uppercase">Tiền Điện</div>
                                                <div className="text-sm font-bold text-slate-800">{settings?.electricPrice ? formatMoney(settings.electricPrice) + 'đ' : 'Gặp chủ nhà'}</div>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100">
                                            <div className="p-2 bg-white rounded-lg text-sky-500 shadow-sm"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 18v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2m8-11a4 4 0 100-8 4 4 0 000 8zM19 12a10.001 10.001 0 011 20H4a10 10 0 011-20" /></svg></div>
                                            <div>
                                                <div className="text-[11px] font-bold text-slate-500 uppercase">Tiền Nước</div>
                                                <div className="text-sm font-bold text-slate-800">{settings?.waterPrice ? formatMoney(settings.waterPrice) + 'đ' : 'Gặp chủ nhà'}</div>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100">
                                            <div className="p-2 bg-white rounded-lg text-violet-500 shadow-sm"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.141 0M1.394 9.393c5.857-5.857 15.355-5.857 21.213 0" /></svg></div>
                                            <div>
                                                <div className="text-[11px] font-bold text-slate-500 uppercase">Wi-Fi</div>
                                                <div className="text-sm font-bold text-slate-800">{settings?.wifiPrice ? formatMoney(settings.wifiPrice) + 'đ' : 'Gặp chủ nhà'}</div>
                                            </div>
                                        </div>
                                        <div className="flex items-center gap-3 p-3 bg-slate-50 rounded-xl border border-slate-100">
                                            <div className="p-2 bg-white rounded-lg text-emerald-500 shadow-sm"><svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg></div>
                                            <div>
                                                <div className="text-[11px] font-bold text-slate-500 uppercase">Dịch vụ chung</div>
                                                <div className="text-sm font-bold text-slate-800">{settings?.serviceFee || settings?.trashFee ? formatMoney((settings.serviceFee || 0) + (settings.trashFee || 0)) + 'đ' : 'Theo quy định'}</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {room.note && (
                                    <div>
                                        <h3 className="text-sm font-bold text-slate-800 uppercase tracking-wider mb-2">Mô tả thêm</h3>
                                        <div className="text-slate-600 bg-slate-50 p-4 rounded-xl leading-relaxed text-sm border border-slate-100 whitespace-pre-line">
                                            {room.note}
                                        </div>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

                    {/* Right: Landlord Info & Action */}
                    <div className="lg:col-span-1">
                        <div className="bg-white p-6 rounded-3xl border border-slate-200 shadow-sm sticky top-24">
                            <h3 className="text-base font-bold text-slate-800 mb-4">Thông tin Quản lý</h3>

                            <div className="space-y-4 mb-6">
                                {(settings?.hostelName || settings?.landlordName) && (
                                    <div className="flex gap-3">
                                        <div className="w-10 h-10 rounded-full bg-indigo-100 text-indigo-600 flex items-center justify-center font-bold flex-shrink-0">
                                            {(settings?.landlordName || settings?.hostelName || 'A')[0].toUpperCase()}
                                        </div>
                                        <div>
                                            <div className="font-bold text-slate-800">{settings?.landlordName || 'Chủ trọ'}</div>
                                            <div className="text-sm text-slate-500">{settings?.hostelName || 'Khu trọ'}</div>
                                        </div>
                                    </div>
                                )}

                                {settings?.hostelAddress && (
                                    <div className="flex items-start gap-3 p-3 bg-slate-50 rounded-xl text-sm text-slate-600">
                                        <svg className="w-5 h-5 text-slate-400 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" /><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" /></svg>
                                        <span>{settings.hostelAddress}</span>
                                    </div>
                                )}

                                {settings?.landlordPhone && (
                                    <a href={`tel:${settings.landlordPhone}`} className="flex items-center gap-3 p-3 bg-indigo-50 border border-indigo-100 rounded-xl text-indigo-700 font-bold hover:bg-indigo-100 transition-colors">
                                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>
                                        <span>{settings.landlordPhone}</span>
                                    </a>
                                )}
                            </div>

                            <button onClick={() => setShowContact(true)} className="w-full py-3.5 bg-slate-900 hover:bg-slate-800 text-white rounded-xl font-bold transition-all shadow-md">
                                Gửi yêu cầu xem phòng
                            </button>
                        </div>
                    </div>
                </div>
            </div>

            {/* Contact Modal — lưu Firestore + push notify */}
            {showContact && (
                <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-sm z-[1000] flex items-center justify-center p-4" onClick={() => { if (!sending) { setShowContact(false); setSent(false); setForm({ name: '', phone: '', note: '' }); } }}>
                    <div className="bg-white rounded-3xl p-6 md:p-8 max-w-md w-full shadow-2xl relative" onClick={e => e.stopPropagation()}>
                        <button onClick={() => { setShowContact(false); setSent(false); setForm({ name: '', phone: '', note: '' }); }} className="absolute top-6 right-6 p-2 rounded-full border border-slate-200 text-slate-400 hover:text-slate-600 hover:border-slate-300 transition-all">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" /></svg>
                        </button>
                        {sent ? (
                            <div className="text-center py-6">
                                <div className="w-16 h-16 bg-emerald-50 text-emerald-500 rounded-full flex items-center justify-center mx-auto mb-4">
                                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" /></svg>
                                </div>
                                <h3 className="text-xl font-bold text-slate-800 mb-2">Đã gửi yêu cầu!</h3>
                                <p className="text-slate-500 text-sm">Chủ nhà sẽ liên hệ lại với bạn qua số điện thoại đã để sớm nhất có thể.</p>
                                <button onClick={() => { setShowContact(false); setSent(false); setForm({ name: '', phone: '', note: '' }); }} className="mt-6 px-6 py-2.5 bg-indigo-600 text-white rounded-xl font-semibold hover:bg-indigo-700 transition-colors">Đóng</button>
                            </div>
                        ) : (
                            <>
                                <h2 className="text-xl font-extrabold text-slate-800 mb-1">Liên hệ xem phòng</h2>
                                <p className="text-slate-500 text-sm mb-5">Phòng: <span className="font-semibold text-slate-800">{room?.roomName}</span></p>
                                {settings?.landlordPhone && (
                                    <a href={`tel:${settings.landlordPhone}`} className="flex items-center gap-2 bg-indigo-50 border border-indigo-100 rounded-xl px-4 py-3 mb-5 text-indigo-700 font-bold hover:bg-indigo-100 transition-colors">
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>
                                        Gọi ngay: {settings.landlordPhone}
                                    </a>
                                )}
                                <form onSubmit={async (e) => {
                                    e.preventDefault();
                                    if (!form.name || !form.phone) return;
                                    setSending(true);
                                    try {
                                        await addDoc(collection(db, `inquiries/${uid}/requests`), {
                                            roomId: id,
                                            roomName: room?.roomName || '',
                                            name: form.name,
                                            phone: form.phone,
                                            note: form.note,
                                            createdAt: serverTimestamp(),
                                            status: 'NEW',
                                        });
                                        try {
                                            await fetch('/api/notify', {
                                                method: 'POST',
                                                headers: { 'Content-Type': 'application/json' },
                                                body: JSON.stringify({
                                                    uid,
                                                    title: 'Khách hỏi xem phòng mới!',
                                                    message: `Khách ${form.name} (${form.phone}) muốn xem ${room?.roomName}.`
                                                })
                                            });
                                        } catch (_) { }
                                        setSent(true);
                                    } catch (err) {
                                        alert('Gửi thất bại: ' + err.message);
                                    } finally {
                                        setSending(false);
                                    }
                                }} className="space-y-4">
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
            )}
        </div>
    );
}
