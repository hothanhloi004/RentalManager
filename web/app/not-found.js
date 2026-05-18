import Link from 'next/link';

export default function NotFound() {
    return (
        <div className="min-h-[80vh] flex flex-col items-center justify-center p-6 text-center">
            <div className="bg-white p-12 rounded-3xl shadow-xl border border-slate-100 max-w-lg w-full flex flex-col items-center">
                <div className="text-8xl font-black text-indigo-600 mb-6 font-mono tracking-tighter shadow-indigo-100 drop-shadow-lg">
                    404
                </div>

                <h1 className="text-3xl font-extrabold text-slate-800 mb-3 tracking-tight">
                    Không tìm thấy trang
                </h1>

                <p className="text-slate-500 mb-8 max-w-sm">
                    Có vẻ như đường dẫn bạn nhập không tồn tại, hoặc phòng trọ này đã bị xoá khỏi hệ thống.
                </p>

                <div className="flex flex-col sm:flex-row gap-4 w-full">
                    <Link href="/" className="flex-1 px-6 py-3.5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-xl font-bold transition-all flex items-center justify-center gap-2">
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
                        Trang chủ
                    </Link>
                    <Link href="/rooms" className="flex-1 px-6 py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl font-bold transition-all shadow-md shadow-indigo-200 flex items-center justify-center gap-2">
                        Tìm phòng khác
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" /></svg>
                    </Link>
                </div>
            </div>
        </div>
    );
}
