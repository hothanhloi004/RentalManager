'use client';
import { useState } from 'react';
import { db } from '../../lib/firebase';
import { collectionGroup, getDocs, doc, getDoc, collection } from 'firebase/firestore';

function formatMoney(n) { return new Intl.NumberFormat('vi-VN').format(n || 0); }

function VietQRImage({ bankCode, bankAccount, amount, content }) {
    const qrUrl = `https://img.vietqr.io/image/${bankCode}-${bankAccount}-compact2.png?amount=${amount}&addInfo=${encodeURIComponent(content)}&accountName=Chu+Nha`;
    return (
        <div className="flex flex-col items-center justify-center p-4 bg-white border border-slate-200 rounded-2xl shadow-sm">
            <img src={qrUrl} alt="VietQR" className="w-56 h-56 object-contain rounded-xl" />
            <p className="text-xs text-slate-500 mt-2 text-center">Quét mã để chuyển khoản ngay</p>
            <p className="text-xs text-indigo-600 font-semibold mt-1">{formatMoney(amount)}đ – {content}</p>
            <p className="text-[10px] text-amber-600 font-medium mt-3 bg-amber-50 px-3 py-1.5 rounded-lg border border-amber-100 text-center">
                ⚠️ Sau khi chuyển khoản, vui lòng chờ chủ nhà xác nhận trên ứng dụng để cập nhật trạng thái hóa đơn.
            </p>
        </div>
    );
}

function BillCard({ bill, settings }) {
    const [showQR, setShowQR] = useState(false);
    const status = bill.paymentStatus;
    const isFullyPaid = status === 'DA_THANH_TOAN';
    const isPartial = status === 'DONG_THIEU';
    const label = isFullyPaid ? 'Đã thanh toán' : isPartial ? 'Đóng thiếu' : 'Chưa trả';
    const labelClass = isFullyPaid ? 'bg-emerald-50 text-emerald-700 border-emerald-200' : isPartial ? 'bg-orange-50 text-orange-600 border-orange-200' : 'bg-red-50 text-red-600 border-red-200';
    const remaining = (bill.totalAmount || 0) - (bill.paidAmount || 0);

    return (
        <div className={`bg-white border rounded-2xl shadow-sm overflow-hidden transition-all ${isFullyPaid ? 'border-slate-200 opacity-80' : 'border-indigo-200'}`}>
            {/* Header */}
            <div className="px-6 py-5 flex justify-between items-start border-b border-slate-100">
                <div>
                    <div className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                        <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                        Tháng {bill.month}
                    </div>
                    <div className="font-bold text-slate-800 text-lg">{bill.roomName || 'Phòng của bạn'}</div>
                </div>
                <span className={`text-[11px] font-bold px-3 py-1 rounded-full border ${labelClass}`}>{label}</span>
            </div>

            {/* Breakdown */}
            <div className="px-6 py-4 space-y-2">
                {bill.rentAmount > 0 && <div className="flex justify-between text-sm text-slate-600"><span>Tiền phòng</span><span className="font-semibold">{formatMoney(bill.rentAmount)}đ</span></div>}
                {bill.electricAmount > 0 && <div className="flex justify-between text-sm text-slate-600"><span>Tiền điện ({bill.electricUsed || '?'} số)</span><span className="font-semibold">{formatMoney(bill.electricAmount)}đ</span></div>}
                {bill.waterAmount > 0 && <div className="flex justify-between text-sm text-slate-600"><span>Tiền nước ({bill.waterUsed || '?'} m³)</span><span className="font-semibold">{formatMoney(bill.waterAmount)}đ</span></div>}
                {bill.wifiAmount > 0 && <div className="flex justify-between text-sm text-slate-600"><span>Wi-Fi</span><span className="font-semibold">{formatMoney(bill.wifiAmount)}đ</span></div>}
                {bill.serviceAmount > 0 && <div className="flex justify-between text-sm text-slate-600"><span>Dịch vụ</span><span className="font-semibold">{formatMoney(bill.serviceAmount)}đ</span></div>}
                {bill.trashAmount > 0 && <div className="flex justify-between text-sm text-slate-600"><span>Rác</span><span className="font-semibold">{formatMoney(bill.trashAmount)}đ</span></div>}
                <div className="border-t border-slate-100 pt-2 mt-2 flex justify-between text-base font-bold text-slate-800">
                    <span>Tổng cộng</span><span className="text-indigo-700">{formatMoney(bill.totalAmount)}đ</span>
                </div>
                {isPartial && <div className="flex justify-between text-sm font-bold text-red-600"><span>Còn lại cần trả</span><span>{formatMoney(remaining)}đ</span></div>}
                {bill.dueDate && (
                    <div className="text-xs text-slate-400 mt-1">
                        Hạn thanh toán: {new Date(bill.dueDate).toLocaleDateString('vi-VN')}
                    </div>
                )}
            </div>

            {/* Actions */}
            {!isFullyPaid && settings?.bankCode && settings?.bankAccount && (
                <div className="px-6 pb-6 mt-2">
                    <button
                        onClick={() => setShowQR(v => !v)}
                        className={`w-full py-3.5 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all shadow-sm ${showQR
                                ? 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                                : 'bg-slate-900 text-white hover:bg-indigo-600'
                            }`}
                    >
                        {showQR ? (
                            <>
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" /></svg>
                                Đóng mã QR
                            </>
                        ) : (
                            <>
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg>
                                Thanh toán qua VietQR
                            </>
                        )}
                    </button>
                    {showQR && (
                        <div className="mt-4">
                            <VietQRImage
                                bankCode={settings.bankCode}
                                bankAccount={settings.bankAccount}
                                amount={isPartial ? remaining : bill.totalAmount}
                                content={`${bill.month} ${bill.roomName || 'Phong tro'}`}
                            />
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

export default function TenantPortalPage() {
    const [phone, setPhone] = useState('');
    const [bills, setBills] = useState([]);
    const [settings, setSettings] = useState(null);
    const [tenantName, setTenantName] = useState('');
    const [loading, setLoading] = useState(false);
    const [searched, setSearched] = useState(false);
    const [error, setError] = useState('');

    async function handleSearch(e) {
        e.preventDefault();
        const p = phone.trim();
        if (!p || p.length < 9) { setError('Vui lòng nhập số điện thoại hợp lệ (ít nhất 9 số).'); return; }
        setError('');
        setLoading(true);
        setSearched(false);
        setBills([]);

        try {
            // Find tenants matching this phone across all landlords
            const tenantSnap = await getDocs(collectionGroup(db, 'tenants'));
            const matched = tenantSnap.docs.filter(d => (d.data().phone || '').replace(/\D/g, '').endsWith(p.replace(/\D/g, '')));

            if (matched.length === 0) { setBills([]); setSearched(true); setLoading(false); return; }

            const firstTenant = matched[0];
            setTenantName(firstTenant.data().fullName || '');
            const uid = firstTenant.ref.path.split('/')[1];
            const tenantId = firstTenant.data().tenantId || parseInt(firstTenant.id);

            // Fetch bills + payments + settings in parallel (optimal — single round-trip)
            const [billSnap, paymentSnap, sDoc] = await Promise.all([
                getDocs(collection(db, `users/${uid}/bills`)),
                getDocs(collection(db, `users/${uid}/payments`)),
                getDoc(doc(db, `users/${uid}/settings/config`)),
            ]);

            // Aggregate total paid amount per billId from the payments sub-collection
            const paymentsDict = {};
            paymentSnap.docs.forEach(d => {
                const p = d.data();
                const bId = p.billId;
                if (bId != null) {
                    paymentsDict[bId] = (paymentsDict[bId] || 0) + (p.amount || 0);
                }
            });
            if (sDoc.exists()) setSettings(sDoc.data());

            // Get contract for this tenant to know their room
            const contractSnap = await getDocs(collection(db, `users/${uid}/contracts`));
            const myContracts = contractSnap.docs.filter(d => d.data().tenantId === tenantId);
            const myContractIds = myContracts.map(d => parseInt(d.data().contractId));

            const roomSnap = await getDocs(collection(db, `users/${uid}/rooms`));
            const roomsDict = {};
            roomSnap.docs.forEach(r => roomsDict[r.data().roomId] = r.data().roomName);

            const contractsDict = {};
            myContracts.forEach(c => contractsDict[parseInt(c.data().contractId)] = parseInt(c.data().roomId));

            const myBills = billSnap.docs
                .map(d => {
                    const b = d.data();
                    const rId = contractsDict[parseInt(b.contractId)];
                    // Tính breakdown từ raw fields vì các amount không được lưu trực tiếp
                    const electricUsed = (b.newElectric || 0) - (b.oldElectric || 0);
                    const waterUsed = (b.newWater || 0) - (b.oldWater || 0);
                    const electricAmount = electricUsed * (b.electricPrice || 0);
                    const waterAmount = waterUsed * (b.waterPrice || 0);
                    const rentAmount = b.rentPrice || 0;
                    const serviceAmount = b.serviceFee || 0;
                    return {
                        id: d.id,
                        roomName: roomsDict[rId],
                        ...b,
                        electricUsed,
                        waterUsed,
                        electricAmount,
                        waterAmount,
                        rentAmount,
                        serviceAmount,
                        // Lấy số tiền ĐÃ trả từ collection `payments` (chính xác 100%)
                        paidAmount: paymentsDict[b.billId] || 0,
                    };
                })
                .filter(b => myContractIds.includes(parseInt(b.contractId)))
                .sort((a, b) => (b.month || '').localeCompare(a.month || ''));

            setBills(myBills);
        } catch (err) {
            setError('Có lỗi xảy ra: ' + err.message);
        } finally {
            setLoading(false);
            setSearched(true);
        }
    }

    const unpaid = bills.filter(b => b.paymentStatus !== 'DA_THANH_TOAN');
    const totalOwed = unpaid.reduce((s, b) => s + (b.totalAmount || 0) - (b.paidAmount || 0), 0);

    return (
        <div className="min-h-screen bg-slate-50 font-sans pb-24 pt-24">
            {/* Header */}
            <div className="text-center px-6 max-w-2xl mx-auto mb-10">
                <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-indigo-50 text-indigo-600 mb-6 shadow-sm border border-indigo-100">
                    <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                </div>
                <h1 className="text-4xl font-extrabold mb-4 text-slate-900 tracking-tight">Cổng Thanh Toán</h1>
                <p className="text-slate-500 text-base max-w-md mx-auto">
                    Tra cứu và thanh toán hóa đơn phòng trọ trực tuyến. Nhập số điện thoại đã đăng ký với chủ nhà để bắt đầu.
                </p>
            </div>

            {/* Search Form */}
            <div className="max-w-lg mx-auto px-4 mb-10">
                <form onSubmit={handleSearch} className="bg-white rounded-2xl shadow-sm border border-slate-200 p-6">
                    <label className="block text-sm font-semibold text-slate-700 mb-3 flex items-center gap-2">
                        <svg className="w-4 h-4 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" /></svg>
                        Số điện thoại thuê phòng
                    </label>
                    <div className="flex flex-col sm:flex-row gap-3">
                        <div className="relative flex-1">
                            <input
                                type="tel"
                                value={phone}
                                onChange={e => setPhone(e.target.value)}
                                placeholder="VD: 0901234567"
                                className="w-full pl-4 pr-4 py-3.5 border border-slate-200 bg-slate-50 rounded-xl text-sm font-medium focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-colors"
                            />
                        </div>
                        <button type="submit" disabled={loading} className="px-6 py-3.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-60 text-white rounded-xl font-bold text-sm transition-colors flex items-center justify-center gap-2 shadow-sm">
                            {loading ? (
                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                            ) : (
                                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
                            )}
                            Tra cứu
                        </button>
                    </div>
                    {error && <p className="text-red-500 text-xs mt-3 flex items-center gap-1"><svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>{error}</p>}
                </form>
            </div>

            {/* Results */}
            {searched && (
                <div className="max-w-lg mx-auto px-4 mt-6">
                    {bills.length === 0 ? (
                        <div className="bg-white rounded-2xl p-8 text-center border border-slate-200 shadow-sm mt-8">
                            <div className="w-16 h-16 mx-auto bg-slate-50 text-slate-400 rounded-full flex items-center justify-center mb-4">
                                <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                            </div>
                            <h3 className="font-bold text-slate-800 text-lg mb-2">Không tìm thấy dữ liệu</h3>
                            <p className="text-sm text-slate-500 mb-6">Chưa có hóa đơn nào được ghi nhận cho số điện thoại này.</p>

                            <div className="bg-slate-50 rounded-xl p-5 text-left text-sm border border-slate-100">
                                <h4 className="font-semibold text-slate-700 mb-3 flex items-center gap-2">
                                    <svg className="w-4 h-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                                    Nguyên nhân có thể:
                                </h4>
                                <ul className="space-y-2 text-slate-500">
                                    <li className="flex gap-2 items-start"><span className="text-indigo-400 mt-1">•</span> Chủ nhà chưa cập nhật hóa đơn tháng này</li>
                                    <li className="flex gap-2 items-start"><span className="text-indigo-400 mt-1">•</span> Số điện thoại nhập vào không khớp với hợp đồng</li>
                                </ul>
                                <div className="mt-4 pt-3 border-t border-slate-200 text-slate-600 font-medium">
                                    Vui lòng liên hệ chủ nhà để kiểm tra lại nếu bạn nghĩ đây là lỗi.
                                </div>
                            </div>
                        </div>
                    ) : (
                        <>
                            <div className="mb-4 flex items-center justify-between flex-wrap gap-2">
                                <div>
                                    <h2 className="text-lg font-bold text-slate-800">Xin chào, {tenantName || 'Khách thuê'}!</h2>
                                    <p className="text-slate-500 text-sm">{bills.length} hóa đơn được tìm thấy</p>
                                </div>
                                {totalOwed > 0 && (
                                    <div className="bg-red-50 border border-red-200 rounded-xl px-4 py-2 text-right">
                                        <div className="text-[10px] text-red-400 font-bold uppercase">Tổng còn nợ</div>
                                        <div className="text-red-600 font-black text-lg font-mono">{formatMoney(totalOwed)}đ</div>
                                    </div>
                                )}
                            </div>
                            <div className="space-y-4">
                                {bills.map(bill => <BillCard key={bill.id} bill={bill} settings={settings} />)}
                            </div>
                        </>
                    )}
                </div>
            )}
        </div>
    );
}
