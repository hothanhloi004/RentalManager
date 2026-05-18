import { Geist } from "next/font/google";
import "./globals.css";
import Navbar from "./components/Navbar";

const geist = Geist({ subsets: ["latin"] });

export const metadata = {
  title: "Rental Manager - Quản lý nhà trọ",
  description: "Hệ thống quản lý phòng trọ thông minh với bản đồ giá thuê Việt Nam",
};

export default function RootLayout({ children }) {
  return (
    <html lang="vi" suppressHydrationWarning>
      <body className={`${geist.className} bg-slate-50 text-slate-900 min-h-screen`}>
        <Navbar />
        <main className="pt-16">{children}</main>
      </body>
    </html>
  );
}
