package com.example.rentalmanager.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.example.rentalmanager.data.model.BillWithInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Xuất hóa đơn ra ảnh và chia sẻ qua Zalo/Messenger/WhatsApp.
 * Cách dùng: InvoiceShareHelper.share(context, bill, electricPrice, waterPrice)
 */
public class InvoiceShareHelper {

    public static void share(Context ctx, BillWithInfo bill,
                             double ePrice, double wPrice) {
        Bitmap bmp = renderInvoice(bill, ePrice, wPrice);
        try {
            File outDir = new File(ctx.getCacheDir(), "invoices");
            outDir.mkdirs();
            File outFile = new File(outDir, "invoice_" + bill.billId + ".png");
            FileOutputStream fos = new FileOutputStream(outFile);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();

            Uri uri = FileProvider.getUriForFile(ctx,
                    ctx.getPackageName() + ".provider", outFile);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/png");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT,
                    "Hóa đơn tháng " + bill.month + " – " + bill.roomName);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            ctx.startActivity(Intent.createChooser(intent, "Gửi hóa đơn qua..."));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Bitmap renderInvoice(BillWithInfo bill, double ePrice, double wPrice) {
        int width = 900, padding = 60;
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

        int eUsed = bill.newElectric - bill.oldElectric;
        int wUsed = bill.newWater - bill.oldWater;
        double eMoney = eUsed * ePrice;
        double wMoney = wUsed * wPrice;
        double rent = bill.totalAmount - eMoney - wMoney;

        String[] lines = {
                "HÓA ĐƠN TIỀN TRỌ",
                "─────────────────────────────────",
                "Phòng: " + bill.roomName,
                "Khách: " + bill.tenantName,
                "Tháng: " + bill.month,
                "─────────────────────────────────",
                "Điện: " + bill.oldElectric + " -> " + bill.newElectric + " (" + eUsed + " kWh x " + fmt.format(ePrice) + "đ) = " + fmt.format(eMoney) + " đ",
                "Nước: " + bill.oldWater + " -> " + bill.newWater + " (" + wUsed + " m3 x " + fmt.format(wPrice) + "đ) = " + fmt.format(wMoney) + " đ",
                "Tiền phòng: " + fmt.format(rent) + " đ",
                "─────────────────────────────────",
                "TỔNG: " + fmt.format(bill.totalAmount) + " đ",
                "Trạng thái: " + ("DA_THANH_TOAN".equals(bill.paymentStatus)
                        ? "[Đã thanh toán]" : "[Chưa thanh toán]"),
                "─────────────────────────────────",
                "Xuất lúc: " + new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                        .format(new Date()),
        };

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(38);
        paint.setColor(Color.BLACK);
        int lineH = 56;
        int height = padding * 2 + lines.length * lineH + 40;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);

        int y = padding + 38;
        for (String line : lines) {
            if (line.startsWith("HÓA ĐƠN")) {
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(44);
                paint.setColor(0xFF3730A3); // indigo
            } else if (line.startsWith("TỔNG")) {
                paint.setTypeface(Typeface.DEFAULT_BOLD);
                paint.setTextSize(42);
                paint.setColor(0xFF059669); // green
            } else if (line.startsWith("─")) {
                paint.setColor(0xFFCCCCCC);
                paint.setTextSize(30);
            } else {
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextSize(36);
                paint.setColor(Color.BLACK);
            }
            canvas.drawText(line, padding, y, paint);
            y += lineH;
        }
        return bmp;
    }
}
