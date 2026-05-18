'use client';
import { useState, useEffect } from 'react';
import { db, auth } from '../../lib/firebase';
import { signInWithPopup, GoogleAuthProvider, signOut, onAuthStateChanged } from 'firebase/auth';
import { collection, query, getDocs, onSnapshot, orderBy } from 'firebase/firestore';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import * as XLSX from 'xlsx';

export default function AdminDashboardPage() {
    const [user, setUser] = useState(null);
    const [loadingAuth, setLoadingAuth] = useState(true);

    const [stats, setStats] = useState({ rooms: 0, tenants: 0, contracts: 0, bills: 0 });
    const [roomsList, setRoomsList] = useState([]);
    const [unpaidBills, setUnpaidBills] = useState([]);
    const [tenantsList, setTenantsList] = useState([]);
    const [contractsList, setContractsList] = useState([]);
    const [revenueData, setRevenueData] = useState([]);
    const [loadingData, setLoadingData] = useState(false);
    const [billMonthFilter, setBillMonthFilter] = useState('ALL');
    const [exportMonth, setExportMonth] = useState('ALL');
    const [inquiries, setInquiries] = useState([]);
    const [readIds, setReadIds] = useState(() => {
        // Persist read state in localStorage
        try { return new Set(JSON.parse(localStorage.getItem('readInquiryIds') || '[]')); }
        catch { return new Set(); }
    });
    const [settingData, setSettingData] = useState({});
    const [activeTab, setActiveTab] = useState('dashboard'); // 'dashboard' | 'inquiries'
    const [aiAnalysis, setAiAnalysis] = useState('');
    const [aiAnalysisLoading, setAiAnalysisLoading] = useState(false);
    const [aiReplies, setAiReplies] = useState({}); // { [inquiryId]: text }
    const [aiReplyLoading, setAiReplyLoading] = useState({}); // { [inquiryId]: bool }

    const currentMonthKey = new Date().toISOString().slice(0, 7);
    const currentMonthUnpaidBills = unpaidBills.filter(b => b.month === currentMonthKey);
    const currentMonthOutstanding = currentMonthUnpaidBills.reduce((sum, bill) => {
        const total = bill.totalAmount || 0;
        const paid = bill.paidAmount || 0;
        return sum + Math.max(total - paid, 0);
    }, 0);

    const callGemini = async (type, data) => {
        const res = await fetch('/api/gemini', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ type, data }),
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.error || 'Lỗi AI');
        return json.text;
    };

    useEffect(() => {
        const unsubscribe = onAuthStateChanged(auth, (currentUser) => {
            setUser(currentUser);
            setLoadingAuth(false);
            if (currentUser) {
                fetchLandlordData(currentUser.uid);
            }
        });
        return () => unsubscribe();
    }, []);

    // Real-time listener cho yêu cầu xem phòng
    useEffect(() => {
        if (!user) return;
        const q = query(
            collection(db, `inquiries/${user.uid}/requests`),
            orderBy('createdAt', 'desc')
        );
        const unsub = onSnapshot(q, (snap) => {
            const list = snap.docs.map(d => ({ id: d.id, ...d.data() }));
            setInquiries(list);
        });
        return () => unsub();
    }, [user]);

    const handleGoogleSignIn = async () => {
        const provider = new GoogleAuthProvider();
        try {
            await signInWithPopup(auth, provider);
        } catch (error) {
            console.error("Lỗi đăng nhập:", error);
            alert("Lỗi đăng nhập Google: " + error.message);
        }
    };

    const handleSignOut = () => {
        signOut(auth);
    };

    const fetchLandlordData = async (uid) => {
        setLoadingData(true);
        try {
            const basePath = `users/${uid}`;

            const [roomSnap, tenantSnap, contractSnap, billSnap, paymentSnap] = await Promise.all([
                getDocs(collection(db, `${basePath}/rooms`)),
                getDocs(collection(db, `${basePath}/tenants`)),
                getDocs(collection(db, `${basePath}/contracts`)),
                getDocs(collection(db, `${basePath}/bills`)),
                getDocs(collection(db, `${basePath}/payments`)),
            ]);

            const parsedRooms = roomSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            const parsedTenants = tenantSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            const parsedContracts = contractSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
            const paymentsByBill = {};
            paymentSnap.docs.forEach(doc => {
                const payment = doc.data();
                const billId = String(payment.billId ?? '');
                if (!billId) return;
                paymentsByBill[billId] = (paymentsByBill[billId] || 0) + (payment.amount || 0);
            });

            const parsedBills = billSnap.docs.map(doc => {
                const data = doc.data();
                const paidAmount = paymentsByBill[String(data.billId ?? doc.id)] || 0;
                return { id: doc.id, ...data, paidAmount };
            });

            setRoomsList(parsedRooms);
            setTenantsList(parsedTenants);
            setContractsList(parsedContracts);
            setUnpaidBills(parsedBills.filter(b => b.paymentStatus !== 'DA_THANH_TOAN'));

            // Calculate Revenue Data for Chart
            const paidBills = parsedBills.filter(b => b.paymentStatus === 'DA_THANH_TOAN');
            const revMap = {};
            paidBills.forEach(b => {
                const m = b.month; // e.g. "2026-04"
                if (!revMap[m]) revMap[m] = 0;
                revMap[m] += b.totalAmount || 0;
            });

            const sortedMonths = Object.keys(revMap).sort();
            const revChartData = sortedMonths.map(m => ({
                name: m,
                total: revMap[m]
            })).slice(-6); // Lấy 6 tháng gần nhất

            setRevenueData(revChartData);

            setStats({
                rooms: roomSnap.size,
                tenants: tenantSnap.size,
                contracts: contractSnap.size,
                bills: billSnap.size
            });

            // Fetch Inquiries + Settings
            try {
                const [inqSnap, sDoc] = await Promise.all([
                    getDocs(collection(db, `inquiries/${uid}/requests`)),
                    import('firebase/firestore').then(({ doc: fdoc, getDoc: fgetDoc }) =>
                        fgetDoc(fdoc(db, `users/${uid}/settings/config`))
                    ),
                ]);
                if (sDoc.exists()) setSettingData(sDoc.data());
                setInquiries(inqSnap.docs.map(d => ({ id: d.id, ...d.data() })).sort((a, b) => {
                    const ta = a.createdAt?.toMillis?.() || 0;
                    const tb = b.createdAt?.toMillis?.() || 0;
                    return tb - ta;
                }));
            } catch (_) { /* Chưa có yêu cầu nào là OK */ }

        } catch (error) {
            console.error("Lỗi tải dữ liệu:", error);
        } finally {
            setLoadingData(false);
        }
    };

    const nf = new Intl.NumberFormat('vi-VN');

    const exportBillsToExcel = () => {
        const roomMap = {};
        roomsList.forEach(r => roomMap[r.id] = r.roomName || r.id);

        const tenantMap = {};
        tenantsList.forEach(t => tenantMap[t.id] = { name: t.fullName, phone: t.phone });

        const getContractTenant = (contractId) => {
            const c = contractsList.find(c => String(c.contractId) === String(contractId) || c.id === String(contractId));
            if (!c) return { name: '', phone: '' };
            const t = tenantMap[String(c.tenantId)];
            const room = roomMap[String(c.roomId)] || '';
            return { name: t?.name || '', phone: t?.phone || '', room };
        };

        const nfMoney = new Intl.NumberFormat('vi-VN');
        const billsToExport = exportMonth === 'ALL' ? unpaidBills : unpaidBills.filter(b => b.month === exportMonth);
        const data = billsToExport.map(b => {
            const tenant = getContractTenant(b.contractId);
            const total = b.totalAmount || 0;
            const paid = b.paidAmount || 0;
            const remaining = total - paid;
            const status = b.paymentStatus === 'DONG_THIEU' ? 'Đóng thiếu' : 'Chưa trả';
            return {
                'Tháng': b.month || '',
                'Phòng': tenant.room || b.roomName || '',
                'Khách thuê': tenant.name || '',
                'SĐT': tenant.phone || '',
                'Tiền phòng (đ)': nfMoney.format(b.rentPrice || 0),
                'Tổng hoá đơn (đ)': nfMoney.format(total),
                'Đã trả (đ)': nfMoney.format(paid),
                'Còn nợ (đ)': nfMoney.format(remaining),
                'Tình trạng': status,
                'Hạn thanh toán': b.dueDate ? new Date(b.dueDate).toLocaleDateString('vi-VN') : '',
            };
        });

        const ws = XLSX.utils.json_to_sheet(data);
        // Set column widths
        ws['!cols'] = [
            { wch: 10 }, // Tháng
            { wch: 14 }, // Phòng
            { wch: 22 }, // Khách thuê
            { wch: 14 }, // SĐT
            { wch: 16 }, // Tiền phòng
            { wch: 18 }, // Tổng hoá đơn
            { wch: 14 }, // Đã trả
            { wch: 14 }, // Còn nợ
            { wch: 12 }, // Tình trạng
            { wch: 16 }, // Hạn TT
        ];

        const wb = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(wb, ws, 'Hóa đơn nợ');
        XLSX.writeFile(wb, `hoa-don-no-${exportMonth === 'ALL' ? 'tat-ca' : exportMonth}.xlsx`);
    };

    const vi2ascii = (str) => (str || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/\u0111/g, 'd').replace(/\u0110/g, 'D')
        .replace(/\u01b0/g, 'u').replace(/\u01af/g, 'U')
        .replace(/\u01a1/g, 'o').replace(/\u01a0/g, 'O')
        .replace(/\u1ea1/g, 'a').replace(/\u1eb9/g, 'e')
        .replace(/[^\x00-\x7F]/g, '?');

    const exportBillsToPDF = async () => {
        const { default: jsPDF } = await import('jspdf');
        const { default: autoTable } = await import('jspdf-autotable');

        const doc = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4' });

        // Header
        doc.setFillColor(79, 70, 229);
        doc.rect(0, 0, 297, 22, 'F');
        doc.setTextColor(255, 255, 255);
        doc.setFontSize(14);
        doc.setFont('helvetica', 'bold');
        doc.text('BAO CAO HOA DON CON NO', 14, 10);
        doc.setFontSize(9);
        doc.setFont('helvetica', 'normal');
        const monthLabel = exportMonth === 'ALL' ? 'Tat ca thang' : `Thang ${exportMonth}`;
        doc.text(`${monthLabel}  |  Xuat ngay: ${new Date().toLocaleDateString('vi-VN')}`, 14, 17);

        // Summary row
        const billsToExport = exportMonth === 'ALL' ? unpaidBills : unpaidBills.filter(b => b.month === exportMonth);
        const totalDebt = billsToExport.reduce((s, b) => s + (b.totalAmount || 0), 0);
        doc.setTextColor(30, 30, 30);
        doc.setFontSize(10);
        doc.setFont('helvetica', 'bold');
        doc.text(`Tong so hoa don: ${billsToExport.length}   |   Tong no: ${new Intl.NumberFormat('vi-VN').format(totalDebt)}d`, 14, 30);

        // Build table rows
        const roomMap = {};
        roomsList.forEach(r => roomMap[r.id] = r.roomName || r.id);
        const tenantMap = {};
        tenantsList.forEach(t => tenantMap[t.id] = t.fullName || '');

        const rows = billsToExport.map((b, idx) => {
            const contract = contractsList.find(c => String(c.contractId) === String(b.contractId) || c.id === String(b.contractId));
            const room = contract ? (roomMap[String(contract.roomId)] || '—') : '—';
            const tenant = contract ? (tenantMap[String(contract.tenantId)] || '—') : '—';
            const status = b.paymentStatus === 'DONG_THIEU' ? 'Dong thieu' : 'Chua tra';
            const nf = new Intl.NumberFormat('vi-VN');
            return [
                idx + 1,
                b.month || '',
                vi2ascii(room),
                vi2ascii(tenant),
                nf.format(b.rentPrice || 0) + 'd',
                nf.format(b.totalAmount || 0) + 'd',
                nf.format((b.paidAmount || 0)) + 'd',
                nf.format((b.totalAmount || 0) - (b.paidAmount || 0)) + 'd',
                status,
            ];
        });

        autoTable(doc, {
            startY: 35,
            head: [['#', 'Thang', 'Phong', 'Khach thue', 'Tien phong', 'Tong HD', 'Da tra', 'Con no', 'Tinh trang']],
            body: rows,
            theme: 'grid',
            headStyles: { fillColor: [79, 70, 229], textColor: 255, fontStyle: 'bold', fontSize: 9 },
            bodyStyles: { fontSize: 8, textColor: [30, 30, 30] },
            alternateRowStyles: { fillColor: [248, 250, 252] },
            columnStyles: {
                0: { cellWidth: 8, halign: 'center' },
                1: { cellWidth: 20 },
                2: { cellWidth: 28 },
                3: { cellWidth: 38 },
                4: { cellWidth: 28, halign: 'right' },
                5: { cellWidth: 28, halign: 'right' },
                6: { cellWidth: 24, halign: 'right' },
                7: { cellWidth: 28, halign: 'right', textColor: [220, 38, 38] },
                8: { cellWidth: 24, halign: 'center' },
            },
            didParseCell: (data) => {
                if (data.column.index === 8 && data.section === 'body') {
                    const val = String(data.cell.raw || '');
                    data.cell.styles.textColor = val.includes('thieu') ? [234, 88, 12] : [220, 38, 38];
                    data.cell.styles.fontStyle = 'bold';
                }
            },
        });

        // Footer
        const pageCount = doc.internal.getNumberOfPages();
        for (let i = 1; i <= pageCount; i++) {
            doc.setPage(i);
            doc.setFontSize(7);
            doc.setTextColor(150);
            doc.text(`Trang ${i}/${pageCount}  |  Rental Manager`, 14, doc.internal.pageSize.height - 5);
        }

        const suffix = exportMonth === 'ALL' ? 'tat-ca' : exportMonth;
        doc.save(`bao-cao-no-${suffix}.pdf`);
    };

    if (loadingAuth) {
        return (
            <div className="min-h-screen bg-slate-50 flex items-center justify-center">
                <div className="w-10 h-10 border-4 border-indigo-500/30 border-t-indigo-500 rounded-full animate-spin"></div>
            </div>
        );
    }

    if (!user) {
        return (
            <div className="min-h-screen bg-slate-50 flex items-center justify-center px-4">
                <div className="w-full max-w-md bg-white p-10 rounded-2xl border border-slate-200 text-center shadow-2xl shadow-indigo-100">
                    <div className="w-16 h-16 bg-indigo-50 text-indigo-600 rounded-2xl flex items-center justify-center mx-auto mb-6">
                        <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                    </div>
                    <h1 className="text-2xl font-bold text-slate-900 mb-2 tracking-tight">Admin Chủ Trọ</h1>
                    <p className="text-slate-500 mb-8 text-sm">Đăng nhập tài khoản Google để Quản lý Dữ liệu hệ sinh thái của bạn.</p>

                    <button
                        onClick={handleGoogleSignIn}
                        className="w-full py-3 bg-white border border-slate-200 text-slate-700 font-semibold rounded-xl flex items-center justify-center gap-3 hover:bg-slate-50 hover:border-slate-300 transition-all shadow-sm"
                    >
                        <svg className="w-5 h-5" viewBox="0 0 24 24">
                            <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                            <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                            <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                            <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                        </svg>
                        Đăng nhập bằng Google
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col font-sans">
            {/* Header */}
            <header className="bg-white border-b border-slate-200 px-4 sm:px-8 py-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 z-10 sticky top-0 shadow-sm">
                <div className="flex items-center gap-3 w-full sm:w-auto justify-between sm:justify-start">
                    <div className="bg-indigo-600 p-2 rounded-lg text-white">
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" /></svg>
                    </div>
                    <div>
                        <h1 className="font-bold text-xl text-slate-800 tracking-tight">Rental Manager</h1>
                        <p className="text-xs text-slate-500 font-medium">Logged in as {user.email}</p>
                    </div>
                </div>
                <div className="flex items-center gap-4 w-full sm:w-auto justify-between sm:justify-start overflow-x-auto pb-1 sm:pb-0">
                    <div className="flex bg-slate-100 border border-slate-200 p-1 rounded-xl gap-1 shrink-0">
                        <button
                            onClick={() => setActiveTab('dashboard')}
                            className={`px-4 py-1.5 rounded-lg text-sm font-semibold transition-all ${activeTab === 'dashboard' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                        >
                            Dashboard
                        </button>
                        <button
                            onClick={() => setActiveTab('inquiries')}
                            className={`px-4 py-1.5 rounded-lg text-sm font-semibold transition-all flex items-center gap-1.5 ${activeTab === 'inquiries' ? 'bg-white shadow text-slate-800' : 'text-slate-500 hover:text-slate-700'}`}
                        >
                            Yêu cầu thuê
                            {inquiries.filter(i => !readIds.has(i.id)).length > 0 && <span className="bg-red-500 text-white text-[10px] font-bold px-1.5 py-0.5 rounded-full shrink-0">{inquiries.filter(i => !readIds.has(i.id)).length}</span>}
                        </button>
                    </div>
                    <button onClick={handleSignOut} className="px-5 py-2 bg-white border border-slate-200 text-slate-600 rounded-lg text-sm font-semibold hover:bg-slate-50 hover:border-slate-300 transition-colors shadow-sm shrink-0">
                        Đăng xuất
                    </button>
                </div>
            </header >

            <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-8">
                {activeTab === 'inquiries' ? (
                    <div>
                        <div className="mb-8 flex flex-col sm:flex-row justify-between items-start sm:items-end gap-3">
                            <div>
                                <h2 className="text-3xl font-bold text-slate-900 tracking-tight mb-1">Yêu cầu Xem Phòng</h2>
                                <p className="text-slate-500 text-sm">{inquiries.length} yêu cầu từ khách vãng lai.</p>
                            </div>
                            <button onClick={() => fetchLandlordData(user.uid)} className="px-4 py-2 bg-indigo-50 text-indigo-700 rounded-lg text-sm font-semibold hover:bg-indigo-100 transition-colors flex items-center gap-2">
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
                                Làm mới
                            </button>
                        </div>

                        {inquiries.length === 0 ? (
                            <div className="bg-white border border-slate-200 rounded-2xl p-16 text-center shadow-sm">
                                <div className="text-5xl mb-4">📭</div>
                                <h3 className="text-xl font-bold text-slate-700 mb-2">Chưa có yêu cầu nào</h3>
                                <p className="text-slate-400 text-sm">Khi khách hàng điền form liên hệ từ trang phòng trống, yêu cầu sẽ hiện ở đây.</p>
                            </div>
                        ) : (
                            <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
                                {inquiries.map(inq => {
                                    const isRead = readIds.has(inq.id);
                                    const markRead = () => {
                                        const next = new Set(readIds).add(inq.id);
                                        setReadIds(next);
                                        try { localStorage.setItem('readInquiryIds', JSON.stringify([...next])); } catch { }
                                    };
                                    return (
                                        <div key={inq.id} className={`bg-white border rounded-2xl p-5 shadow-sm hover:shadow-md transition-shadow ${isRead ? 'border-slate-200 opacity-80' : 'border-indigo-300 ring-1 ring-indigo-100'}`}>
                                            <div className="flex justify-between items-start mb-3">
                                                <div>
                                                    <p className="font-bold text-slate-800 text-lg">{inq.name}</p>
                                                    <a href={`tel:${inq.phone}`} className="text-indigo-600 font-mono font-semibold text-sm hover:underline flex items-center gap-1 mt-0.5">
                                                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>
                                                        {inq.phone}
                                                    </a>
                                                </div>
                                                {isRead
                                                    ? <span className="bg-slate-100 text-slate-400 text-[10px] font-bold px-2 py-1 rounded-full border border-slate-200">ĐÃ ĐỌC</span>
                                                    : <button onClick={markRead} className="bg-sky-50 text-sky-600 text-[10px] font-bold px-2 py-1 rounded-full border border-sky-200 hover:bg-sky-100 transition-colors">MỚI · Đánh dấu đọc</button>
                                                }
                                            </div>
                                            <div className="bg-slate-50 border border-slate-200 rounded-xl p-3 mb-3">
                                                <div className="text-[10px] text-slate-400 font-bold tracking-wider uppercase mb-1">Phòng quan tâm</div>
                                                <div className="text-sm font-semibold text-slate-700">{inq.roomName}</div>
                                            </div>
                                            {inq.note && (
                                                <p className="text-sm text-slate-500 italic">&ldquo;{inq.note}&rdquo;</p>
                                            )}
                                            <div className="mt-3 text-[10px] text-slate-400">
                                                {inq.createdAt?.toDate?.()?.toLocaleString?.('vi-VN') || 'N/A'}
                                            </div>

                                            {/* AI Reply Button */}
                                            <div className="mt-4 pt-4 border-t border-slate-100">
                                                <button
                                                    onClick={async () => {
                                                        setAiReplyLoading(prev => ({ ...prev, [inq.id]: true }));
                                                        try {
                                                            const text = await callGemini('reply_suggestion', {
                                                                inquiry: inq,
                                                                room: roomsList.find(r => r.id === inq.roomId),
                                                                hostelName: settingData.hostelName,
                                                                landlordName: settingData.landlordName,
                                                            });
                                                            setAiReplies(prev => ({ ...prev, [inq.id]: text }));
                                                            markRead();
                                                        } catch (e) {
                                                            setAiReplies(prev => ({ ...prev, [inq.id]: 'Lỗi: ' + e.message }));
                                                        } finally {
                                                            setAiReplyLoading(prev => ({ ...prev, [inq.id]: false }));
                                                        }
                                                    }}
                                                    disabled={aiReplyLoading[inq.id]}
                                                    className="w-full py-2 bg-violet-50 hover:bg-violet-100 border border-violet-200 text-violet-700 rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1.5 disabled:opacity-60"
                                                >
                                                    {aiReplyLoading[inq.id] ? (
                                                        <><div className="w-3 h-3 border-2 border-violet-300 border-t-violet-600 rounded-full animate-spin" /> Đang soạn...</>
                                                    ) : (
                                                        <>✨ AI gợi ý trả lời</>
                                                    )}
                                                </button>
                                                {aiReplies[inq.id] && (
                                                    <div className="mt-2 bg-violet-50 border border-violet-200 rounded-xl p-3">
                                                        <p className="text-xs text-slate-600 leading-relaxed whitespace-pre-wrap">{aiReplies[inq.id]}</p>
                                                        <button
                                                            onClick={() => navigator.clipboard.writeText(aiReplies[inq.id])}
                                                            className="mt-2 text-[10px] text-violet-600 font-semibold hover:underline flex items-center gap-1"
                                                        >
                                                            📋 Copy tin nhắn
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                ) : (
                    <>
                        <div className="mb-8 flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
                            <div>
                                <h2 className="text-3xl font-bold text-slate-900 tracking-tight mb-1">Tổng quan Dashboard</h2>
                                <p className="text-slate-500 text-sm">Dữ liệu kinh doanh được báo cáo thời gian thực.</p>
                            </div>
                            <div className="flex gap-2 flex-wrap">
                                <select
                                    value={exportMonth}
                                    onChange={e => setExportMonth(e.target.value)}
                                    className="text-xs border border-emerald-200 rounded-lg px-2 py-2 bg-white text-emerald-700 focus:outline-none focus:ring-1 focus:ring-emerald-400"
                                >
                                    <option value="ALL">Xuất tất cả</option>
                                    {[...new Set(unpaidBills.map(b => b.month))].sort().reverse().map(m => (
                                        <option key={m} value={m}>Tháng {m}</option>
                                    ))}
                                </select>
                                <button onClick={exportBillsToExcel} className="px-4 py-2 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded-lg text-sm font-semibold hover:bg-emerald-100 transition-colors flex items-center gap-2">
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                                    Xuất Excel
                                </button>
                                <button onClick={exportBillsToPDF} className="px-4 py-2 bg-rose-50 text-rose-700 border border-rose-200 rounded-lg text-sm font-semibold hover:bg-rose-100 transition-colors flex items-center gap-2">
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 21h10a2 2 0 002-2V9.414a1 1 0 00-.293-.707l-5.414-5.414A1 1 0 0012.586 3H7a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
                                    Xuất PDF
                                </button>
                                <button onClick={() => fetchLandlordData(user.uid)} className="px-4 py-2 bg-indigo-50 text-indigo-700 rounded-lg text-sm font-semibold hover:bg-indigo-100 transition-colors flex items-center gap-2">
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" /></svg>
                                    Làm mới
                                </button>
                            </div>
                        </div>

                        {loadingData ? (
                            <div className="flex justify-center py-20">
                                <div className="w-8 h-8 border-4 border-indigo-500/30 border-t-indigo-600 rounded-full animate-spin"></div>
                            </div>
                        ) : (
                            <>
                                {/* Stats Cards */}
                                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                                    {[
                                        { label: 'TỔNG SỐ PHÒNG', val: stats.rooms, icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6', color: 'indigo' },
                                        { label: 'KHÁCH THUÊ', val: stats.tenants, icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z', color: 'sky' },
                                        { label: 'HỢP ĐỒNG', val: stats.contracts, icon: 'M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z', color: 'emerald' },
                                        { label: 'HOÁ ĐƠN', val: stats.bills, icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z', color: 'rose' }
                                    ].map(item => (
                                        <div key={item.label} className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm flex items-center gap-5 hover:shadow-md transition-shadow">
                                            <div className={`p-4 rounded-xl bg-slate-50 text-slate-700`}>
                                                <svg className="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d={item.icon} /></svg>
                                            </div>
                                            <div>
                                                <div className="text-[11px] text-slate-400 font-bold uppercase tracking-wider mb-1">{item.label}</div>
                                                <div className="text-3xl font-extrabold text-slate-800">{item.val}</div>
                                            </div>
                                        </div>
                                    ))}
                                </div>

                                {/* Chart & Summary Row */}
                                <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mb-8">
                                    <div className="lg:col-span-2 bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
                                        <h3 className="text-lg font-bold text-slate-800 mb-6">Biểu đồ doanh thu (Đã thu)</h3>
                                        <div className="h-72 w-full">
                                            {revenueData.length > 0 ? (
                                                <ResponsiveContainer width="100%" height="100%">
                                                    <BarChart data={revenueData} margin={{ top: 10, right: 10, left: 20, bottom: 0 }}>
                                                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e2e8f0" />
                                                        <XAxis dataKey="name" axisLine={false} tickLine={false} tick={{ fill: '#64748b', fontSize: 12 }} dy={10} />
                                                        <YAxis
                                                            axisLine={false}
                                                            tickLine={false}
                                                            tick={{ fill: '#64748b', fontSize: 12 }}
                                                            tickFormatter={(value) => `${value / 1000000}M`}
                                                        />
                                                        <Tooltip
                                                            cursor={{ fill: '#f8fafc' }}
                                                            contentStyle={{ borderRadius: '8px', border: '1px solid #e2e8f0', boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)' }}
                                                            formatter={(value) => [`${nf.format(value)} đ`, 'Doanh thu']}
                                                        />
                                                        <Bar dataKey="total" fill="#6366f1" radius={[4, 4, 0, 0]} maxBarSize={50} />
                                                    </BarChart>
                                                </ResponsiveContainer>
                                            ) : (
                                                <div className="h-full flex items-center justify-center text-slate-400 italic">Chưa có dữ liệu doanh thu do chưa thanh toán hoá đơn nào.</div>
                                            )}
                                        </div>
                                    </div>

                                    {/* Attention Widget */}
                                    <div className="bg-gradient-to-br from-indigo-600 to-indigo-800 rounded-xl p-6 shadow-md text-white flex flex-col">
                                        <h3 className="text-lg font-bold mb-2">Tình hình nợ cước</h3>
                                        <p className="text-indigo-200 text-sm mb-6">
                                            Có {currentMonthUnpaidBills.length} hoá đơn chưa được thanh toán thành công trong tháng {currentMonthKey}.
                                        </p>

                                        <div className="mt-auto bg-white/10 p-4 rounded-lg backdrop-blur-sm border border-white/20">
                                            <div className="text-indigo-200 text-xs font-bold uppercase tracking-wider mb-1">Tổng tiền tồn đọng</div>
                                            <div className="text-3xl font-black font-mono">
                                                {nf.format(currentMonthOutstanding)}đ
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                {/* AI Revenue Analysis Card */}
                                <div className="bg-gradient-to-br from-violet-50 to-indigo-50 border border-violet-200 rounded-xl p-6 mb-8 shadow-sm">
                                    <div className="flex justify-between items-start">
                                        <div className="flex items-center gap-3">
                                            <div className="bg-violet-600 p-2 rounded-lg">
                                                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9.663 17h4.673M12 3v1m6.364 1.636l-.707.707M21 12h-1M4 12H3m3.343-5.657l-.707-.707m2.828 9.9a5 5 0 117.072 0l-.347.347a3.5 3.5 0 01-4.95 4.95l-.347-.347a5 5 0 010-7.072z" /></svg>
                                            </div>
                                            <div>
                                                <h3 className="font-bold text-violet-900 text-base">AI Phân tích Doanh thu</h3>
                                            </div>
                                        </div>
                                        <button
                                            onClick={async () => {
                                                setAiAnalysisLoading(true);
                                                setAiAnalysis('');
                                                try {
                                                    const text = await callGemini('revenue_analysis', {
                                                        revenueData,
                                                        unpaidBills,
                                                        stats,
                                                        tenantsList,
                                                        currentMonthKey,
                                                        currentMonthUnpaidBills,
                                                        currentMonthOutstanding,
                                                    });
                                                    setAiAnalysis(text);
                                                } catch (e) {
                                                    setAiAnalysis('Lỗi: ' + e.message);
                                                } finally {
                                                    setAiAnalysisLoading(false);
                                                }
                                            }}
                                            disabled={aiAnalysisLoading}
                                            className="px-4 py-2 bg-violet-600 hover:bg-violet-700 disabled:opacity-60 text-white rounded-lg text-sm font-semibold transition-colors flex items-center gap-2 flex-shrink-0"
                                        >
                                            {aiAnalysisLoading ? (
                                                <><div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> Đang phân tích...</>
                                            ) : (
                                                <>✨ Phân tích ngay</>
                                            )}
                                        </button>
                                    </div>
                                    {aiAnalysis && (
                                        <div className="mt-4 bg-white/70 border border-violet-200 rounded-xl p-4 text-slate-700 text-sm leading-relaxed">
                                            {aiAnalysis}
                                        </div>
                                    )}
                                </div>

                                {/* Four Split Data Tables */}
                                <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
                                    {/* Rooms Table */}
                                    <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden flex flex-col max-h-[500px]">
                                        <div className="p-5 border-b border-slate-200 flex justify-between items-center bg-slate-50/50">
                                            <h3 className="font-bold text-slate-800">Quản Lý Phòng</h3>
                                            <span className="bg-slate-100 text-slate-600 text-xs font-semibold px-2.5 py-1 rounded-full">{roomsList.length} phòng</span>
                                        </div>
                                        <div className="overflow-x-auto flex-1 custom-scrollbar">
                                            <table className="w-full text-left text-sm">
                                                <thead className="bg-slate-50 text-slate-500 text-xs font-semibold uppercase sticky top-0 shadow-sm z-10">
                                                    <tr>
                                                        <th className="px-5 py-3 font-semibold">Tên phòng</th>
                                                        <th className="px-5 py-3 font-semibold">Giá thuê</th>
                                                        <th className="px-5 py-3 font-semibold text-right">Trạng thái</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-100">
                                                    {roomsList.map(r => (
                                                        <tr key={r.id} className="hover:bg-slate-50">
                                                            <td className="px-5 py-3 font-semibold text-slate-800">{r.roomName}</td>
                                                            <td className="px-5 py-3 font-mono text-slate-600">{nf.format(r.price)}đ</td>
                                                            <td className="px-5 py-3 text-right">
                                                                <span className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold ${r.status === 'TRONG' ? 'bg-emerald-50 text-emerald-600 border border-emerald-200' : 'bg-slate-100 text-slate-600 border border-slate-200'}`}>
                                                                    {r.status === 'TRONG' ? 'Trống' : 'Đang thuê'}
                                                                </span>
                                                            </td>
                                                        </tr>
                                                    ))}
                                                    {roomsList.length === 0 && <tr><td colSpan="3" className="text-center py-6 text-slate-400">Trống</td></tr>}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>

                                    {/* Unpaid Bills Table */}
                                    <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden flex flex-col max-h-[500px]">
                                        <div className="p-5 border-b border-slate-200 flex justify-between items-center bg-slate-50/50">
                                            <h3 className="font-bold text-slate-800">Cần Thu Tiền</h3>
                                            <div className="flex items-center gap-2">
                                                <select
                                                    value={billMonthFilter}
                                                    onChange={e => setBillMonthFilter(e.target.value)}
                                                    className="text-xs border border-slate-200 rounded-lg px-2 py-1.5 bg-white text-slate-600 focus:outline-none focus:ring-1 focus:ring-indigo-500"
                                                >
                                                    <option value="ALL">Tất cả tháng</option>
                                                    {[...new Set(unpaidBills.map(b => b.month))].sort().reverse().map(m => (
                                                        <option key={m} value={m}>{m}</option>
                                                    ))}
                                                </select>
                                                <span className="bg-red-50 text-red-600 text-xs font-semibold px-2.5 py-1 rounded-full border border-red-100">
                                                    {(billMonthFilter === 'ALL' ? unpaidBills : unpaidBills.filter(b => b.month === billMonthFilter)).length} h.đơn
                                                </span>
                                            </div>
                                        </div>
                                        <div className="overflow-x-auto flex-1 custom-scrollbar">
                                            <table className="w-full text-left text-sm">
                                                <thead className="bg-slate-50 text-slate-500 text-xs font-semibold uppercase sticky top-0 shadow-sm z-10">
                                                    <tr>
                                                        <th className="px-4 py-3 font-semibold">Tháng</th>
                                                        <th className="px-4 py-3 font-semibold">Phòng</th>
                                                        <th className="px-4 py-3 font-semibold">Khách thuê</th>
                                                        <th className="px-4 py-3 font-semibold">Tổng nợ</th>
                                                        <th className="px-4 py-3 font-semibold text-right">Tình trạng</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-100">
                                                    {(billMonthFilter === 'ALL' ? unpaidBills : unpaidBills.filter(b => b.month === billMonthFilter)).map(b => {
                                                        // Lookup phòng và khách thuê qua hợp đồng
                                                        const contract = contractsList.find(c => String(c.contractId) === String(b.contractId) || c.id === String(b.contractId));
                                                        const room = contract ? (roomsList.find(r => String(r.id) === String(contract.roomId))?.roomName || contract.roomId || '—') : '—';
                                                        const tenant = contract ? (tenantsList.find(t => String(t.id) === String(contract.tenantId))?.fullName || '—') : '—';
                                                        return (
                                                            <tr key={b.id} className="hover:bg-slate-50">
                                                                <td className="px-4 py-3 font-bold text-slate-700 text-xs">{b.month}</td>
                                                                <td className="px-4 py-3 font-semibold text-slate-800 text-sm">{room}</td>
                                                                <td className="px-4 py-3 text-slate-600 text-sm">{tenant}</td>
                                                                <td className="px-4 py-3 font-mono font-bold text-red-600">{nf.format(b.totalAmount)}đ</td>
                                                                <td className="px-4 py-3 text-right">
                                                                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold ${b.paymentStatus === 'DONG_THIEU' ? 'bg-orange-50 text-orange-600 border border-orange-200' : 'bg-red-50 text-red-600 border border-red-200'}`}>
                                                                        {b.paymentStatus === 'DONG_THIEU' ? 'ĐÓNG THIẾU' : 'CHƯA TRẢ'}
                                                                    </span>
                                                                </td>
                                                            </tr>
                                                        );
                                                    })}
                                                    {unpaidBills.length === 0 && <tr><td colSpan="5" className="text-center py-6 text-slate-400">Mọi hoá đơn đã thu xịn!</td></tr>}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>

                                    {/* Tenants Table */}
                                    <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden flex flex-col max-h-[500px]">
                                        <div className="p-5 border-b border-slate-200 flex justify-between items-center bg-slate-50/50">
                                            <h3 className="font-bold text-slate-800">Danh Sách Khách Thuê</h3>
                                        </div>
                                        <div className="overflow-x-auto flex-1 custom-scrollbar">
                                            <table className="w-full text-left text-sm">
                                                <thead className="bg-slate-50 text-slate-500 text-xs font-semibold uppercase sticky top-0 shadow-sm z-10">
                                                    <tr>
                                                        <th className="px-5 py-3 font-semibold">Khách thuê</th>
                                                        <th className="px-5 py-3 font-semibold">Căn cước</th>
                                                        <th className="px-5 py-3 font-semibold">Điện thoại</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-100">
                                                    {tenantsList.map(t => (
                                                        <tr key={t.id} className="hover:bg-slate-50">
                                                            <td className="px-5 py-3 font-semibold text-slate-800">{t.fullName}</td>
                                                            <td className="px-5 py-3 text-slate-500 font-mono text-xs">{t.cccd}</td>
                                                            <td className="px-5 py-3 text-slate-600">{t.phone}</td>
                                                        </tr>
                                                    ))}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>

                                    {/* Contracts Table */}
                                    <div className="bg-white border border-slate-200 rounded-xl shadow-sm overflow-hidden flex flex-col max-h-[500px]">
                                        <div className="p-5 border-b border-slate-200 flex justify-between items-center bg-slate-50/50">
                                            <h3 className="font-bold text-slate-800">Hợp Đồng</h3>
                                        </div>
                                        <div className="overflow-x-auto flex-1 custom-scrollbar">
                                            <table className="w-full text-left text-sm">
                                                <thead className="bg-slate-50 text-slate-500 text-xs font-semibold uppercase sticky top-0 shadow-sm z-10">
                                                    <tr>
                                                        <th className="px-5 py-3 font-semibold">Phòng</th>
                                                        <th className="px-5 py-3 font-semibold">Tiền cọc</th>
                                                        <th className="px-5 py-3 font-semibold text-right">Trạng thái</th>
                                                    </tr>
                                                </thead>
                                                <tbody className="divide-y divide-slate-100">
                                                    {contractsList.map(c => {
                                                        const room = roomsList.find(r => r.roomId === c.roomId);
                                                        return (
                                                            <tr key={c.id} className="hover:bg-slate-50">
                                                                <td className="px-5 py-3 font-semibold text-slate-800">{room ? room.roomName : 'Phòng #' + c.roomId}</td>
                                                                <td className="px-5 py-3 text-slate-600 font-mono">{nf.format(c.deposit)}đ</td>
                                                                <td className="px-5 py-3 text-right">
                                                                    <span className={`inline-flex items-center px-2 py-0.5 rounded text-[10px] font-bold ${c.status === 'HIEU_LUC' ? 'bg-indigo-50 text-indigo-600 border border-indigo-200' : 'bg-slate-100 text-slate-500 border border-slate-200'}`}>
                                                                        {c.status === 'HIEU_LUC' ? 'HIỆU LỰC' : 'ĐÃ KẾT THÚC'}
                                                                    </span>
                                                                </td>
                                                            </tr>
                                                        )
                                                    })}
                                                </tbody>
                                            </table>
                                        </div>
                                    </div>

                                </div>
                            </>
                        )}
                    </>
                )}
            </main>
        </div >
    );
}
