import Link from 'next/link';
import { initializeApp, getApps } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { cert } from 'firebase-admin/app';

// ─── Firebase Admin init (Server Component only) ──────────────────────────────
function getAdminDb() {
  if (!getApps().length) {
    const serviceAccount = JSON.parse(
      process.env.FIREBASE_SERVICE_ACCOUNT_KEY || '{}'
    );
    if (!serviceAccount.project_id) return null; // env not configured
    initializeApp({ credential: cert(serviceAccount) });
  }
  try { return getFirestore(); } catch { return null; }
}

async function fetchLiveStats() {
  try {
    const db = getAdminDb();
    if (!db) throw new Error('no admin db');

    const [roomsSnap, usersSnap, billsSnap] = await Promise.all([
      db.collectionGroup('rooms').count().get(),
      db.collection('users').count().get(),
      db.collectionGroup('bills').count().get(),
    ]);

    const totalRooms = roomsSnap.data().count ?? 0;
    const totalLandlords = usersSnap.data().count ?? 0;
    const totalBills = billsSnap.data().count ?? 0;

    return {
      rooms: totalRooms > 0 ? totalRooms.toLocaleString('vi-VN') + '+' : '—',
      landlords: totalLandlords > 0 ? totalLandlords.toLocaleString('vi-VN') + '+' : '—',
      provinces: '63',
      bills: totalBills > 0 ? totalBills.toLocaleString('vi-VN') + '+' : '—',
    };
  } catch {
    // Fallback: env not set or Firebase Admin not configured → show dashes
    return { rooms: '—', landlords: '—', provinces: '63', bills: '—' };
  }
}

const features = [
  {
    icon: <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" /></svg>,
    title: 'Bản đồ Giá Thuê Chữ S',
    desc: 'Xem giá thuê trọ trung bình trực quan theo từng tỉnh thành với Heatmap Vector siêu nét, minh bạch hoàn toàn.',
    href: '/map',
    accent: 'indigo',
  },
  {
    icon: <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" /></svg>,
    title: 'Tìm Phòng Nhanh',
    desc: 'Hàng ngàn phòng trọ, căn hộ, studio đang trống từ hệ thống chủ nhà. Không cần tài khoản, xem miễn phí.',
    href: '/rooms',
    accent: 'sky',
  },
  {
    icon: <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>,
    title: 'Admin Dashboard',
    desc: 'Dành cho Chủ trọ. Đăng nhập bằng Google để quản lý hợp đồng, hoá đơn, khách thuê thời gian thực.',
    href: '/admin',
    accent: 'emerald',
  },
];

const accentMap = {
  indigo: { bg: 'bg-indigo-50', icon: 'text-indigo-600', border: 'hover:border-indigo-200', badge: 'bg-indigo-600 text-white' },
  sky: { bg: 'bg-sky-50', icon: 'text-sky-600', border: 'hover:border-sky-200', badge: 'bg-sky-600 text-white' },
  emerald: { bg: 'bg-emerald-50', icon: 'text-emerald-600', border: 'hover:border-emerald-200', badge: 'bg-emerald-600 text-white' },
};

export default async function HomePage() {
  const liveStats = await fetchLiveStats();

  const stats = [
    {
      label: 'Tổng số phòng',
      value: liveStats.rooms,
      icon: <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" /></svg>,
    },
    {
      label: 'Chủ trọ tham gia',
      value: liveStats.landlords,
      icon: <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" /></svg>,
    },
    {
      label: 'Tỉnh thành phố',
      value: liveStats.provinces,
      icon: <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0021 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" /></svg>,
    },
    {
      label: 'Hoá đơn quản lý',
      value: liveStats.bills,
      icon: <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>,
    },
  ];

  return (
    <div className="min-h-screen bg-slate-50 font-sans">
      {/* Hero Section */}
      <section className="relative overflow-hidden px-6 py-28 text-center bg-white border-b border-slate-200">
        <div className="absolute inset-0 opacity-40"
          style={{
            backgroundImage: 'linear-gradient(#e2e8f0 1px, transparent 1px), linear-gradient(90deg, #e2e8f0 1px, transparent 1px)',
            backgroundSize: '40px 40px'
          }}
        />
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-indigo-100 rounded-full blur-3xl opacity-60" />
        <div className="absolute bottom-0 right-1/4 w-80 h-80 bg-sky-100 rounded-full blur-3xl opacity-50" />

        <div className="relative z-10 max-w-4xl mx-auto">
          <div className="inline-flex items-center gap-2 bg-indigo-50 border border-indigo-100 rounded-full px-4 py-2 text-sm text-indigo-700 mb-8 font-medium shadow-sm">
            <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse"></span>
            Mạng lưới kết nối Chủ Trọ &amp; Người Thuê thời gian thực
          </div>

          <h1 className="text-5xl md:text-7xl font-black mb-6 leading-tight tracking-tight">
            <span className="bg-clip-text text-transparent bg-gradient-to-r from-indigo-600 via-blue-600 to-sky-500">
              Rental Ecosystem
            </span>
            <br />
            <span className="text-slate-700 text-3xl md:text-4xl font-semibold mt-2 block">Hiện đại. Trực quan. Minh bạch.</span>
          </h1>

          <p className="text-slate-500 text-lg md:text-xl max-w-2xl mx-auto mb-10 leading-relaxed">
            Nền tảng Quản lý và Tìm kiếm nhà trọ toàn diện. Dữ liệu từ App Android
            của Chủ trọ đồng bộ lên Web theo thời gian thực.
          </p>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Link href="/rooms" id="btn-find-rooms"
              className="px-8 py-4 bg-indigo-600 hover:bg-indigo-700 rounded-xl font-bold text-lg text-white transition-all duration-200 shadow-lg shadow-indigo-200 flex items-center justify-center gap-3">
              <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
              Xem Phòng Chờ Thuê
            </Link>
            <Link href="/admin" id="btn-admin"
              className="px-8 py-4 bg-white border border-slate-200 rounded-xl font-bold text-lg text-slate-700 hover:bg-slate-50 hover:border-slate-300 transition-all duration-200 shadow-sm flex items-center justify-center gap-3">
              <svg className="w-5 h-5 flex-shrink-0 text-slate-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
              Đăng nhập Chủ Trọ
            </Link>
          </div>
        </div>
      </section>

      {/* Stats — Live from Firestore */}
      <section className="py-16 px-6 bg-slate-50">
        <div className="max-w-5xl mx-auto grid grid-cols-2 md:grid-cols-4 gap-6">
          {stats.map((s) => (
            <div key={s.label} className="bg-white border border-slate-200 rounded-xl p-6 flex items-center gap-4 shadow-sm hover:shadow-md transition-shadow">
              <div className="p-3 bg-slate-50 text-slate-600 rounded-lg flex-shrink-0">{s.icon}</div>
              <div>
                <div className="text-2xl font-black text-slate-800 leading-tight">{s.value}</div>
                <div className="text-slate-500 text-xs font-medium mt-0.5">{s.label}</div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className="py-16 px-6 bg-white border-t border-slate-100">
        <div className="max-w-5xl mx-auto">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-slate-900 tracking-tight mb-3">Khám phá tính năng</h2>
            <p className="text-slate-500">Hệ sinh thái duy nhất đồng bộ dữ liệu Real-time từ Mobile đến Web</p>
          </div>
          <div className="grid md:grid-cols-3 gap-6">
            {features.map((f) => {
              const a = accentMap[f.accent];
              return (
                <Link key={f.title} href={f.href} className={`bg-white border border-slate-200 ${a.border} rounded-xl p-7 transition-all duration-300 hover:shadow-lg group flex flex-col`}>
                  <div className={`w-12 h-12 rounded-xl ${a.bg} ${a.icon} flex items-center justify-center mb-5 group-hover:scale-110 transition-transform`}>
                    {f.icon}
                  </div>
                  <h3 className="font-bold text-xl mb-2 text-slate-800">{f.title}</h3>
                  <p className="text-slate-500 text-sm leading-relaxed mb-6 flex-1">{f.desc}</p>
                  <div className={`text-sm font-semibold ${a.icon} flex items-center gap-1`}>
                    Trải nghiệm ngay <span className="group-hover:translate-x-1 transition-transform">→</span>
                  </div>
                </Link>
              );
            })}
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white py-8">
        <div className="max-w-5xl mx-auto px-6 flex flex-col sm:flex-row justify-between items-center gap-4 text-sm text-slate-400">
          <p>© 2026 Rental Manager — Hệ Sinh Thái Trọ &amp; Căn Hộ</p>
          <div className="flex gap-6">
            <Link href="/map" className="hover:text-indigo-600 transition-colors">Bản đồ giá</Link>
            <Link href="/rooms" className="hover:text-indigo-600 transition-colors">Tìm phòng</Link>
            <Link href="/tenant" className="hover:text-indigo-600 transition-colors">Tra hóa đơn</Link>
            <Link href="/admin" className="hover:text-indigo-600 transition-colors">Admin</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
