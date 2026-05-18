import { GoogleGenerativeAI } from '@google/generative-ai';
import { NextResponse } from 'next/server';

const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY || '');

export async function POST(request) {
    if (!process.env.GEMINI_API_KEY) {
        return NextResponse.json({ error: 'GEMINI_API_KEY chua duoc cau hinh trong .env.local' }, { status: 500 });
    }

    try {
        const body = await request.json();
        const { type, data } = body;

        const model = genAI.getGenerativeModel({ model: 'gemini-2.5-flash' });

        let prompt = '';

        if (type === 'reply_suggestion') {
            const { inquiry, room, hostelName, landlordName, landlordPhone } = data;
            const senderName = hostelName || landlordName || 'ben cho thue';

            prompt = `Ban la tro ly cua chu nha tro nguoi Viet Nam. Hay soan mot tin nhan phan hoi ngan gon, lich su va than thien bang tieng Viet de gui cho khach thue co yeu cau xem phong sau:

- Ten khach: ${inquiry.name}
- Phong quan tam: ${inquiry.roomName}
- Loi nhan cua khach: ${inquiry.note || 'Khong co'}
- Gia phong: ${room?.price ? new Intl.NumberFormat('vi-VN').format(room.price) + 'd/thang' : 'Chua ro'}
- Ten nha tro / chu nha: ${senderName}
${landlordPhone ? `- So dien thoai lien he cua chu nha (MOI): ${landlordPhone}` : ''}

Yeu cau:
- Chao ten khach, xac nhan da nhan yeu cau
- Moi khach den xem phong
${landlordPhone ? `- Yeu cau khach vui long lien he truoc qua so dien thoai cua ban la: ${landlordPhone}` : '- Hen lich den xem phong va trao doi them'}
- Tu xung la "${senderName}" (ten nha tro hoac ten chu nha thuc te)
- Khong dai qua 5 cau
- Viet nhu nhan tin Zalo thuc te, co emoji nhe nhang
Chi tra ve noi dung tin nhan, khong giai thich them.`;
        } else if (type === 'revenue_analysis') {
            const {
                revenueData,
                unpaidBills,
                stats,
                currentMonthKey,
                currentMonthUnpaidBills,
                currentMonthOutstanding,
            } = data;

            const totalRevenue = revenueData.reduce((s, r) => s + (r.total || 0), 0);
            const lastMonth = revenueData[revenueData.length - 1];
            const prevMonth = revenueData[revenueData.length - 2];
            const totalDebt = typeof currentMonthOutstanding === 'number'
                ? currentMonthOutstanding
                : unpaidBills.reduce((s, b) => s + Math.max((b.totalAmount || 0) - (b.paidAmount || 0), 0), 0);
            const debtBillCount = Array.isArray(currentMonthUnpaidBills)
                ? currentMonthUnpaidBills.length
                : unpaidBills.length;
            const debtMonthLabel = currentMonthKey || lastMonth?.name || 'N/A';

            prompt = `Ban la chuyen gia phan tich tai chinh cho chu nha tro nguoi Viet Nam. Hay viet mot doan phan tich nhanh bang tieng Viet dua tren du lieu that sau day:

DU LIEU THUC TE:
- Tong so phong: ${stats.rooms} phong
- So khach thue dang o: ${stats.tenants} nguoi
- Hop dong dang hieu luc: ${stats.contracts}
- Tong doanh thu tich luy: ${new Intl.NumberFormat('vi-VN').format(totalRevenue)}d
- Doanh thu thang gan nhat (${lastMonth?.name || 'N/A'}): ${new Intl.NumberFormat('vi-VN').format(lastMonth?.total || 0)}d
- Doanh thu thang truoc do (${prevMonth?.name || 'N/A'}): ${new Intl.NumberFormat('vi-VN').format(prevMonth?.total || 0)}d
- So hoa don chua thu trong thang ${debtMonthLabel}: ${debtBillCount} hoa don
- Tong tien con no trong thang ${debtMonthLabel}: ${new Intl.NumberFormat('vi-VN').format(totalDebt)}d
- Lich su doanh thu: ${revenueData.map(r => `${r.name}: ${new Intl.NumberFormat('vi-VN').format(r.total)}d`).join(', ')}

Hay viet dung 3-4 cau phan tich ngan gon, suc tich:
1. Nhan xet xu huong doanh thu (tang/giam bao nhieu % so voi thang truoc)
2. Tinh trang thu no cua rieng thang dang bao cao, khong duoc cong don cac thang cu
3. Mot loi khuyen thuc te ngan cho chu nha
Chi tra ve noi dung phan tich, khong viet tieu de, khong giai thich them.`;
        } else {
            return NextResponse.json({ error: 'Loai yeu cau khong hop le' }, { status: 400 });
        }

        const result = await model.generateContent(prompt);
        const text = result.response.text();

        return NextResponse.json({ text });
    } catch (error) {
        console.error('Gemini API error:', error);
        return NextResponse.json({ error: 'Loi ket noi AI: ' + error.message }, { status: 500 });
    }
}
